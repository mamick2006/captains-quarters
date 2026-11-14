package cq.api;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.util.Misc;

import cq.campaign.CqData;
import cq.campaign.CqStrings;
import cq.campaign.acquire.CqSalvageSeeder;
import cq.items.CqCatalog;

public final class CaptainsQuartersAPI {

  private CaptainsQuartersAPI() {
  }

  public static final int CONTRACT_VERSION = 1;

  public static boolean isAvailable() {
    try {
      return Global.getSector() != null && CqData.exists();
    } catch (Throwable t) {
      return false;
    }
  }

  public static boolean isKnownFurnishing(String itemId) {
    try {
      return CqCatalog.get(itemId) != null;
    } catch (Throwable t) {
      return false;
    }
  }

  public static boolean isOwned(String itemId) {
    try {
      return isAvailable() && CqData.getInstance().isOwned(itemId);
    } catch (Throwable t) {
      return false;
    }
  }

  public static boolean grant(String itemId) {
    try {
      if (!isAvailable()) {
        return false;
      }
      CqCatalog.Furnishing f = CqCatalog.get(itemId);
      if (f == null) {
        return false;
      }
      CqData data = CqData.getInstance();
      if (!data.addOwned(f.getId())) {
        return false;
      }
      CqSalvageSeeder.refreshSeeded();
      CampaignUIAPI ui = Global.getSector().getCampaignUI();
      if (ui != null) {
        ui.addMessage(String.format(CqStrings.items("item_msg_collected"), f.getName()),
            Misc.getPositiveHighlightColor());
      }
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  public static boolean grantOnce(String itemId, String memoryKey) {
    try {
      if (!isAvailable() || CqCatalog.get(itemId) == null) {
        return false;
      }
      String key = memoryKey == null || memoryKey.isEmpty()
          ? "$cq_granted_" + itemId : memoryKey;
      if (Global.getSector().getMemoryWithoutUpdate().getBoolean(key)) {
        return false;
      }
      Global.getSector().getMemoryWithoutUpdate().set(key, true);
      return grant(itemId);
    } catch (Throwable t) {
      return false;
    }
  }

  public static boolean giveAsCargo(String itemId, CargoAPI into) {
    try {
      if (into == null) {
        return false;
      }
      CqCatalog.Furnishing f = CqCatalog.get(itemId);
      if (f == null) {
        return false;
      }
      into.addSpecial(new SpecialItemData(f.getId(), null), 1);
      return true;
    } catch (Throwable t) {
      return false;
    }
  }
}
