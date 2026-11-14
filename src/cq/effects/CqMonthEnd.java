package cq.effects;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import cq.campaign.CqData;
import cq.campaign.CqStrings;
import cq.effects.pure.CqMonthMath;
import cq.items.CqCatalog;
import cq.items.CqEffectText;

public final class CqMonthEnd {

  private CqMonthEnd() {
  }

  public static void register(SectorAPI sector) {
    boolean hasListener = false;
    for (CampaignEventListener l : sector.getAllListeners()) {
      if (l instanceof Listener) {
        hasListener = true;
        break;
      }
    }
    if (!hasListener) {
      sector.addTransientListener(new Listener());
    }
    if (!sector.hasTransientScript(Watch.class)) {
      sector.addTransientScript(new Watch());
    }
  }

  static final class Watch implements EveryFrameScript {
    private final IntervalUtil interval = new IntervalUtil(0.9f, 1.1f);
    private boolean afterPause = true;
    private boolean warned;

    @Override
    public boolean isDone() {
      return false;
    }

    @Override
    public boolean runWhilePaused() {
      return true;
    }

    @Override
    public void advance(float amount) {
      if (Global.getSector().isPaused()) {
        afterPause = true;
        return;
      }
      if (!afterPause) {
        interval.advance(amount);
        if (!interval.intervalElapsed()) {
          return;
        }
      }
      afterPause = false;
      try {
        if (CqData.exists()) {
          CqData data = CqData.getInstance();
          CqMonthMath.restamp(data.month().activeSince(), ids(monthRows(data)),
              Global.getSector().getClock().getTimestamp());
        }
      } catch (Throwable t) {
        if (!warned) {
          warned = true;
          Global.getLogger(CqMonthEnd.class).warn("Captain's Quarters: the month-end effects"
              + " could not note which furnishings are slotted; a furnishing slotted since the"
              + " last economy tick is counted from the next one.", t);
        }
      }
    }
  }

  static final class Listener extends BaseCampaignEventListener {
    private boolean warned;

    Listener() {
      super(false);
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
      try {
        if (CqData.exists()) {
          accrue(CqData.getInstance());
        }
      } catch (Throwable t) {
        warn(t);
      }
    }

    @Override
    public void reportEconomyMonthEnd() {
      try {
        if (CqData.exists()) {
          CqData data = CqData.getInstance();
          accrue(data);
          pay(data);
        }
      } catch (Throwable t) {
        warn(t);
      }
    }

    private void warn(Throwable t) {
      if (!warned) {
        warned = true;
        Global.getLogger(CqMonthEnd.class).warn("Captain's Quarters: a month-end effect failed;"
            + " it is skipped and every other furnishing effect is unaffected.", t);
      }
    }
  }

  static void accrue(CqData data) {
    CqMonthData month = data.month();
    CampaignClockAPI clock = Global.getSector().getClock();
    long now = clock.getTimestamp();
    long last = month.getLastStamp();
    month.setLastStamp(now);
    List<CqCatalog.Furnishing> rows = monthRows(data);
    Map<String, Long> since = month.activeSince();
    CqMonthMath.restamp(since, ids(rows), now);
    if (last == 0 || now <= last) {
      return;
    }
    double stretch = clock.getElapsedDaysSince(last);
    if (stretch <= 0) {
      return;
    }
    month.addMonthDays(stretch);
    for (CqCatalog.Furnishing f : rows) {
      CqCatalog.Kind kind = kindOf(f);
      double mag = CqMonthMath.snap(CqOccasions.magOf(f, kind));
      double in = clock.getElapsedDaysSince(since.get(f.getId()));
      month.addAccrued(f.getId(),
          CqMonthMath.accrueAtTick(modeOf(f, kind), mag, stretch, in));
      if (repairs(f)) {
        double dmods = CqMonthMath.snap(CqOccasions.magOf(f, CqCatalog.Kind.DMOD_REPAIRED));
        month.addRepairAccrued(f.getId(), CqMonthMath.accrueAtTick(
            CqMonthMath.Mode.REPAIR, dmods, stretch, in));
      }
    }
  }

  private static List<CqCatalog.Furnishing> monthRows(CqData data) {
    List<CqCatalog.Furnishing> rows = new ArrayList<CqCatalog.Furnishing>();
    for (CqCatalog.Furnishing f : CqOccasions.activeRows(data)) {
      if (modeOf(f, kindOf(f)) != null || repairs(f)) {
        rows.add(f);
      }
    }
    return rows;
  }

  private static boolean repairs(CqCatalog.Furnishing f) {
    return f.getKind() == CqCatalog.Kind.DMOD_REPAIRED
        || f.getKind2() == CqCatalog.Kind.DMOD_REPAIRED;
  }

  private static List<String> ids(List<CqCatalog.Furnishing> rows) {
    List<String> ids = new ArrayList<String>(rows.size());
    for (CqCatalog.Furnishing f : rows) {
      ids.add(f.getId());
    }
    return ids;
  }

