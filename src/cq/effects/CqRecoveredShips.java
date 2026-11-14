package cq.effects;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberStatusAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipRecoverySpecialData;
import com.fs.starfarer.api.util.Misc;

import cq.campaign.CqData;
import cq.campaign.CqStrings;
import cq.effects.pure.CqRecoveryMath;
import cq.items.CqCatalog;
import cq.items.CqEffectText;

public final class CqRecoveredShips {

  private static Encounter current;
  private static boolean warned;
  private static boolean warnedNote;

  private CqRecoveredShips() {
  }

  public static void register(SectorAPI sector) {
    current = null;
    warned = false;
    warnedNote = false;
    boolean hasEncounterHook = false;
    for (CampaignEventListener l : sector.getAllListeners()) {
      if (l instanceof EncounterHook) {
        hasEncounterHook = true;
        break;
      }
    }
    if (!hasEncounterHook) {
      sector.addTransientListener(new EncounterHook());
    }
    ListenerManagerAPI lm = sector.getListenerManager();
    if (!lm.hasListenerOfClass(RecoveryHook.class)) {
      lm.addListener(new RecoveryHook(), true);
    }
  }

  private static final class Encounter {
    final InteractionDialogAPI dialog;
    final CqRecoveryMath.Ledger ledger = new CqRecoveryMath.Ledger();
    boolean salvage;

    Encounter(InteractionDialogAPI dialog) {
      this.dialog = dialog;
    }
  }

  private static Encounter open(InteractionDialogAPI dialog) {
    if (current != null && current.dialog == dialog) {
      return current;
    }
    if (current != null) {
      close(current);
    }
    current = new Encounter(dialog);
    Global.getSector().addTransientScript(new Watcher(current));
    return current;
  }

