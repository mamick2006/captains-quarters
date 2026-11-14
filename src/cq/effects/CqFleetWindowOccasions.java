package cq.effects;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

import cq.campaign.CqData;
import cq.campaign.CqStrings;
import cq.effects.pure.CqEffectLine;
import cq.effects.pure.CqFleetWindows;
import cq.effects.pure.CqWhen;
import cq.items.CqCatalog;

public final class CqFleetWindowOccasions {

  private CqFleetWindowOccasions() {
  }

  private static boolean warned;

  public static void register(SectorAPI sector) {
    warned = false;
    for (CampaignEventListener listener : sector.getAllListeners()) {
      if (listener instanceof Events) {
        return;
      }
    }
    sector.addTransientListener(new Events());
  }

  public static boolean holds(CqWhen when, CampaignFleetAPI fleet, CqFleetWindowData state) {
    switch (when) {
      case SUPPLIES_SHORT:
        return CqFleetWindows.suppliesShort(fleet.getCargo().getSupplies(),
            fleet.getLogistics().getTotalSuppliesPerDay());
      case CREW_SHORT:
        return CqFleetWindows.crewShort(fleet.getCargo().getCrew(),
            fleet.getFleetData().getMinCrew());
      case RECENTLY_DOCKED:
        return open(Global.getSector().getClock(), state.dockedAt(),
            CqFleetWindows.RECENTLY_DOCKED_DAYS);
      default:
        return false;
    }
  }

  private static float daysSince(CampaignClockAPI clock, Long stamp) {
    return stamp == null ? 0f : clock.getElapsedDaysSince(stamp.longValue());
  }

  private static boolean open(CampaignClockAPI clock, Long stamp, float windowDays) {
    return CqFleetWindows.windowOpen(stamp != null, daysSince(clock, stamp), windowDays);
  }

  private static boolean opens(CampaignClockAPI clock, Long stamp, float windowDays) {
    return CqFleetWindows.opensWindow(stamp != null, daysSince(clock, stamp), windowDays);
  }

  private static final class Events extends BaseCampaignEventListener {

    Events() {
      super(false);
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
      try {
        leftMarket(market);
      } catch (Throwable t) {
        warn(t);
      }
    }
  }

  private static void leftMarket(MarketAPI market) {
    if (!CqOccasions.docked(market)) {
      return;
    }
    CqFleetWindowData state = CqData.getInstance().fleetWindows();
    CampaignClockAPI clock = Global.getSector().getClock();
    boolean opens = opens(clock, state.dockedAt(), CqFleetWindows.RECENTLY_DOCKED_DAYS);
    state.leftMarket(clock.getTimestamp());
    if (opens) {
      announce(CqWhen.RECENTLY_DOCKED, "window_docked", CqFleetWindows.RECENTLY_DOCKED_DAYS);
    }
  }

  private static void announce(CqWhen when, String key, float days) {
    try {
      CampaignUIAPI ui = Global.getSector().getCampaignUI();
      if (ui == null) {
        return;
      }
      List<String> paying = new ArrayList<String>();
      for (CqCatalog.Furnishing f
          : CqOccasions.activeRows(CqData.getInstance(), CqWhen.Family.FLEET_WINDOW)) {
        if (f.getWhen() == when) {
          paying.add(f.getName());
        }
      }
      String names = CqEffectLine.listed(paying, CqStrings.effects("window_comma"),
          CqStrings.effects("window_join"));
      if (names.isEmpty()) {
        return;
      }
      String length = String.valueOf(Math.round(days));
      Color highlight = Misc.getHighlightColor();
      ui.addMessage(CqStrings.effects(key, names, length), Misc.getTextColor(),
          names, length, highlight, highlight);
    } catch (Throwable t) {
      warn(t);
    }
  }

  private static void warn(Throwable t) {
    if (!warned) {
      warned = true;
      Global.getLogger(CqFleetWindowOccasions.class).warn("Captain's Quarters: leaving a market"
          + " or its message failed. A furnishing that pays for 2 days after docking may miss"
          + " one. Every other furnishing effect is unaffected.", t);
    }
  }
}
