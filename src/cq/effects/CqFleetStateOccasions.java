package cq.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.util.Misc;

import cq.effects.pure.CqFleetStateMath;
import cq.effects.pure.CqWhen;

public final class CqFleetStateOccasions {

  private CqFleetStateOccasions() {
  }

  private static String marketLocationId;
  private static long marketCountedAt;
  private static boolean noMarketHere;

  public static void register(SectorAPI sector) {
    marketLocationId = null;
    marketCountedAt = 0L;
    noMarketHere = false;
  }

  public static void onBeat(CampaignFleetAPI fleet, CqFleetStateData state) {
    if (fleet == null) {
      return;
    }
    state.noteDark(abilityActive(fleet, Abilities.GO_DARK),
        Global.getSector().getClock().getTimestamp());
  }

  public static boolean holds(CqWhen when, CampaignFleetAPI fleet, CqFleetStateData state) {
    if (when == null || fleet == null) {
      return false;
    }
    switch (when) {
      case MOVING_SLOWLY:
        return Misc.isSlowMoving(fleet);
      case SUSTAINED_BURN:
        return abilityActive(fleet, Abilities.SUSTAINED_BURN);
      case IN_SLIPSTREAM:
        return fleet.isInHyperspace() && Misc.isInsideSlipstream(fleet);
      case STOPPED:
        return CqFleetStateMath.stopped(fleet.getVelocity().length());
      case TRANSPONDER_OFF:
        return !fleet.isTransponderOn();
      case DARK_DAY:
        return state.isDark() && CqFleetStateMath.darkDay(
            abilityActive(fleet, Abilities.GO_DARK), state.isDark(),
            Global.getSector().getClock().getElapsedDaysSince(state.getDarkSince()));
      case NO_MARKET_SYSTEM:
        return noMarketSystem(fleet);
      case IN_HYPERSPACE:
        return fleet.isInHyperspace();
      case RUNNING_DARK:
        return abilityActive(fleet, Abilities.GO_DARK);
      default:
        return false;
    }
  }

  private static boolean abilityActive(CampaignFleetAPI fleet, String abilityId) {
    AbilityPlugin ability = fleet.getAbility(abilityId);
    return ability != null && ability.isActive();
  }

  private static boolean noMarketSystem(CampaignFleetAPI fleet) {
    LocationAPI location = fleet.getContainingLocation();
    if (location == null || fleet.isInHyperspace()) {
      return false;
    }
    CampaignClockAPI clock = Global.getSector().getClock();
    String locationId = location.getId();
    boolean same = CqFleetStateMath.sameLocation(marketLocationId, locationId);
    if (CqFleetStateMath.recountMarkets(same,
        same ? clock.getElapsedDaysSince(marketCountedAt) : 0f)) {
      int visible = 0;
      for (MarketAPI market : Misc.getMarketsInLocation(location)) {
        if (!market.isHidden()) {
          visible++;
          break;
        }
      }
      noMarketHere = CqFleetStateMath.noMarketSystem(false, visible);
      marketLocationId = locationId;
      marketCountedAt = clock.getTimestamp();
    }
    return noMarketHere;
  }
}