  private static void close(Encounter enc) {
    if (current != enc) {
      return;
    }
    current = null;
    if (!enc.salvage) {
      return;
    }
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (fleet == null) {
      return;
    }
    List<Source> rows = null;
    List<CqRecoveryMath.CargoRow> cargoRows = null;
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      if (!enc.ledger.claim(member.getId())) {
        continue;
      }
      if (rows == null) {
        rows = rows();
        cargoRows = cargoRows();
      }
      pay(member, true, rows, cargoRows, null);
    }
  }

  private static void noteFleet(Encounter enc) {
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (fleet == null) {
      return;
    }
    List<String> ids = new ArrayList<String>();
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      ids.add(member.getId());
    }
    enc.ledger.noteMembers(ids);
  }

  private static final class EncounterHook extends BaseCampaignEventListener {

    EncounterHook() {
      super(false);
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
      try {
        if (dialog == null || !offersShip(dialog.getInteractionTarget())) {
          return;
        }
        Encounter enc = open(dialog);
        enc.salvage = true;
        noteFleet(enc);
      } catch (Throwable t) {
        fail(t);
      }
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
      try {
        noteFleet(open(Global.getSector().getCampaignUI().getCurrentInteractionDialog()));
      } catch (Throwable t) {
        fail(t);
      }
    }
  }

  private static final class RecoveryHook implements ShipRecoveryListener {

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> ships, InteractionDialogAPI dialog) {
      try {
        recovered(ships, dialog);
      } catch (Throwable t) {
        fail(t);
      }
    }
  }

  private static final class Watcher implements EveryFrameScript {
    private final Encounter enc;
    private boolean done;

    Watcher(Encounter enc) {
      this.enc = enc;
    }

    @Override
    public boolean isDone() {
      return done;
    }

    @Override
    public boolean runWhilePaused() {
      return true;
    }

    @Override
    public void advance(float amount) {
      if (done) {
        return;
      }
      try {
        if (Global.getSector().getCampaignUI().getCurrentInteractionDialog() == enc.dialog) {
          return;
        }
        done = true;
        close(enc);
      } catch (Throwable t) {
        done = true;
        fail(t);
      }
    }
  }

  private static boolean offersShip(SectorEntityToken target) {
    if (target == null || target instanceof CampaignFleetAPI) {
      return false;
    }
    MemoryAPI memory = target.getMemoryWithoutUpdate();
    return memory != null
        && memory.get(MemFlags.SALVAGE_SPECIAL_DATA) instanceof ShipRecoverySpecialData;
  }

  private static void recovered(List<FleetMemberAPI> ships, InteractionDialogAPI dialog) {
    if (ships == null || ships.isEmpty()) {
      return;
    }
    SectorEntityToken target = dialog == null ? null : dialog.getInteractionTarget();
    boolean derelict = target != null && !(target instanceof CampaignFleetAPI);
    CqRecoveryMath.Ledger ledger;
    if (current != null) {
      ledger = current.ledger;
    } else if (derelict) {
      ledger = new CqRecoveryMath.Ledger();
    } else {
      return;
    }
    List<Source> rows = rows();
    List<CqRecoveryMath.CargoRow> cargoRows = cargoRows();
    for (FleetMemberAPI member : ships) {
      if (member != null && ledger.claim(member.getId())) {
        pay(member, derelict, rows, cargoRows, dialog);
      }
    }
  }

  private static final class Source {
    final String id;
    final String group;
    final float hull;
    final float cr;
    final float dmods;

    Source(String id, String group, float hull, float cr, float dmods) {
      this.id = id;
      this.group = group;
      this.hull = hull;
      this.cr = cr;
      this.dmods = dmods;
    }
  }

  private static List<Source> rows() {
    List<Source> rows = new ArrayList<Source>();
    for (CqCatalog.Furnishing f : CqOccasions.activeRows(CqData.getInstance())) {
      float hull = points(f, CqCatalog.Kind.RECOVERY_HULL)
          + points(f, CqCatalog.Kind.RECOVERY_HULL_CR);
      float cr = points(f, CqCatalog.Kind.RECOVERY_CR)
          + points(f, CqCatalog.Kind.RECOVERY_HULL_CR);
      float dmods = points(f, CqCatalog.Kind.RECOVERY_DMODS);
      if (hull > 0f || cr > 0f || dmods > 0f) {
        rows.add(new Source(f.getId(), f.getArg(), hull, cr, dmods));
      }
    }
    return rows;
  }

  private static List<CqRecoveryMath.Row> owedTo(String memberId, List<Source> rows) {
    List<CqRecoveryMath.Row> owed = new ArrayList<CqRecoveryMath.Row>(rows.size());
    for (Source row : rows) {
      int dmods = CqRolls.pay(row.dmods, CqCatalog.Kind.RECOVERY_DMODS.name(), row.id, memberId);
      owed.add(new CqRecoveryMath.Row(row.group, row.hull, row.cr, dmods));
    }
    return owed;
  }

  private static List<CqRecoveryMath.CargoRow> cargoRows() {
    List<CqRecoveryMath.CargoRow> rows = new ArrayList<CqRecoveryMath.CargoRow>();
    for (CqCatalog.Furnishing f : CqOccasions.activeRows(CqData.getInstance(),
        CqCatalog.Kind.RECOVERY_CARGO)) {
      int units = Math.round(points(f, CqCatalog.Kind.RECOVERY_CARGO));
      String commodity = CqRecoveryMath.cargoCommodity(f.getArg());
      if (units > 0 && !commodity.isEmpty()
          && Global.getSettings().getCommoditySpec(commodity) != null) {
        rows.add(new CqRecoveryMath.CargoRow(f.getId(), CqRecoveryMath.cargoGroup(f.getArg()),
            commodity, units));
      }
    }
    return rows;
  }

  private static CqRecoveryMath.CargoClock cargoClock() {
    final CqRecoveryData state = CqData.getInstance().recovery();
    final CampaignClockAPI clock = Global.getSector().getClock();
    return new CqRecoveryMath.CargoClock() {
      @Override
      public Float daysSincePaid(String rowId) {
        Long stamp = state.cargoPaidAt(rowId);
        return stamp == null ? null : Float.valueOf(clock.getElapsedDaysSince(stamp.longValue()));
      }

      @Override
      public void paid(String rowId) {
        state.setCargoPaidAt(rowId, clock.getTimestamp());
      }
    };
  }

  private static List<String> paidBy(CqRecoveryMath.Ship ship, List<Source> rows,
      List<CqRecoveryMath.Row> owed, float hullGain, float crGain, int dmodsOff) {
    List<String> ids = new ArrayList<String>();
    for (int i = 0; i < rows.size(); i++) {
      Source row = rows.get(i);
      if (!CqRecoveryMath.inGroup(row.group, ship)) {
        continue;
      }
      boolean hull = row.hull > 0f;
      boolean cr = row.cr > 0f;
      boolean dmods = owed.get(i).dmods > 0;
      if ((hull && hullGain > 0f) || (cr && crGain > 0f) || (dmods && dmodsOff > 0)) {
        ids.add(row.id);
      }
    }
    return ids;
  }

  private static float points(CqCatalog.Furnishing f, CqCatalog.Kind kind) {
    return Math.max(0f, CqOccasions.magOf(f, kind));
  }

  private static void pay(FleetMemberAPI member, boolean fromDerelict,
      List<Source> rows, List<CqRecoveryMath.CargoRow> cargoRows,
      InteractionDialogAPI dialog) {
    if (rows.isEmpty() && cargoRows.isEmpty()) {
      return;
    }
    ShipVariantAPI variant = member.getVariant();
    ShipHullSpecAPI spec = member.getHullSpec();
    if (variant == null || spec == null) {
      return;
    }
    List<String> dmods = CqDmods.on(variant);
    CqRecoveryMath.Ship ship = new CqRecoveryMath.Ship(spec.getManufacturer(),
        spec.getHullSize().name(), spec.isCivilianNonCarrier(), fromDerelict, dmods.size(),
        CqDmods.hullUnrestorable(spec), CqDmods.variantUnrestorable(variant));
    List<CqRecoveryMath.Row> rolled = owedTo(member.getId(), rows);
    CqRecoveryMath.Benefit owed = CqRecoveryMath.total(ship, rolled);
    List<CqRecoveryMath.CargoRow> cargo = cargoRows.isEmpty()
        ? new ArrayList<CqRecoveryMath.CargoRow>()
        : CqRecoveryMath.cargo(ship, cargoRows, cargoClock());
    if (owed.isNone() && cargo.isEmpty()) {
      return;
    }
    CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
    if (playerFleet != null) {
      for (CqRecoveryMath.CargoRow row : cargo) {
        playerFleet.getCargo().addCommodity(row.commodity, row.units);
      }
    }

    int dmodsOff = CqDmods.remove(member, dmods, owed.dmods, CqRecoveryMath.MIN_DMODS_LEFT);

    float hullGain = 0f;
    FleetMemberStatusAPI status = member.getStatus();
    if (status != null) {
      float before = status.getHullFraction();
      float after = CqRecoveryMath.raised(before, owed.hullPoints, CqRecoveryMath.HULL_MAX);
      if (after > before) {
        status.setHullFraction(after);
        hullGain = after - before;
      }
    }

    float crGain = 0f;
    RepairTrackerAPI tracker = member.getRepairTracker();
    if (tracker != null && !member.isMothballed()) {
      float before = tracker.getBaseCR();
      float after = CqRecoveryMath.raised(before, owed.crPoints, tracker.getMaxCR());
      if (after > before) {
        tracker.setCR(after);
        crGain = after - before;
      }
    }

    List<String> paidBy = paidBy(ship, rows, rolled, hullGain, crGain, dmodsOff);
    for (CqRecoveryMath.CargoRow row : cargo) {
      paidBy.add(row.id);
    }
    say(member, hullGain, crGain, dmodsOff, cargo, paidBy, dialog);
  }

  private static void say(FleetMemberAPI member, float hullGain, float crGain, int dmodsOff,
      List<CqRecoveryMath.CargoRow> cargo, List<String> paidBy, InteractionDialogAPI dialog) {
    try {
      List<String> parts = new ArrayList<String>();
      String hull = percent(hullGain);
      if (hull != null) {
        parts.add(CqStrings.effects("recovery_note_hull", hull));
      }
      String cr = percent(crGain);
      if (cr != null) {
        parts.add(CqStrings.effects("recovery_note_cr", cr));
      }
      if (dmodsOff == 1) {
        parts.add(CqStrings.effects("recovery_note_dmod_one", "1"));
      } else if (dmodsOff > 1) {
        parts.add(CqStrings.effects("recovery_note_dmod_many", String.valueOf(dmodsOff)));
      }
      for (CqRecoveryMath.CargoRow row : cargo) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(row.commodity);
        parts.add(CqStrings.effects("recovery_note_cargo", Misc.getWithDGS(row.units),
            spec == null ? row.commodity : spec.getLowerCaseName()));
      }
      if (parts.isEmpty()) {
        return;
      }
      StringBuilder list = new StringBuilder();
      String join = CqStrings.effects("recovery_note_join");
      for (String part : parts) {
        if (list.length() > 0) {
          list.append(join);
        }
        list.append(part);
      }
      String name = member.getShipName();
      if (name == null || name.isEmpty()) {
        name = member.getHullSpec().getHullNameWithDashClass();
      }
      String line = CqEffectText.credit(
          CqStrings.effects("recovery_note", name, list.toString()), paidBy);
      if (dialog != null && dialog.getTextPanel() != null) {
        dialog.getTextPanel().addPara(line);
      } else {
        Global.getSector().getCampaignUI().addMessage(line);
      }
    } catch (Throwable t) {
      if (!warnedNote) {
        warnedNote = true;
        Global.getLogger(CqRecoveredShips.class).warn(
            "Captain's Quarters: a recovered ship was improved but the line saying so failed.", t);
      }
    }
  }

  private static String percent(float gain) {
    float tenths = Math.round(gain * 1000f) / 10f;
    if (tenths <= 0f) {
      return null;
    }
    return tenths == Math.round(tenths) ? String.valueOf(Math.round(tenths))
        : String.valueOf(tenths);
  }

  private static void fail(Throwable t) {
    if (!warned) {
      warned = true;
      Global.getLogger(CqRecoveredShips.class).warn(
          "Captain's Quarters: a display case effect failed on one recovery. Other recoveries"
              + " and every other furnishing effect are unaffected.", t);
    }
  }
}
