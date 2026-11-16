package cq.newgame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import cq.campaign.CqStrings;
import cq.ui.CqUi;

public final class CqStartLocationPrompt implements EveryFrameScript {

  private boolean done = false;

  @Override
  public boolean isDone() {
    return done;
  }

  @Override
  public boolean runWhilePaused() {
    return true;
  }

  @Override
  public void advance(float amount) {
    if (done) {
      return;
    }
    CampaignUIAPI ui = Global.getSector().getCampaignUI();
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    if (ui == null || fleet == null || ui.isShowingDialog() || ui.isShowingMenu()
        || ui.getCurrentInteractionDialog() != null) {
      return;
    }
    done = true;
    List<MarketAPI> markets = CqFleetStart.startMarkets();
    if (markets.isEmpty()) {
      ui.addMessage(CqStrings.fleet("loc_none"));
      return;
    }
    ui.showInteractionDialog(new Host(markets), fleet);
  }

  static List<MarketAPI> sorted(List<MarketAPI> markets) {
    List<MarketAPI> out = new ArrayList<MarketAPI>(markets);
    Collections.sort(out, new Comparator<MarketAPI>() {
      @Override
      public int compare(MarketAPI a, MarketAPI b) {
        int c = a.getStarSystem().getBaseName().compareToIgnoreCase(b.getStarSystem().getBaseName());
        return c != 0 ? c : a.getName().compareToIgnoreCase(b.getName());
      }
    });
    return out;
  }

  private static final class Host implements InteractionDialogPlugin {
    private final List<MarketAPI> markets;
    private InteractionDialogAPI dialog;

    Host(List<MarketAPI> markets) {
      this.markets = sorted(markets);
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
      this.dialog = dialog;
      dialog.hideTextPanel();
      float sw = Global.getSettings().getScreenWidth();
      float sh = Global.getSettings().getScreenHeight();
      dialog.showCustomVisualDialog(Math.min(620f, sw - 80f), Math.min(700f, sh - 120f),
          new ListPanel(this));
    }

    void close() {
      if (dialog != null) {
        dialog.dismiss();
      }
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
      return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
      return null;
    }
  }

  private static final class ListPanel extends CqOpaque implements CustomVisualDialogDelegate {
    private static final float ROW_H = 30f;
    private final Host host;
    private DialogCallbacks callbacks;
    private final BaseCustomUIPanelPlugin router = new BaseCustomUIPanelPlugin() {
      @Override
      public void buttonPressed(Object buttonId) {
        ListPanel.this.buttonPressed(buttonId);
      }
    };

    ListPanel(Host host) {
      this.host = host;
    }

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
      this.callbacks = callbacks;
      float w = panel.getPosition().getWidth();
      float h = panel.getPosition().getHeight();
      CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
      String here = fleet.getContainingLocation() == null ? ""
          : fleet.getContainingLocation().getNameWithLowercaseTypeShort();

      TooltipMakerAPI head = panel.createUIElement(w, 70f, false);
      head.addSectionHeading(s("loc_title"), Alignment.MID, 0f);
      head.addPara(String.format(s("loc_intro"), here), CqUi.text(), 8f);
      panel.addUIElement(head).inTL(0f, 0f);

      TooltipMakerAPI list = panel.createUIElement(w, h - 70f - 40f, true);
      float rowW = w - 16f;
      for (MarketAPI m : host.markets) {
        CustomPanelAPI row = panel.createCustomPanel(rowW, ROW_H, router);
        TooltipMakerAPI text = row.createUIElement(rowW - 110f, ROW_H, false);
        text.addPara(String.format(s("loc_row"), m.getName(), m.getStarSystem().getBaseName(),
            m.getFaction().getDisplayName()), m.getFaction().getBaseUIColor(), 6f);
        row.addUIElement(text).inTL(0f, 0f);
        TooltipMakerAPI go = row.createUIElement(100f, 24f, false);
        go.addButton(s("loc_go"), m, CqUi.base(), CqUi.dark(), 100f, 24f, 0f);
        row.addUIElement(go).inTL(rowW - 100f, 3f);
        list.addCustom(row, 2f);
      }
      panel.addUIElement(list).inTL(0f, 70f);

      TooltipMakerAPI foot = panel.createUIElement(w, 30f, false);
      foot.addButton(s("loc_stay"), "stay", CqUi.base(), CqUi.dark(), 160f, 24f, 0f);
      panel.addUIElement(foot).inTL(0f, h - 28f);
    }

    @Override
    public void buttonPressed(Object buttonId) {
      if (buttonId instanceof MarketAPI) {
        CqFleetStart.moveTo(Global.getSector().getPlayerFleet(), (MarketAPI) buttonId);
      }
      callbacks.dismissDialog();
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
      return this;
    }

    @Override
    public float getNoiseAlpha() {
      return 0f;
    }

    @Override
    public void reportDismissed(int option) {
      host.close();
    }

    private static String s(String key) {
      return CqStrings.fleet(key);
    }
  }
}
