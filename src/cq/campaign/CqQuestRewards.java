package cq.campaign;

import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.util.Misc;

import cq.campaign.acquire.CqDropMath;
import cq.items.CqCatalog;

public final class CqQuestRewards {

  static final String MARKER_PREFIX = "$cq_";
  static final String MARKER_SUFFIX = "_rewarded";
  static final String RED_PLANET_SLUG = "survey_slab";
  static final String RED_PLANET_GLOBAL = "$core_planetaryShieldPlanet";
  static final String RED_PLANET_SPEC = "red_planet";
  static final String LAB_LOOT_BLUEPRINTS = "blueprints";
  static final String LAB_LOOT_RARE_TECH = "rare_tech";

  private static volatile boolean warnedOnce;
  private static Map<String, String> rewardsCache;
  private static int redPlanetBeat;

  private CqQuestRewards() {
  }

  public static void poll(CqData data) {
    try {
      if (Global.getSector() == null) {
        return;
      }
      if (rewardsCache == null) {
        rewardsCache = CqSettings.getStringMap("cq_quest_rewards");
      }
      for (Map.Entry<String, String> e : rewardsCache.entrySet()) {
        if (marked(e.getValue())) {
          continue;
        }
        checkOne(data, e.getValue(), flagSet(e.getKey()));
      }
      if (!marked(RED_PLANET_SLUG)) {
        redPlanetBeat++;
        if (redPlanetBeat >= 10) {
          redPlanetBeat = 0;
          checkOne(data, RED_PLANET_SLUG, redPlanetDone());
        }
      }
    } catch (Throwable t) {
      if (!warnedOnce) {
        warnedOnce = true;
        Global.getLogger(CqQuestRewards.class).warn(
            "Captain's Quarters: quest-reward poll failed; quest furnishings may not be"
                + " granted this session.", t);
      }
    }
  }

  private static boolean marked(String configured) {
    return Global.getSector().getMemoryWithoutUpdate()
        .getBoolean(MARKER_PREFIX + configured + MARKER_SUFFIX);
  }

  private static void checkOne(CqData data, String configured, boolean completed) {
    CqCatalog.Furnishing f = CqCatalog.resolveConfigured(configured);
    if (f == null) {
      return;
    }
    String itemId = f.getId();
    int action = CqDropMath.questGrantAction(completed, false, data.isOwned(itemId));
    if (action == CqDropMath.QUEST_NOTHING) {
      return;
    }
    Global.getSector().getMemoryWithoutUpdate()
        .set(MARKER_PREFIX + configured + MARKER_SUFFIX, true);
    if (action == CqDropMath.QUEST_MARK_AND_GRANT) {
      data.addOwned(itemId);
      CampaignUIAPI ui = Global.getSector().getCampaignUI();
      if (ui != null) {
        ui.addMessage(String.format(CqStrings.items("item_msg_collected"), f.getName()),
            Misc.getPositiveHighlightColor());
      }
    }
  }

  private static boolean flagSet(String flag) {
    if (Global.getSector().getMemoryWithoutUpdate().getBoolean(flag)) {
      return true;
    }
    return Global.getSector().getCharacterData() != null
        && Global.getSector().getCharacterData().getMemoryWithoutUpdate() != null
        && Global.getSector().getCharacterData().getMemoryWithoutUpdate().getBoolean(flag);
  }

  private static boolean redPlanetDone() {
    try {
      if (Global.getSector().getMemoryWithoutUpdate().contains(RED_PLANET_GLOBAL)) {
        return false;
      }
      return findRedPlanet() != null;
    } catch (Throwable t) {
    }
    return false;
  }

  static PlanetAPI findRedPlanet() {
    for (StarSystemAPI system : Global.getSector().getStarSystems()) {
      if (!system.hasTag(Tags.THEME_SPECIAL)) {
        continue;
      }
      for (PlanetAPI planet : system.getPlanets()) {
        if (RED_PLANET_SPEC.equals(
            planet.getMemoryWithoutUpdate().getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE))
            || hasLabLoot(planet)) {
          return planet;
        }
      }
    }
    return null;
  }

  private static boolean hasLabLoot(PlanetAPI planet) {
    boolean blueprints = false;
    boolean rareTech = false;
    for (SalvageEntityGenDataSpec.DropData drop : planet.getDropRandom()) {
      blueprints |= LAB_LOOT_BLUEPRINTS.equals(drop.group);
      rareTech |= LAB_LOOT_RARE_TECH.equals(drop.group);
    }
    return blueprints && rareTech;
  }
}
