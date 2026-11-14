package cq.api;

import java.util.List;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;

import cq.items.CqCatalog.Furnishing;

public class BaseCqFurnishingEffect implements CqFurnishingEffect {

  @Override
  public void applyFleetAndCharacter(Furnishing furnishing, String modId,
      MutableFleetStatsAPI fleetStats, MutableCharacterStatsAPI commanderStats) {
  }

  @Override
  public void clearFleetAndCharacter(Furnishing furnishing, String modId,
      MutableFleetStatsAPI fleetStats, MutableCharacterStatsAPI commanderStats) {
  }

  @Override
  public void applyPerMember(Furnishing furnishing, String modId, FleetMemberAPI member) {
  }

  @Override
  public void clearPerMember(Furnishing furnishing, String modId, FleetMemberAPI member) {
  }

  @Override
  public void applyPilotedShip(Furnishing furnishing, String modId,
      MutableShipStatsAPI shipStats) {
  }

  @Override
  public List<String> effectLines(Furnishing furnishing) {
    return null;
  }

  @Override
  public String[] effectHighlights(Furnishing furnishing) {
    return null;
  }
}
