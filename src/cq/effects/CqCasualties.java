package cq.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.GroundRaidObjectivesListener;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.graid.GroundRaidObjectivePlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidType;
import com.fs.starfarer.api.util.Misc;

import cq.campaign.CqData;
import cq.campaign.CqStrings;
import cq.effects.pure.CqCasualtyMath;
import cq.items.CqCatalog;
import cq.items.CqEffectText;

public final class CqCasualties {

  static final String CR_EVENT_ID = "cq_cr_regained";

  private static boolean warnedOnce;

  private CqCasualties() {
  }

  public static void register(SectorAPI sector) {
    warnedOnce = false;
    boolean hasBattles = false;
    for (CampaignEventListener l : sector.getAllListeners()) {
      if (l instanceof Battles) {
        hasBattles = true;
        break;
      }
    }
    if (!hasBattles) {
      sector.addTransientListener(new Battles());
    }
    ListenerManagerAPI lm = sector.getListenerManager();
    if (!lm.hasListenerOfClass(Raids.class)) {
      lm.addListener(new Raids(), true);
    }
  }

  static final class Battles extends BaseCampaignEventListener {

    Battles() {
      super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
      try {
        onEngagement(result);
      } catch (Throwable t) {
        warn(t);
      }
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
      try {
        onBattleFinished(battle);
      } catch (Throwable t) {
        warn(t);
      }
    }
  }

  static final class Raids implements GroundRaidObjectivesListener {

    @Override
    public void modifyRaidObjectives(MarketAPI market, SectorEntityToken entity,
        List<GroundRaidObjectivePlugin> objectives, RaidType type, int marineTokens,
        int priority) {
    }

    @Override
    public void reportRaidObjectivesAchieved(RaidResultData data, InteractionDialogAPI dialog,
        Map<String, MemoryAPI> memoryMap) {
      try {
        onRaid(data, dialog);
      } catch (Throwable t) {
        warn(t);
      }
    }
  }

  static void onEngagement(EngagementResultAPI result) {
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (result == null || fleet == null) {
      return;
    }
    BattleAPI battle = result.getBattle() != null ? result.getBattle() : fleet.getBattle();
    CqCasualtyMath.Encounter encounter = CqData.getInstance().casualties().open(battle);
    CargoAPI cargo = fleet.getCargo();
    encounter.engagement(cargo.getCrew(), cargo.getMarines(), fleet.isInHyperspace(),
        result.didPlayerWin());
    down(encounter, battle, fleet, result.getWinnerResult());
    down(encounter, battle, fleet, result.getLoserResult());
    FleetMemberAPI piloted = fleet.getFlagship();
    if (piloted != null) {
      encounter.piloted(piloted.getId(), piloted.getRepairTracker().getBaseCR());
    }
  }

  private static void down(CqCasualtyMath.Encounter encounter, BattleAPI battle,
      CampaignFleetAPI fleet, EngagementResultForFleetAPI side) {
    if (side == null) {
      return;
    }
    down(encounter, battle, fleet, side.getDestroyed(), true);
    down(encounter, battle, fleet, side.getDisabled(), false);
  }

  private static void down(CqCasualtyMath.Encounter encounter, BattleAPI battle,
      CampaignFleetAPI fleet, List<FleetMemberAPI> ships, boolean destroyed) {
    if (ships == null) {
      return;
    }
    for (FleetMemberAPI member : ships) {
      boolean own = battle != null ? battle.getSourceFleet(member) == fleet
          : fleet.getFleetData().getMembersListCopy().contains(member);
      encounter.down(own, destroyed);
    }
  }

  static void onBattleFinished(BattleAPI battle) {
    CqData data = CqData.getInstance();
    CqCasualtyMath.Encounter encounter = data.casualties().end(battle);
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (encounter == null || fleet == null) {
      return;
    }
    CqCasualtyMath.Event event = encounter.event();
    boolean inHyperspace = encounter.inHyperspace();
    boolean shipDestroyed = encounter.shipDestroyed();
    CargoAPI cargo = fleet.getCargo();
    List<String> paidBy = new ArrayList<String>();
    int crew = returned(data, CqCatalog.Kind.CREW_RETURNED,
        encounter.crewLost(cargo.getCrew()), event, inHyperspace, shipDestroyed, paidBy);
    int marines = returned(data, CqCatalog.Kind.MARINES_RETURNED,
        encounter.marinesLost(cargo.getMarines()), event, inHyperspace, shipDestroyed, paidBy);
    if (crew > 0) {
      cargo.addCrew(crew);
    }
    if (marines > 0) {
      cargo.addMarines(marines);
    }
    try {
      regainReadiness(data, fleet, encounter, event, inHyperspace, shipDestroyed);
    } catch (Throwable t) {
      warn(t);
    }
    if (crew > 0 || marines > 0) {
      postReturned(crew, marines, paidBy);
    }
  }