  private static CqCatalog.Kind kindOf(CqCatalog.Furnishing f) {
    if (f.getKind() == CqCatalog.Kind.EXPERIENCE || f.getKind2() == CqCatalog.Kind.EXPERIENCE) {
      return CqCatalog.Kind.EXPERIENCE;
    }
    return CqCatalog.Kind.MONTHLY_YIELD;
  }

  static CqMonthMath.Mode modeOf(CqCatalog.Furnishing f, CqCatalog.Kind kind) {
    if (kind == CqCatalog.Kind.EXPERIENCE) {
      return CqMonthMath.Mode.DAY;
    }
    boolean carries = f.getKind() == CqCatalog.Kind.MONTHLY_YIELD
        || f.getKind2() == CqCatalog.Kind.MONTHLY_YIELD;
    return carries ? CqMonthMath.Mode.YIELD : null;
  }

  static void pay(CqData data) {
    CqMonthData month = data.month();
    Map<String, Double> accrued = month.accrued();
    Map<String, Double> repairs = month.repairAccrued();
    if (accrued.isEmpty() && repairs.isEmpty()) {
      month.closeMonth();
      return;
    }
    double divisor = CqMonthMath.monthDivisor(month.getMonthDays(), closedMonthLength());
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    List<String> lines = new ArrayList<String>();
    for (Map.Entry<String, Double> e : repairs.entrySet()) {
      CqCatalog.Furnishing f = CqCatalog.get(e.getKey());
      double amount = CqMonthMath.monthAmount(CqMonthMath.Mode.REPAIR, e.getValue(), divisor);
      if (f == null || fleet == null || amount <= 0) {
        continue;
      }
      try {
        payRepair(fleet, f, amount, lines);
      } catch (RuntimeException ex) {
        Global.getLogger(CqMonthEnd.class).warn("Captain's Quarters: " + e.getKey()
            + " could not repair a ship at month end and is skipped.", ex);
      }
    }
    for (Map.Entry<String, Double> e : accrued.entrySet()) {
      String id = e.getKey();
      CqCatalog.Furnishing f = CqCatalog.get(id);
      if (f == null || fleet == null) {
        continue;
      }
      CqCatalog.Kind kind = kindOf(f);
      CqMonthMath.Mode mode = modeOf(f, kind);
      if (mode == null) {
        continue;
      }
      double amount = CqMonthMath.monthAmount(mode, e.getValue(), divisor);
      if (amount <= 0) {
        continue;
      }
      try {
        if (mode == CqMonthMath.Mode.YIELD) {
          payYield(fleet, month, f, amount, lines);
        } else {
          payExperience(fleet, month, f, amount, lines);
        }
      } catch (RuntimeException ex) {
        Global.getLogger(CqMonthEnd.class).warn("Captain's Quarters: " + id
            + " could not pay at month end and is skipped.", ex);
      }
    }
    month.closeMonth();
    post(lines);
  }

  private static double closedMonthLength() {
    GregorianCalendar cal = (GregorianCalendar) Global.getSector().getClock().getCal().clone();
    cal.add(Calendar.MONTH, -1);
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
  }

  private static void payRepair(CampaignFleetAPI fleet, CqCatalog.Furnishing f, double amount,
      List<String> lines) {
    CampaignClockAPI clock = Global.getSector().getClock();
    int cycle = clock.getCycle();
    int closed = clock.getMonth() - 1;
    if (closed < 1) {
      closed = 12;
      cycle--;
    }
    int count = CqRolls.pay((float) amount, f.getId(), String.valueOf(cycle),
        String.valueOf(closed));
    if (count <= 0) {
      return;
    }
    List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
    float[] values = new float[members.size()];
    int[] spare = new int[members.size()];
    boolean[] restorable = new boolean[members.size()];
    for (int i = 0; i < members.size(); i++) {
      FleetMemberAPI member = members.get(i);
      ShipHullSpecAPI spec = member.getHullSpec();
      values[i] = spec == null ? 0f : spec.getBaseValue();
      restorable[i] = CqDmods.restorable(member);
      spare[i] = CqDmods.spare(member, CqMonthMath.REPAIR_DMODS_LEFT);
    }
    int pick = CqMonthMath.repairPick(values, spare, restorable);
    if (pick < 0) {
      return;
    }
    FleetMemberAPI ship = members.get(pick);
    int off = CqDmods.remove(ship, CqDmods.on(ship.getVariant()), count,
        CqMonthMath.REPAIR_DMODS_LEFT);
    if (off <= 0) {
      return;
    }
    String name = ship.getShipName();
    if (name == null || name.isEmpty()) {
      name = ship.getHullSpec().getHullNameWithDashClass();
    }
    lines.add(CqStrings.effects(off == 1 ? "month_end_dmod" : "month_end_dmod_many",
        String.valueOf(off), name));
  }

