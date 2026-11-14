package cq.api;

import java.util.List;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;

import cq.items.CqCatalog.Furnishing;

public interface CqFurnishingEffect {

  void applyFleetAndCharacter(Furnishing furnishing, String modId,
      MutableFleetStatsAPI fleetStats, MutableCharacterStatsAPI commanderStats);

  void clearFleetAndCharacter(Furnishing furnishing, String modId,
      MutableFleetStatsAPI fleetStats, MutableCharacterStatsAPI commanderStats);

  void applyPerMember(Furnishing furnishing, String modId, FleetMemberAPI member);

  void clearPerMember(Furnishing furnishing, String modId, FleetMemberAPI member);

  void applyPilotedShip(Furnishing furnishing, String modId, MutableShipStatsAPI shipStats);

  List<String> effectLines(Furnishing furnishing);

  String[] effectHighlights(Furnishing furnishing);
}
