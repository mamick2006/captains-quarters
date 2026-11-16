package cq.newgame;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

import cq.campaign.CqSettings;

public class CqFleetBuilder extends BaseCommandPlugin {

  public static final String SETTING = "cq_fleet_builder";

  public static final String NEX_FINISH_KEY = "$cqFleetBuilderNexFinish";

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params,
      Map<String, MemoryAPI> memoryMap) {
    if (dialog == null || params == null || params.isEmpty()) {
      return false;
    }
    String action = params.get(0).getString(memoryMap);
    if ("enabled".equals(action)) {
      return enabled(memoryMap);
    }
    if ("open".equals(action)) {
      if (!enabled(memoryMap)) {
        return false;
      }
      CqFleetBuilderPanel.open(dialog, memoryMap, null);
      return true;
    }
    if ("nexMark".equals(action)) {
      nexSurvival = enabled(memoryMap);
      return true;
    }
    if ("openNex".equals(action)) {
      Object finish = memoryMap.get(MemKeys.LOCAL).get(NEX_FINISH_KEY);
      if (!enabled(memoryMap) || !(finish instanceof CqFleetBuilderPanel.Finish)) {
        return false;
      }
      CqFleetBuilderPanel.open(dialog, memoryMap, (CqFleetBuilderPanel.Finish) finish);
      return true;
    }
    return false;
  }

  private static boolean nexSurvival = false;

  public static boolean nexSurvival() {
    return nexSurvival;
  }

  public static boolean enabled(Map<String, MemoryAPI> memoryMap) {
    if (!CqSettings.tunableBool(SETTING, true)) {
      return false;
    }
    MemoryAPI local = memoryMap == null ? null : memoryMap.get(MemKeys.LOCAL);
    Object data = local == null ? null : local.get("$characterData");
    return data instanceof CharacterCreationData && ((CharacterCreationData) data).isExtMode();
  }
}
