package cq.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import cq.campaign.acquire.CqDropMath;

public final class CqSurvival {

  public static final String FINDS_KEY = "cq_survival_finds";

  public static final String SPECIAL_ITEM_FRACTION_KEY = "ext_specialItemSalvageDropFraction";

  private CqSurvival() {
  }

  public static boolean isSurvivalCampaign() {
    SectorAPI sector = Global.getSector();
    return sector != null && sector.isExtMode();
  }

  public static boolean findsCutOn() {
    return CqSettings.tunableBool(FINDS_KEY, true);
  }

  public static float findMult() {
    if (!isSurvivalCampaign() || !findsCutOn()) {
      return 1f;
    }
    return CqDropMath.survivalFindMult(
        CqSettings.getFloat(SPECIAL_ITEM_FRACTION_KEY, CqDropMath.VANILLA_SPECIAL_ITEM_FRACTION));
  }
}