  private static void payYield(CampaignFleetAPI fleet, CqMonthData month, CqCatalog.Furnishing f,
      double amount, List<String> lines) {
    String commodity = f.getArg();
    CommoditySpecAPI spec = commodity == null ? null
        : Global.getSettings().getCommoditySpec(commodity.trim());
    if (spec == null) {
      return;
    }
    double carry = month.getCarry(f.getId());
    long units = CqMonthMath.whole(amount, carry);
    month.setCarry(f.getId(), CqMonthMath.carryAfter(amount, carry));
    if (units <= 0) {
      return;
    }
    fleet.getCargo().addCommodity(spec.getId(), units);
    String name = CqEffectText.commodityWords(spec.getId());
    lines.add(CqStrings.effects("month_end_yield", Misc.getWithDGS(units),
        name.isEmpty() ? spec.getLowerCaseName() : name));
  }

  private static void payExperience(CampaignFleetAPI fleet, CqMonthData month,
      CqCatalog.Furnishing f, double amount, List<String> lines) {
    CqMonthMath.Recipient who = CqMonthMath.Recipient.parse(f.getArg());
    if (who == null) {
      return;
    }
    if (who == CqMonthMath.Recipient.MARINES) {
      payMarines(amount, lines);
      return;
    }
    double carry = month.getCarry(f.getId());
    long xp = CqMonthMath.whole(amount, carry);
    month.setCarry(f.getId(), CqMonthMath.carryAfter(amount, carry));
    if (xp <= 0) {
      return;
    }
    if (who == CqMonthMath.Recipient.PLAYER) {
      Global.getSector().getPlayerStats().addXP(xp, null, false);
      lines.add(CqStrings.effects("month_end_xp_player", Misc.getWithDGS(xp)));
      return;
    }
    List<OfficerDataAPI> officers = eligibleOfficers(fleet);
    int[] levels = new int[officers.size()];
    int[] maxLevels = new int[officers.size()];
    OfficerLevelupPlugin plugin =
        (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");
    for (int i = 0; i < officers.size(); i++) {
      PersonAPI p = officers.get(i).getPerson();
      levels[i] = p.getStats().getLevel();
      maxLevels[i] = plugin.getMaxLevel(p);
    }
    int[] picked = CqMonthMath.pick(who, levels, maxLevels);
    if (picked.length == 0) {
      return;
    }
    for (int i : picked) {
      addOfficerXp(officers.get(i), xp, plugin);
    }
    if (picked.length == 1) {
      lines.add(CqStrings.effects("month_end_xp_officer", Misc.getWithDGS(xp),
          officers.get(picked[0]).getPerson().getNameString()));
    } else {
      lines.add(CqStrings.effects("month_end_xp_officers", Misc.getWithDGS(xp),
          String.valueOf(picked.length)));
    }
  }

  private static void addOfficerXp(OfficerDataAPI officer, long xp, OfficerLevelupPlugin plugin) {
    PersonAPI person = officer.getPerson();
    MutableCharacterStatsAPI stats = person.getStats();
    stats.addXP(xp, null, false, true, false);
    long cap = plugin.getXPForLevel(plugin.getMaxLevel(person));
    if (stats.getXP() > cap) {
      stats.setXP(cap);
    }
    if (officer.canLevelUp() && !officer.isMadePicks()) {
      officer.makeSkillPicks();
    }
  }

  private static List<OfficerDataAPI> eligibleOfficers(CampaignFleetAPI fleet) {
    List<OfficerDataAPI> out = new ArrayList<OfficerDataAPI>();
    for (OfficerDataAPI o : fleet.getFleetData().getOfficersCopy()) {
      PersonAPI p = o == null ? null : o.getPerson();
      if (p == null || p.isPlayer() || p.isAICore() || Misc.isMercenary(p)) {
        continue;
      }
      out.add(o);
    }
    return out;
  }

  private static void payMarines(double percent, List<String> lines) {
    PlayerFleetPersonnelTracker tracker = PlayerFleetPersonnelTracker.getInstance();
    if (tracker == null) {
      return;
    }
    tracker.update();
    PlayerFleetPersonnelTracker.PersonnelData marines = tracker.getMarineData();
    if (marines == null || marines.num <= 0) {
      return;
    }
    float before = marines.getXPLevel();
    marines.addXP((float) CqMonthMath.marinePoints(percent, marines.num));
    float gained = (marines.getXPLevel() - before) * 100f;
    if (gained <= 0) {
      return;
    }
    lines.add(CqStrings.effects("month_end_xp_marines", CqMonthMath.percentText(gained)));
  }

  private static void post(List<String> lines) {
    if (lines.isEmpty()) {
      return;
    }
    CampaignUIAPI ui = Global.getSector().getCampaignUI();
    if (ui == null) {
      return;
    }
    StringBuilder list = new StringBuilder();
    for (String line : lines) {
      if (list.length() > 0) {
        list.append(CqStrings.effects("month_end_separator"));
      }
      list.append(line);
    }
    ui.addMessage(CqStrings.effects("month_end_message", list.toString()),
        Misc.getPositiveHighlightColor());
  }
}
