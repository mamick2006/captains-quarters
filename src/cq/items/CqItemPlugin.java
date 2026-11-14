package cq.items;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import cq.campaign.CqData;
import cq.campaign.CqStrings;
import cq.campaign.acquire.CqSalvageSeeder;
import cq.ui.CqUi;

public class CqItemPlugin extends BaseSpecialItemPlugin {

  private CqCatalog.Furnishing furnishing;

  @Override
  public void setId(String id) {
    super.setId(id);
    furnishing = CqCatalog.get(id);
  }

  @Override
  public boolean hasRightClickAction() {
    return true;
  }

  @Override
  public boolean shouldRemoveOnRightClickAction() {
    return canCollect() && !CqData.getInstance().isOwned(getId());
  }

  @Override
  public void performRightClickAction() {
    if (!canCollect()) {
      return;
    }
    CqData data = CqData.getInstance();
    if (data.isOwned(getId())) {
      Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
          String.format(CqStrings.items("item_msg_duplicate"), getName()));
      return;
    }
    data.addOwned(getId());
    CqSalvageSeeder.refreshSeeded();
    Global.getSoundPlayer().playUISound("ui_chip_pickup", 1f, 1f);
    Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
        String.format(CqStrings.items("item_msg_collected"), getName()));
  }

  private boolean canCollect() {
    return furnishing != null && Global.getSector() != null;
  }

  @Override
  public void createTooltip(TooltipMakerAPI tooltip, boolean expanded,
      CargoTransferHandlerAPI transferHandler, Object stackSource) {
    float opad = 10f;
    Color gray = Misc.getGrayColor();

    tooltip.addTitle(getName());
    Misc.addDesignTypePara(tooltip, getDesignType(), opad);
    CqUi.furnishingCardCore(tooltip, getId());
    if (furnishing == null) {
      return;
    }

    addCostLabel(tooltip, opad, transferHandler, stackSource);

    if (Global.CODEX_TOOLTIP_MODE || Global.getSector() == null) {
      return;
    }
    if (CqData.getInstance().isOwned(getId())) {
      tooltip.addPara(CqStrings.items("item_hint_owned"), gray, opad);
    } else {
      tooltip.addPara(CqStrings.items("item_hint_collect"),
          Misc.getPositiveHighlightColor(), opad);
    }
  }

  @Override
  public boolean isTooltipExpandable() {
    return false;
  }
}
