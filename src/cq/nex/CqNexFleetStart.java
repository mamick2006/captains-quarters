package cq.nex;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;

import cq.campaign.CqStrings;
import cq.newgame.CqFleetBuilder;
import cq.newgame.CqFleetBuilderPanel;
import cq.newgame.CqFleetStart;
import cq.newgame.pure.CqStartRules.Flavour;
import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.customstart.CustomStart;

public class CqNexFleetStart extends CustomStart implements CqFleetBuilderPanel.Finish {

  @Override
  public boolean shouldShow() {
    return CqFleetBuilder.nexSurvival();
  }

  @Override
  public void execute(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
    memoryMap.get(MemKeys.LOCAL).set(CqFleetBuilder.NEX_FINISH_KEY, this, 0f);
    dialog.getOptionPanel().clearOptions();
    dialog.getOptionPanel().addOption(CqStrings.fleet("nex_open"), "cqFleetBuilderNexOpen");
    dialog.getOptionPanel().addOption(CqStrings.fleet("back"), "nex_NGCStartBack");
    CqFleetBuilderPanel.open(dialog, memoryMap, this);
  }

  @Override
  public void finish(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, Flavour flavour,
      boolean officer, List<String> variantIds, boolean chooseLocation) {
    CharacterCreationData data =
        (CharacterCreationData) memoryMap.get(MemKeys.LOCAL).get("$characterData");
    PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER);
    ExerelinSetupData.getInstance().freeStart = true;
    dialog.getOptionPanel().clearOptions();
    CampaignFleetAPI shown = FleetFactoryV3.createEmptyFleet(Factions.PLAYER, FleetTypes.PATROL_SMALL,
        null);
    for (String v : variantIds) {
      data.addStartingFleetMember(v, FleetMemberType.SHIP);
      FleetMemberAPI m = shown.getFleetData().addFleetMember(v);
      if (m != null && shown.getFleetData().getMembersListCopy().size() == 1) {
        shown.getFleetData().setFlagship(m);
        m.setCaptain(data.getPerson());
      }
      AddRemoveCommodity.addFleetMemberGainText(
          Global.getSettings().getVariant(v), dialog.getTextPanel());
    }
    shown.forceSync();
    dialog.getVisualPanel().showFleetInfo(CqStrings.fleet("fleet_heading"), shown, null, null);
    data.addScript(new CqFleetStart.Outfit(flavour, true));
    data.addScript(new CqFleetStart.Provision(variantIds.size(), officer && flavour.officerAllowed));
    if (chooseLocation) {
      data.addScript(CqFleetStart.locationPrompt());
    }
    FireBest.fire(null, dialog, memoryMap, "ExerelinNGCStep4");
  }
}
