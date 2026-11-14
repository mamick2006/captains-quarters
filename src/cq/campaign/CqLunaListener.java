package cq.campaign;

import com.fs.starfarer.api.Global;

import cq.campaign.acquire.CqSalvageSeeder;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

public final class CqLunaListener implements LunaSettingsListener {

  public static void register() {
    if (!LunaSettings.hasSettingsListenerOfClass(CqLunaListener.class)) {
      LunaSettings.addSettingsListener(new CqLunaListener());
    }
  }

  @Override
  public void settingsChanged(String modId) {
    if (!CqSettings.MOD_ID.equals(modId) || Global.getSector() == null) {
      return;
    }
    CqSalvageSeeder.syncTables();
  }
}
