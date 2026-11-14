package cq.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;

final class CqConsoleRoute extends BaseCustomUIPanelPlugin {

  static final CqConsoleRoute INSTANCE = new CqConsoleRoute();

  private CqConsoleRoute() {
  }

  @Override
  public void buttonPressed(Object buttonId) {
    CqConsolePanel.routeButton(buttonId);
  }
}
