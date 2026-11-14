package cq.campaign;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;

import cq.campaign.acquire.CqBattleLoot;
import cq.campaign.acquire.CqMarketStocker;
import cq.campaign.acquire.CqSalvageSeeder;
import cq.effects.CqOccasions;
import cq.effects.CqRolls;
import cq.items.CqCatalog;

public class CqEffects implements EveryFrameScript {

  static final String SUPPLY_CACHE_KEY = "cq_supply_applied";

  public static void registerAll(SectorAPI sector) {
    CqCatalog.reload();

    CqData data = CqData.getInstance();
    data.selfHeal(CqCatalog.itemIds());
    data.markEffectsDirty();

    if (!sector.hasTransientScript(CqEffects.class)) {
      sector.addTransientScript(new CqEffects());
    }
    ListenerManagerAPI lm = sector.getListenerManager();
    if (!lm.hasListenerOfClass(CqMarketStocker.class)) {
      lm.addListener(new CqMarketStocker(), true);
    }
    if (!lm.hasListenerOfClass(CqSalvageSeeder.class)) {
      lm.addListener(new CqSalvageSeeder(), true);
    }
    boolean hasBattleLoot = false;
    for (CampaignEventListener l : sector.getAllListeners()) {
      if (l instanceof CqBattleLoot) {
        hasBattleLoot = true;
        break;
      }
    }
    if (!hasBattleLoot) {
      sector.addTransientListener(new CqBattleLoot());
    }
    CqSalvageSeeder.syncTables();
    CqOccasions.registerAll(sector);
  }

  private final IntervalUtil interval = new IntervalUtil(0.9f, 1.1f);
  private final CqFlagshipTracker tracker = new CqFlagshipTracker();
  private final Map<String, Float> supplyMults = new LinkedHashMap<String, Float>();
  private final Map<String, Float> fuelMults = new LinkedHashMap<String, Float>();
  private final Map<String, Float> instaFlats = new LinkedHashMap<String, Float>();
  private final Map<String, Float> coronaMults = new LinkedHashMap<String, Float>();
  private final Map<String, Float> crRecPercents = new LinkedHashMap<String, Float>();
  private final List<String> pluginItems = new ArrayList<String>();
  private boolean perMemberActive;
  private boolean warnedOnce;
  private float lastEffectMult = Float.NaN;

  @Override
  public void advance(float amount) {
    try {
      CqData data = CqData.getInstance();
      if (data.consumeEffectsDirty()) {
        recompute(data);
        return;
      }
      interval.advance(amount);
      if (!interval.intervalElapsed()) {
        return;
      }
      if (!cq.topbar.CqMgmtInjector.isAlive()) {
        cq.intel.CqIntel.ensureExists();
      }
      CqQuestRewards.poll(data);
      if (tracker.maintain()) {
        recompute(data);
        return;
      }
      float mult = CqSettings.difficultyEffectMult();
      if (mult != lastEffectMult) {
        lastEffectMult = mult;
        recompute(data);
        return;
      }
      CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
      PersonAPI player = Global.getSector().getPlayerPerson();
      if (fleet == null || player == null) {
        return;
      }
      CqOccasions.beat(fleet, data);
      if (CqOccasions.refresh(fleet, data)) {
        recompute(data);
        return;
      }
      assertStatMods(data, fleet, player);
      if (perMemberCacheMissing(fleet)) {
        recomputePerMember(data, fleet);
      } else {
        reassertPerMember(fleet);
      }
    } catch (Throwable t) {
      if (!warnedOnce) {
        warnedOnce = true;
        Global.getLogger(CqEffects.class).warn(
            "Captain's Quarters: effect recompute failed; furnishing bonuses may not apply"
                + " until the next slot change or game load.", t);
      }
    }
  }

  private boolean perMemberCacheMissing(CampaignFleetAPI fleet) {
    if (!perMemberActive) {
      return false;
    }
    return fleet.getFleetData() != null
        && fleet.getFleetData().getCacheClearedOnSync().get(SUPPLY_CACHE_KEY) == null;
  }

