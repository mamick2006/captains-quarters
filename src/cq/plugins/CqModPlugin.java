package cq.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import cq.campaign.CqData;
import cq.campaign.CqLunaListener;
import cq.campaign.CqSettings;
import cq.intel.CqIntel;
import cq.topbar.CqMgmtInjector;
import cq.ui.CqConsolePanel;
import cq.ui.CqMgmtScreen;

public class CqModPlugin extends BaseModPlugin {

  @Override
  public void onApplicationLoad() {
    if (!Global.getSettings().getModManager().isModEnabled("lunalib")) {
      return;
    }
    try {
      CqLunaListener.register();
    } catch (Throwable t) {
      Global.getLogger(CqModPlugin.class).warn("Captain's Quarters: could not register for LunaLib "
          + "settings changes. A changed salvage setting reaches stations already found at the "
          + "next furnishing pickup.", t);
    }
  }

  @Override
  public void onNewGame() {
  }

  @Override
  public void onGameLoad(boolean newGame) {
    SectorAPI sector = Global.getSector();
    if (sector == null) {
      return;
    }
    CqConsolePanel.resetStaticsForLoad();
    CqMgmtInjector.resetStaticsForLoad();
    CqMgmtScreen.resetStaticsForLoad();
    CqData.getInstance();
    CqIntel.ensureExists();
    if (!CqSettings.tunableBool("cq_mgmt_intel_only", false)) {
      try {
        if (!sector.hasTransientScript(CqMgmtInjector.class)) {
          sector.addTransientScript(new CqMgmtInjector());
        }
      } catch (Throwable ignore) {
      }
    }
    cq.campaign.CqEffects.registerAll(sector);
  }
}
