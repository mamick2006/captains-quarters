package cq.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;

public class CqFlagshipTracker {

  public static final String HULLMOD_ID = "cq_quarters_effects";

  private FleetMemberAPI lastFlagship;
  private boolean sweptOnce;

  public boolean maintain() {
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (fleet == null) {
      return false;
    }
    FleetMemberAPI flagship = fleet.getFlagship();
    if (flagship == lastFlagship && (flagship == null || hasMod(flagship))) {
      return false;
    }
    boolean changed = false;
    if (!sweptOnce || flagship != lastFlagship) {
      sweptOnce = true;
      for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
        if (member != flagship && hasMod(member)) {
          removeMod(member);
          changed = true;
        }
      }
    }
    if (flagship != null && !hasMod(flagship)) {
      addMod(flagship);
      changed = true;
    }
    lastFlagship = flagship;
    return changed;
  }

  private static boolean hasMod(FleetMemberAPI member) {
    return member.getVariant() != null && member.getVariant().getHullMods().contains(HULLMOD_ID);
  }

  private static void addMod(FleetMemberAPI member) {
    if (member.getVariant() == null) {
      return;
    }
    if (member.getVariant().getSource() != VariantSource.REFIT) {
      cycleToCustomVariant(member);
    }
    member.getVariant().addPermaMod(HULLMOD_ID);
  }

  private static void removeMod(FleetMemberAPI member) {
    ShipVariantAPI variant = member.getVariant();
    if (variant == null) {
      return;
    }
    variant.removePermaMod(HULLMOD_ID);
    variant.removeMod(HULLMOD_ID);
  }

  private static void cycleToCustomVariant(FleetMemberAPI member) {
    ShipVariantAPI variant = member.getVariant().clone();
    variant.setOriginalVariant(null);
    variant.setHullVariantId(Misc.genUID());
    variant.setSource(VariantSource.REFIT);
    member.setVariant(variant, false, true);
  }
}