  private void recompute(CqData data) {
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    PersonAPI player = Global.getSector().getPlayerPerson();
    if (fleet == null || player == null) {
      data.markEffectsDirty();
      return;
    }
    MutableFleetStatsAPI fs = fleet.getStats();
    MutableCharacterStatsAPI ps = player.getStats();

    CqOccasions.refresh(fleet, data);

    for (CqCatalog.Furnishing f : CqCatalog.all()) {
      String itemId = f.getId();
      fs.getDynamic().getMod(Stats.PLANETARY_OPERATIONS_MOD).unmodify(itemId);
      fs.getDynamic().getStat(Stats.NON_COMBAT_CREW_LOSS_MULT).unmodify(itemId);
      fs.getDynamic().getStat(Stats.SALVAGE_VALUE_MULT_FLEET_NOT_RARE).unmodify(itemId);
      fs.getDynamic().getStat(Stats.SALVAGE_VALUE_MULT_FLEET_INCLUDES_RARE).unmodify(itemId);
      fs.getDynamic().getStat(Stats.BATTLE_SALVAGE_MULT_FLEET).unmodify(itemId);
      fs.getDynamic().getStat(Stats.SURVEY_COST_MULT).unmodify(itemId);
      fs.getSensorRangeMod().unmodify(itemId);
      fs.getDetectedRangeMod().unmodify(itemId);
      ps.getRepairRateMult().unmodify(itemId);
      fs.getDynamic().getStat(Stats.PLANETARY_OPERATIONS_CASUALTIES_MULT).unmodify(itemId);
      fs.getDynamic().getStat(Stats.FUEL_SALVAGE_VALUE_MULT_FLEET).unmodify(itemId);
      fs.getDynamic().getStat(Stats.EMERGENCY_BURN_CR_MULT).unmodify(itemId);
      fs.getDynamic().getStat(Stats.GO_DARK_DETECTED_AT_MULT).unmodify(itemId);
      ps.getDynamic().getMod(Stats.CUSTOM_PRODUCTION_MOD).unmodify(itemId);
      ps.getDynamic().getStat(Stats.NAVIGATION_PENALTY_MULT).unmodify(itemId);
      fs.getDynamic().getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD).unmodify(itemId);
      fs.getDynamic().getMod(Stats.OWN_WEAPON_RECOVERY_MOD).unmodify(itemId);
      fs.getDynamic().getMod(Stats.OWN_WING_RECOVERY_MOD).unmodify(itemId);
      fs.getDynamic().getMod(Stats.ENEMY_WEAPON_RECOVERY_MOD).unmodify(itemId);
      fs.getDynamic().getMod(Stats.ENEMY_WING_RECOVERY_MOD).unmodify(itemId);
      fs.getDynamic().getMod(Stats.RECOVERED_HULL_MAX).unmodify(itemId);
      fs.getDynamic().getMod(Stats.RECOVERED_CR_MAX).unmodify(itemId);
      ps.getDynamic().getStat(Stats.COMMAND_POINT_RATE_COMMANDER).unmodify(itemId);
      ps.getDynamic().getMod(Stats.NUM_MAX_CONTACTS_MOD).unmodify(itemId);
      fs.getDynamic().getMod(Stats.SHIP_DMOD_REDUCTION).unmodify(itemId);
      ps.getCommandPoints().unmodify(itemId);
      CqPluginEffects.clearFleet(f, fs, ps);
    }

    fleet.forceSync();

