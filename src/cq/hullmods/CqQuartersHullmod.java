package cq.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import cq.campaign.CqData;
import cq.campaign.CqPluginEffects;
import cq.effects.CqCombatOccasions;
import cq.effects.CqOccasions;
import cq.items.CqCatalog;

public class CqQuartersHullmod extends BaseHullMod {

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    if (Global.getSector() == null || !CqData.exists()) {
      return;
    }
    for (String itemId : CqOccasions.payingItemIds(CqData.getInstance())) {
      CqCatalog.Furnishing f = CqCatalog.get(itemId);
      if (f == null) {
        continue;
      }
      applyOne(stats, id + "_" + itemId, f.getKind(), f.getEffectiveMag());
      if (f.getKind2() != null) {
        applyOne(stats, id + "_" + itemId, f.getKind2(), f.getEffectiveMag2());
      }
      CqPluginEffects.applyShip(f, id + "_" + itemId, stats);
    }
  }

  private static volatile boolean combatFailed;

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    if (combatFailed || Global.getSector() == null || !CqData.exists()) {
      return;
    }
    try {
      CqCombatOccasions.onShipCreated(ship, id);
    } catch (Throwable t) {
      combatFailed(t);
    }
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {
    if (combatFailed) {
      return;
    }
    try {
      CqCombatOccasions.advance(ship, amount);
    } catch (Throwable t) {
      combatFailed(t);
    }
  }

  private static void combatFailed(Throwable t) {
    combatFailed = true;
    Global.getLogger(CqQuartersHullmod.class).warn("Captain's Quarters: the in-battle furnishing"
        + " effects failed and are off for the rest of this session. Every other furnishing"
        + " effect is unaffected.", t);
  }

  public static void applyOne(MutableShipStatsAPI stats, String modId, CqCatalog.Kind kind,
      float mag) {
    if (kind == null) {
      return;
    }
    switch (kind) {
      case PEAK_TIME:
        stats.getPeakCRDuration().modifyFlat(modId, mag);
        break;
      case EMP_TAKEN:
        stats.getEmpDamageTakenMult().modifyMult(modId, 1f - mag / 100f);
        break;
      case FLUX_DISSIPATION:
        stats.getFluxDissipation().modifyPercent(modId, mag);
        break;
      case CREW_COMBAT_LOSS:
      case CREW_HULL_DAMAGE:
        stats.getCrewLossMult().modifyMult(modId, 1f - mag / 100f);
        break;
      case CR_DEGRADATION:
        stats.getCRLossPerSecondPercent().modifyMult(modId, 1f - mag / 100f);
        break;
      case GROUND_SUPPORT:
        stats.getDynamic().getMod(Stats.FLEET_GROUND_SUPPORT).modifyFlat(modId, mag);
        break;
      case TOP_SPEED:
        stats.getMaxSpeed().modifyPercent(modId, mag);
        break;
      case VENT_RATE:
        stats.getVentRateMult().modifyFlat(modId, mag * 0.01f);
        break;
      case FLUX_CAPACITY:
        stats.getFluxCapacity().modifyPercent(modId, mag);
        break;
      case SHIELD_DAMAGE:
        stats.getShieldDamageTakenMult().modifyMult(modId, 1f - mag / 100f);
        break;
      case OVERLOAD_REDUCTION:
        stats.getOverloadTimeMod().modifyMult(modId, 1f - mag / 100f);
        break;
      case RECOIL:
        stats.getMaxRecoilMult().modifyMult(modId, 1f - 0.01f * mag);
        stats.getRecoilPerShotMult().modifyMult(modId, 1f - 0.01f * mag);
        stats.getRecoilDecayMult().modifyMult(modId, 1f - 0.01f * mag);
        break;
      case MANEUVERABILITY:
        stats.getAcceleration().modifyPercent(modId, mag);
        stats.getDeceleration().modifyPercent(modId, mag);
        stats.getTurnAcceleration().modifyPercent(modId, mag * 2f);
        stats.getMaxTurnRate().modifyPercent(modId, mag);
        break;
      case ARMOR_DAMAGE_TAKEN:
        stats.getArmorDamageTakenMult().modifyMult(modId, 1f - mag / 100f);
        break;
      case WEAPON_RANGE:
        stats.getBallisticWeaponRangeBonus().modifyPercent(modId, mag);
        stats.getEnergyWeaponRangeBonus().modifyPercent(modId, mag);
        break;
      case MAX_CR:
        stats.getMaxCombatReadiness().modifyFlat(modId, mag * 0.01f);
        break;
      default:
        break;
    }
  }
}
