package cq.campaign;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;

import cq.api.CqFurnishingEffect;
import cq.items.CqCatalog;

public final class CqPluginEffects {

  private CqPluginEffects() {
  }

  public static void applyFleet(CqCatalog.Furnishing f, MutableFleetStatsAPI fleetStats,
      MutableCharacterStatsAPI commanderStats) {
    if (!f.isPluginActive()) {
      return;
    }
    CqFurnishingEffect plugin = f.getEffectPlugin();
    try {
      plugin.applyFleetAndCharacter(f, f.getId(), fleetStats, commanderStats);
    } catch (Throwable t) {
      f.notePluginFailure("applyFleetAndCharacter", t);
    }
  }

  public static void clearFleet(CqCatalog.Furnishing f, MutableFleetStatsAPI fleetStats,
      MutableCharacterStatsAPI commanderStats) {
    if (!f.isPluginActive()) {
      return;
    }
    CqFurnishingEffect plugin = f.getEffectPlugin();
    try {
      plugin.clearFleetAndCharacter(f, f.getId(), fleetStats, commanderStats);
    } catch (Throwable t) {
      f.notePluginFailure("clearFleetAndCharacter", t);
    }
  }

  public static void applyMember(CqCatalog.Furnishing f, FleetMemberAPI member) {
    if (!f.isPluginActive()) {
      return;
    }
    CqFurnishingEffect plugin = f.getEffectPlugin();
    try {
      plugin.applyPerMember(f, f.getId(), member);
    } catch (Throwable t) {
      f.notePluginFailure("applyPerMember", t);
    }
  }

  public static void clearMember(CqCatalog.Furnishing f, FleetMemberAPI member) {
    if (!f.isPluginActive()) {
      return;
    }
    CqFurnishingEffect plugin = f.getEffectPlugin();
    try {
      plugin.clearPerMember(f, f.getId(), member);
    } catch (Throwable t) {
      f.notePluginFailure("clearPerMember", t);
    }
  }

  public static void applyShip(CqCatalog.Furnishing f, String modId,
      MutableShipStatsAPI shipStats) {
    if (!f.isPluginActive()) {
      return;
    }
    CqFurnishingEffect plugin = f.getEffectPlugin();
    try {
      plugin.applyPilotedShip(f, modId, shipStats);
    } catch (Throwable t) {
      f.notePluginFailure("applyPilotedShip", t);
    }
  }
}