    assertStatMods(data, fleet, player);
    recomputePerMember(data, fleet);
  }

  private void assertStatMods(CqData data, CampaignFleetAPI fleet, PersonAPI player) {
    MutableFleetStatsAPI fs = fleet.getStats();
    MutableCharacterStatsAPI ps = player.getStats();
    for (String itemId : CqOccasions.payingItemIds(data)) {
      CqCatalog.Furnishing f = CqCatalog.get(itemId);
      if (f == null) {
        continue;
      }
      applyOne(fs, ps, itemId, f.getName(), f.getKind(), f.getEffectiveMag());
      if (f.getKind2() != null) {
        applyOne(fs, ps, itemId, f.getName(), f.getKind2(), f.getEffectiveMag2());
      }
      CqPluginEffects.applyFleet(f, fs, ps);
    }
  }

  private void applyOne(MutableFleetStatsAPI fs, MutableCharacterStatsAPI ps, String id,
      String desc, CqCatalog.Kind kind, float mag) {
      if (kind == null) {
        return;
      }
      switch (kind) {
        case GROUND_OPS:
          fs.getDynamic().getMod(Stats.PLANETARY_OPERATIONS_MOD).modifyPercent(id, mag, desc);
          break;
        case NONCOMBAT_CREW_LOSS:
          fs.getDynamic().getStat(Stats.NON_COMBAT_CREW_LOSS_MULT)
              .modifyMult(id, 1f - mag / 100f, desc);
          break;
        case SALVAGE_NOT_RARE:
          fs.getDynamic().getStat(Stats.SALVAGE_VALUE_MULT_FLEET_NOT_RARE)
              .modifyFlat(id, mag * 0.01f, desc);
          break;
        case RARE_SALVAGE:
          fs.getDynamic().getStat(Stats.SALVAGE_VALUE_MULT_FLEET_INCLUDES_RARE)
              .modifyFlat(id, mag * 0.01f, desc);
          break;
        case BATTLE_SALVAGE:
          fs.getDynamic().getStat(Stats.BATTLE_SALVAGE_MULT_FLEET)
              .modifyFlat(id, mag * 0.01f, desc);
          break;
        case SURVEY_COST:
          fs.getDynamic().getStat(Stats.SURVEY_COST_MULT).modifyMult(id, 1f - mag / 100f, desc);
          break;
        case DETECTED_AT:
          fs.getDetectedRangeMod().modifyMult(id, 1f - mag / 100f, desc);
          break;
        case SENSOR_RANGE:
          fs.getSensorRangeMod().modifyPercent(id, mag, desc);
          break;
        case REPAIR_RATE:
          ps.getRepairRateMult().modifyPercent(id, mag, desc);
          break;
        case MARINE_CASUALTIES:
          fs.getDynamic().getStat(Stats.PLANETARY_OPERATIONS_CASUALTIES_MULT)
              .modifyMult(id, 1f - mag / 100f, desc);
          break;
        case FUEL_SALVAGE:
          fs.getDynamic().getStat(Stats.FUEL_SALVAGE_VALUE_MULT_FLEET)
              .modifyFlat(id, mag * 0.01f, desc);
          break;
        case EBURN_CR_COST:
          fs.getDynamic().getStat(Stats.EMERGENCY_BURN_CR_MULT)
              .modifyMult(id, 1f - mag / 100f, desc);
          break;
        case GO_DARK_DETECT:
          fs.getDynamic().getStat(Stats.GO_DARK_DETECTED_AT_MULT)
              .modifyMult(id, 1f - mag / 100f, desc);
          break;
        case CUSTOM_PRODUCTION:
          ps.getDynamic().getMod(Stats.CUSTOM_PRODUCTION_MOD)
              .modifyMult(id, 1f + mag / 100f, desc);
          break;
        case TERRAIN_PENALTY:
          ps.getDynamic().getStat(Stats.NAVIGATION_PENALTY_MULT)
              .modifyFlat(id, -0.01f * mag, desc);
          break;
        case MOVE_SLOW_BONUS:
          fs.getDynamic().getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD)
              .modifyFlat(id, standingCount(mag, id), desc);
          break;
        case ENEMY_SALVAGE_RECOVERY:
          fs.getDynamic().getMod(Stats.ENEMY_WEAPON_RECOVERY_MOD)
              .modifyFlat(id, mag * 0.01f, desc);
          fs.getDynamic().getMod(Stats.ENEMY_WING_RECOVERY_MOD)
              .modifyFlat(id, mag * 0.01f, desc);
          break;
        case RECOVERED_HULL_MAX:
          fs.getDynamic().getMod(Stats.RECOVERED_HULL_MAX).modifyFlat(id, mag * 0.01f, desc);
          break;
        case RECOVERED_CR_MAX:
          fs.getDynamic().getMod(Stats.RECOVERED_CR_MAX).modifyFlat(id, mag * 0.01f, desc);
          break;
        case COMMAND_POINT_RATE:
          ps.getDynamic().getStat(Stats.COMMAND_POINT_RATE_COMMANDER)
              .modifyFlat(id, mag / 100f, desc);
          break;
        case MAX_CONTACTS:
          ps.getDynamic().getMod(Stats.NUM_MAX_CONTACTS_MOD).modifyFlat(id, mag, desc);
          break;
        case DMOD_REDUCTION:
          fs.getDynamic().getMod(Stats.SHIP_DMOD_REDUCTION)
              .modifyFlat(id, standingCount(mag, id), desc);
          break;
        case COMMAND_POINTS:
          ps.getCommandPoints().modifyFlat(id, CqRolls.pay(mag, id, CqRolls.day()), desc);
          break;
        case SUPPLY_USE_CAPPED:
        case FUEL_USE_CAPPED:
        case INSTA_REPAIR:
        case CORONA_RESIST:
        case CR_RECOVERY:
        case PEAK_TIME:
        case OFFICER_XP:
        case EMP_TAKEN:
        case FLUX_DISSIPATION:
        case CREW_COMBAT_LOSS:
        case CREW_HULL_DAMAGE:
        case CR_DEGRADATION:
        case GROUND_SUPPORT:
        case TOP_SPEED:
        case VENT_RATE:
        case FLUX_CAPACITY:
        case SHIELD_DAMAGE:
        case OVERLOAD_REDUCTION:
        case RECOIL:
        case MANEUVERABILITY:
        default:
          break;
      }
  }

  private static int standingCount(float mag, String id) {
    return CqRolls.pay(mag, id, CqRolls.month());
  }

  private void recomputePerMember(CqData data, CampaignFleetAPI fleet) {
    FleetDataAPI fleetData = fleet.getFleetData();
    if (fleetData == null) {
      return;
    }
    List<FleetMemberAPI> members = fleetData.getMembersListCopy();
    for (CqCatalog.Furnishing f : CqCatalog.all()) {
      if (carries(f, CqCatalog.Kind.SUPPLY_USE_CAPPED)) {
        String modId = "cq_supply_" + f.getSlug();
        for (FleetMemberAPI member : members) {
          member.getStats().getSuppliesPerMonth().unmodifyMult(modId);
        }
      }
      if (carries(f, CqCatalog.Kind.FUEL_USE_CAPPED)) {
        String modId = "cq_fuel_" + f.getSlug();
        for (FleetMemberAPI member : members) {
          member.getStats().getFuelUseMod().unmodifyMult(modId);
        }
      }
      if (carries(f, CqCatalog.Kind.INSTA_REPAIR)) {
        for (FleetMemberAPI member : members) {
          member.getStats().getDynamic().getMod(Stats.INSTA_REPAIR_FRACTION)
              .unmodifyFlat(f.getId());
        }
      }
      if (carries(f, CqCatalog.Kind.CORONA_RESIST)) {
        for (FleetMemberAPI member : members) {
          member.getStats().getDynamic().getStat(Stats.CORONA_EFFECT_MULT).unmodify(f.getId());
        }
      }
      if (carries(f, CqCatalog.Kind.CR_RECOVERY)) {
        for (FleetMemberAPI member : members) {
          member.getStats().getBaseCRRecoveryRatePercentPerDay().unmodify(f.getId());
        }
      }
      if (f.getEffectPlugin() != null) {
        for (FleetMemberAPI member : members) {
          CqPluginEffects.clearMember(f, member);
        }
      }
    }

    List<CqCatalog.Furnishing> supply = new ArrayList<CqCatalog.Furnishing>();
    List<float[]> supplyPctCap = new ArrayList<float[]>();
    List<CqCatalog.Furnishing> fuel = new ArrayList<CqCatalog.Furnishing>();
    List<float[]> fuelPctCap = new ArrayList<float[]>();
    instaFlats.clear();
    coronaMults.clear();
    crRecPercents.clear();
    pluginItems.clear();
    for (String itemId : CqOccasions.payingItemIds(data)) {
      CqCatalog.Furnishing f = CqCatalog.get(itemId);
      if (f == null) {
        continue;
      }
      if (f.getEffectPlugin() != null) {
        pluginItems.add(itemId);
      }
      if (carries(f, CqCatalog.Kind.SUPPLY_USE_CAPPED)) {
        supply.add(f);
        supplyPctCap.add(cappedPair(f, CqCatalog.Kind.SUPPLY_USE_CAPPED));
      }
      if (carries(f, CqCatalog.Kind.FUEL_USE_CAPPED)) {
        fuel.add(f);
        fuelPctCap.add(cappedPair(f, CqCatalog.Kind.FUEL_USE_CAPPED));
      }
      if (carries(f, CqCatalog.Kind.INSTA_REPAIR)) {
        instaFlats.put(f.getId(),
            Float.valueOf(magOf(f, CqCatalog.Kind.INSTA_REPAIR) * 0.01f));
      }
      if (carries(f, CqCatalog.Kind.CORONA_RESIST)) {
        coronaMults.put(f.getId(),
            Float.valueOf(1f - magOf(f, CqCatalog.Kind.CORONA_RESIST) / 100f));
      }
      if (carries(f, CqCatalog.Kind.CR_RECOVERY)) {
        crRecPercents.put(f.getId(), Float.valueOf(magOf(f, CqCatalog.Kind.CR_RECOVERY)));
      }
    }
    supplyMults.clear();
    fuelMults.clear();
    perMemberActive = !supply.isEmpty() || !fuel.isEmpty() || !instaFlats.isEmpty()
        || !coronaMults.isEmpty() || !crRecPercents.isEmpty() || !pluginItems.isEmpty();
    if (!perMemberActive) {
      return;
    }

    if (!supply.isEmpty()) {
      float base = 0f;
      for (FleetMemberAPI member : members) {
        base += member.getStats().getSuppliesPerMonth().getModifiedValue();
      }
      if (base > 0f) {
        for (int i = 0; i < supply.size(); i++) {
          float reduced = Math.min(base * supplyPctCap.get(i)[0] / 100f, supplyPctCap.get(i)[1]);
          supplyMults.put(supply.get(i).getId(), Float.valueOf(1f - reduced / base));
        }
      }
    }
    if (!fuel.isEmpty()) {
      float base = 0f;
      for (FleetMemberAPI member : members) {
        base += member.getFuelUse();
      }
      if (base > 0f) {
        for (int i = 0; i < fuel.size(); i++) {
          float reduced = Math.min(base * fuelPctCap.get(i)[0] / 100f, fuelPctCap.get(i)[1]);
          fuelMults.put(fuel.get(i).getId(), Float.valueOf(1f - reduced / base));
        }
      }
    }
    reassertPerMember(fleet);
    fleetData.getCacheClearedOnSync().put(SUPPLY_CACHE_KEY, Boolean.TRUE);
  }

  private static boolean carries(CqCatalog.Furnishing f, CqCatalog.Kind kind) {
    return CqEffectSlots.carries(f.getKind(), f.getKind2(), kind);
  }

  private static float magOf(CqCatalog.Furnishing f, CqCatalog.Kind kind) {
    return CqEffectSlots.pick(f.getKind(), f.getKind2(), kind,
        f.getEffectiveMag(), f.getEffectiveMag2());
  }

  private static float[] cappedPair(CqCatalog.Furnishing f, CqCatalog.Kind kind) {
    return new float[] {
        CqEffectSlots.pick(f.getKind(), f.getKind2(), kind,
            f.getEffectivePct(), f.getEffectivePct2()),
        CqEffectSlots.pick(f.getKind(), f.getKind2(), kind,
            f.getEffectiveMag(), f.getEffectiveMag2())};
  }

  private void reassertPerMember(CampaignFleetAPI fleet) {
    if (!perMemberActive || fleet.getFleetData() == null) {
      return;
    }
    List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
    for (Map.Entry<String, Float> e : supplyMults.entrySet()) {
      CqCatalog.Furnishing f = CqCatalog.get(e.getKey());
      if (f == null) {
        continue;
      }
      String modId = "cq_supply_" + f.getSlug();
      for (FleetMemberAPI member : members) {
        member.getStats().getSuppliesPerMonth().modifyMult(modId, e.getValue().floatValue(), f.getName());
      }
    }
    for (Map.Entry<String, Float> e : fuelMults.entrySet()) {
      CqCatalog.Furnishing f = CqCatalog.get(e.getKey());
      if (f == null) {
        continue;
      }
      String modId = "cq_fuel_" + f.getSlug();
      for (FleetMemberAPI member : members) {
        member.getStats().getFuelUseMod().modifyMult(modId, e.getValue().floatValue(), f.getName());
      }
    }
    for (Map.Entry<String, Float> e : instaFlats.entrySet()) {
      CqCatalog.Furnishing f = CqCatalog.get(e.getKey());
      if (f == null) {
        continue;
      }
      for (FleetMemberAPI member : members) {
        member.getStats().getDynamic().getMod(Stats.INSTA_REPAIR_FRACTION)
            .modifyFlat(e.getKey(), e.getValue().floatValue(), f.getName());
      }
    }
    for (Map.Entry<String, Float> e : coronaMults.entrySet()) {
      CqCatalog.Furnishing f = CqCatalog.get(e.getKey());
      if (f == null) {
        continue;
      }
      for (FleetMemberAPI member : members) {
        member.getStats().getDynamic().getStat(Stats.CORONA_EFFECT_MULT)
            .modifyMult(e.getKey(), e.getValue().floatValue(), f.getName());
      }
    }
    for (Map.Entry<String, Float> e : crRecPercents.entrySet()) {
      CqCatalog.Furnishing f = CqCatalog.get(e.getKey());
      if (f == null) {
        continue;
      }
      for (FleetMemberAPI member : members) {
        member.getStats().getBaseCRRecoveryRatePercentPerDay()
            .modifyPercent(e.getKey(), e.getValue().floatValue(), f.getName());
      }
    }
    for (int i = 0; i < pluginItems.size(); i++) {
      CqCatalog.Furnishing f = CqCatalog.get(pluginItems.get(i));
      if (f == null) {
        continue;
      }
      for (FleetMemberAPI member : members) {
        CqPluginEffects.applyMember(f, member);
      }
    }
  }

  @Override
  public boolean isDone() {
    return false;
  }

  @Override
  public boolean runWhilePaused() {
    return true;
  }
}
