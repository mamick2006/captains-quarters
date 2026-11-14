package cq.effects;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;

import cq.effects.pure.CqRecoveryMath;

final class CqDmods {

  private CqDmods() {
  }

  static List<String> on(ShipVariantAPI variant) {
    List<String> dmods = new ArrayList<String>();
    if (variant == null) {
      return dmods;
    }
    for (String id : variant.getHullMods()) {
      HullModSpecAPI mod = Global.getSettings().getHullModSpec(id);
      if (mod != null && mod.hasTag(Tags.HULLMOD_DMOD)) {
        dmods.add(id);
      }
    }
    return dmods;
  }

  static boolean hullUnrestorable(ShipHullSpecAPI spec) {
    return spec != null && spec.hasTag(Tags.HULL_UNRESTORABLE);
  }

  static boolean variantUnrestorable(ShipVariantAPI variant) {
    return variant != null && variant.hasTag(Tags.VARIANT_UNRESTORABLE);
  }

  static boolean restorable(FleetMemberAPI member) {
    return !hullUnrestorable(member.getHullSpec()) && !variantUnrestorable(member.getVariant());
  }

  static int spare(FleetMemberAPI member, int leave) {
    ShipHullSpecAPI spec = member.getHullSpec();
    ShipVariantAPI variant = member.getVariant();
    if (spec == null || variant == null || !restorable(member)) {
      return 0;
    }
    return CqRecoveryMath.dmodsToRemove(on(variant), spec.getBuiltInMods(), Integer.MAX_VALUE,
        leave).size();
  }

  static int remove(FleetMemberAPI member, List<String> dmods, int fewer, int leave) {
    ShipVariantAPI variant = member.getVariant();
    ShipHullSpecAPI spec = member.getHullSpec();
    if (variant == null || spec == null || fewer <= 0) {
      return 0;
    }
    List<String> remove = CqRecoveryMath.dmodsToRemove(dmods, spec.getBuiltInMods(), fewer,
        leave);
    if (remove.isEmpty()) {
      return 0;
    }
    if (variant.isStockVariant()) {
      variant = variant.clone();
      variant.setSource(VariantSource.REFIT);
      member.setVariant(variant, false, false);
    }
    for (String id : remove) {
      DModManager.removeDMod(variant, id);
    }
    if (spec.isDefaultDHull() && DModManager.getNumNonBuiltInDMods(variant) <= 0
        && spec.getDParentHull() != null) {
      variant.setHullSpecAPI(spec.getDParentHull());
    }
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (fleet != null) {
      fleet.getFleetData().setSyncNeeded();
    }
    return remove.size();
  }
}