  static int returned(CqData data, CqCatalog.Kind kind, int lost, CqCasualtyMath.Event event,
      boolean inHyperspace, boolean shipDestroyed, List<String> paidBy) {
    if (lost <= 0) {
      return 0;
    }
    float shares = 0f;
    List<String> ids = new ArrayList<String>();
    for (CqCatalog.Furnishing f : CqOccasions.activeRows(data, kind)) {
      if (CqCasualtyMath.pays(f.getWhen(), f.getArg(), event, inHyperspace, shipDestroyed)) {
        float share = CqCasualtyMath.share(lost, CqOccasions.pctOf(f, kind),
            CqOccasions.magOf(f, kind));
        if (share > 0f) {
          shares += share;
          ids.add(f.getId());
        }
      }
    }
    int back = CqCasualtyMath.returned(lost, shares);
    if (back > 0) {
      paidBy.addAll(ids);
    }
    return back;
  }

  private static void regainReadiness(CqData data, CampaignFleetAPI fleet,
      CqCasualtyMath.Encounter encounter, CqCasualtyMath.Event event, boolean inHyperspace,
      boolean shipDestroyed) {
    Map<String, Float> piloted = encounter.pilotedCr();
    if (piloted.isEmpty()) {
      return;
    }
    float pct = 0f;
    for (CqCatalog.Furnishing f : CqOccasions.activeRows(data, CqCatalog.Kind.CR_REGAINED)) {
      if (CqCasualtyMath.pays(f.getWhen(), f.getArg(), event, inHyperspace, shipDestroyed)) {
        pct += CqOccasions.magOf(f, CqCatalog.Kind.CR_REGAINED);
      }
    }
    if (pct <= 0f) {
      return;
    }
    String label = null;
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      Float before = piloted.get(member.getId());
      if (before == null) {
        continue;
      }
      float gain = CqCasualtyMath.crRegained(before.floatValue(),
          member.getRepairTracker().getBaseCR(), pct);
      if (gain > 0f) {
        if (label == null) {
          label = CqStrings.effects("casualty_cr_event");
        }
        member.getRepairTracker().applyCREvent(gain, CR_EVENT_ID, label);
      }
    }
  }

  private static void postReturned(int crew, int marines, List<String> paidBy) {
    String text;
    if (crew > 0 && marines > 0) {
      text = CqStrings.effects("casualty_msg_both", Misc.getWithDGS(crew),
          Misc.getWithDGS(marines));
    } else if (crew > 0) {
      text = CqStrings.effects("casualty_msg_crew", Misc.getWithDGS(crew));
    } else {
      text = CqStrings.effects("casualty_msg_marines", Misc.getWithDGS(marines));
    }
    text = CqEffectText.credit(text, paidBy);
    CampaignUIAPI ui = Global.getSector().getCampaignUI();
    if (ui != null) {
      ui.addMessage(text, Misc.getPositiveHighlightColor());
    }
  }

  static void onRaid(GroundRaidObjectivesListener.RaidResultData raid,
      InteractionDialogAPI dialog) {
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (raid == null || raid.marinesLost <= 0 || fleet == null) {
      return;
    }
    List<String> paidBy = new ArrayList<String>();
    int marines = returned(CqData.getInstance(), CqCatalog.Kind.MARINES_RETURNED,
        raid.marinesLost, CqCasualtyMath.Event.RAID, fleet.isInHyperspace(), false, paidBy);
    if (marines <= 0) {
      return;
    }
    fleet.getCargo().addMarines(marines);
    String count = Misc.getWithDGS(marines);
    String text = CqEffectText.credit(CqStrings.effects("casualty_msg_raid", count), paidBy);
    TextPanelAPI panel = dialog == null ? null : dialog.getTextPanel();
    if (panel != null) {
      panel.setFontSmallInsignia();
      panel.addPara(text, Misc.getPositiveHighlightColor());
      panel.highlightInLastPara(Misc.getHighlightColor(), count);
      panel.setFontInsignia();
      return;
    }
    CampaignUIAPI ui = Global.getSector().getCampaignUI();
    if (ui != null) {
      ui.addMessage(text, Misc.getPositiveHighlightColor());
    }
  }

  private static void warn(Throwable t) {
    if (!warnedOnce) {
      warnedOnce = true;
      Global.getLogger(CqCasualties.class).warn("Captain's Quarters: a furnishing that returns"
          + " crew, marines or combat readiness failed and paid nothing for that battle or raid."
          + " Every other furnishing effect is unaffected.", t);
    }
  }
}
