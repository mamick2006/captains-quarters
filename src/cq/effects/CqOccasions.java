package cq.effects;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import cq.campaign.CqData;
import cq.campaign.CqEffectSlots;
import cq.effects.pure.CqFleetWindows;
import cq.effects.pure.CqWhen;
import cq.items.CqCatalog;

public final class CqOccasions {

  private CqOccasions() {
  }

  private static final EnumSet<CqWhen.Family> failed = EnumSet.noneOf(CqWhen.Family.class);

  public static void registerAll(SectorAPI sector) {
    failed.clear();
    CqData.getInstance().setOccasionsHolding(EnumSet.noneOf(CqWhen.class));
    try {
      CqFleetStateOccasions.register(sector);
    } catch (Throwable t) {
      fail(CqWhen.Family.FLEET_STATE, t);
    }
    try {
      CqFleetWindowOccasions.register(sector);
    } catch (Throwable t) {
      fail(CqWhen.Family.FLEET_WINDOW, t);
    }
    try {
      CqTariffs.register(sector);
    } catch (Throwable t) {
      fail(CqWhen.Family.TARIFF, t);
    }
    try {
      CqCasualties.register(sector);
    } catch (Throwable t) {
      fail(CqWhen.Family.CASUALTY, t);
    }
    try {
      CqContacts.register(sector);
    } catch (Throwable t) {
      fail(CqWhen.Family.CONTACT, t);
    }
    try {
      CqRecoveredShips.register(sector);
    } catch (Throwable t) {
      fail(CqWhen.Family.RECOVERY, t);
    }
    try {
      CqMonthEnd.register(sector);
    } catch (Throwable t) {
      fail(CqWhen.Family.MONTH, t);
    }
  }

  public static void beat(CampaignFleetAPI fleet, CqData data) {
    if (!failed.contains(CqWhen.Family.FLEET_STATE)) {
      try {
        CqFleetStateOccasions.onBeat(fleet, data.fleetState());
      } catch (Throwable t) {
        fail(CqWhen.Family.FLEET_STATE, t);
      }
    }
  }

  public static boolean refresh(CampaignFleetAPI fleet, CqData data) {
    EnumSet<CqWhen> asked = EnumSet.noneOf(CqWhen.class);
    EnumSet<CqWhen> holding = EnumSet.noneOf(CqWhen.class);
    for (CqCatalog.Furnishing f : activeRows(data)) {
      CqWhen when = f.getWhen();
      if (when == null || !when.family().isFleet() || !asked.add(when)) {
        continue;
      }
      if (ask(when, fleet, data)) {
        holding.add(when);
      }
    }
    return data.setOccasionsHolding(holding);
  }

  private static boolean ask(CqWhen when, CampaignFleetAPI fleet, CqData data) {
    CqWhen.Family family = when.family();
    if (failed.contains(family)) {
      return false;
    }
    try {
      return family == CqWhen.Family.FLEET_STATE
          ? CqFleetStateOccasions.holds(when, fleet, data.fleetState())
          : CqFleetWindowOccasions.holds(when, fleet, data.fleetWindows());
    } catch (Throwable t) {
      fail(family, t);
      return false;
    }
  }

  private static void fail(CqWhen.Family family, Throwable t) {
    if (failed.add(family)) {
      Global.getLogger(CqOccasions.class).warn("Captain's Quarters: the " + family
          + " effects failed and are off for the rest of this session. Every other furnishing"
          + " effect is unaffected.", t);
    }
  }

  private static final String TRADE_MODE_KEY = "$tradeMode";

  public static boolean docked(MarketAPI market) {
    if (market == null) {
      return false;
    }
    FactionAPI faction = market.getFaction();
    return CqFleetWindows.isDocking(market.isPlanetConditionMarketOnly(), market.isInEconomy(),
        faction == null || faction.isNeutralFaction(), refused(market));
  }

  private static boolean refused(MarketAPI market) {
    SectorEntityToken port = market.getPrimaryEntity();
    CampaignUIAPI ui = Global.getSector().getCampaignUI();
    InteractionDialogAPI dialog = ui == null ? null : ui.getCurrentInteractionDialog();
    if (dialog != null && dialog.getInteractionTarget() != null
        && dialog.getInteractionTarget().getMarket() == market) {
      port = dialog.getInteractionTarget();
    }
    return port != null && CoreUITradeMode.NONE.name().equals(
        port.getMemoryWithoutUpdate().getString(TRADE_MODE_KEY));
  }

  public static boolean holds(CqData data, CqWhen when) {
    return data.occasionHolds(when);
  }

  public static List<CqCatalog.Furnishing> activeRows(CqData data) {
    List<CqCatalog.Furnishing> result = new ArrayList<CqCatalog.Furnishing>();
    for (String itemId : data.getActiveItemIds()) {
      CqCatalog.Furnishing f = CqCatalog.get(itemId);
      if (f != null) {
        result.add(f);
      }
    }
    return result;
  }

  public static List<CqCatalog.Furnishing> activeRows(CqData data, CqCatalog.Kind kind) {
    List<CqCatalog.Furnishing> result = new ArrayList<CqCatalog.Furnishing>();
    for (CqCatalog.Furnishing f : activeRows(data)) {
      if (CqEffectSlots.carries(f.getKind(), f.getKind2(), kind)) {
        result.add(f);
      }
    }
    return result;
  }

  public static List<CqCatalog.Furnishing> activeRows(CqData data, CqWhen.Family family) {
    List<CqCatalog.Furnishing> result = new ArrayList<CqCatalog.Furnishing>();
    for (CqCatalog.Furnishing f : activeRows(data)) {
      if (f.getWhen() != null && f.getWhen().family() == family) {
        result.add(f);
      }
    }
    return result;
  }

  public static List<String> payingItemIds(CqData data) {
    List<String> result = new ArrayList<String>();
    for (CqCatalog.Furnishing f : activeRows(data)) {
      CqWhen when = f.getWhen();
      if (when != null) {
        CqWhen.Family family = when.family();
        if (family == CqWhen.Family.COMBAT || (family.isFleet() && !data.occasionHolds(when))) {
          continue;
        }
      }
      result.add(f.getId());
    }
    return result;
  }

  public static float magOf(CqCatalog.Furnishing f, CqCatalog.Kind kind) {
    return CqEffectSlots.pick(f.getKind(), f.getKind2(), kind,
        f.getEffectiveMag(), f.getEffectiveMag2());
  }

  public static float pctOf(CqCatalog.Furnishing f, CqCatalog.Kind kind) {
    return CqEffectSlots.pick(f.getKind(), f.getKind2(), kind,
        f.getEffectivePct(), f.getEffectivePct2());
  }
}
