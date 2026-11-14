package cq.intel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import cq.campaign.CqData;
import cq.campaign.CqSlotMath;
import cq.campaign.CqStrings;
import cq.ui.CqFrame;
import cq.ui.CqPickerRail;
import cq.ui.CqSlots;
import cq.ui.CqQuartersView;
import cq.ui.CqUi;

public class CqIntel extends BaseIntelPlugin {

  public static final String TAG = "Quarters";

  private static final String BTN_REBUILD = "cq_btn_rebuild";

  private static final String BTN_DESELECT = "cq_btn_deselect";

  private transient String uiSelectedSlot;

  private transient boolean uiPickerOpen;

  private transient boolean uiViewerOpen;

  private transient String uiAsk;

  private transient boolean uiAskDrawn;

  private transient IntelUIAPI lastIntelUi;

  public static CqIntel get() {
    if (Global.getSector() == null) {
      return null;
    }
    return (CqIntel) Global.getSector().getIntelManager().getFirstIntel(CqIntel.class);
  }

  public static void ensureExists() {
    if (Global.getSector() == null || !CqData.exists()) {
      return;
    }
    IntelManagerAPI im = Global.getSector().getIntelManager();
    if (get() == null) {
      im.addIntel(new CqIntel(), true);
    }
  }

  @Override
  public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
    info.addPara(getName(), getTitleColor(mode), 0f);
    if (!CqData.exists()) {
      return;
    }
    List<CqSlots.Entry> slots = CqSlots.list();
    String[] occupants = new String[slots.size()];
    boolean[] decorative = new boolean[slots.size()];
    for (int i = 0; i < occupants.length; i++) {
      occupants[i] = slots.get(i).occupant;
      decorative[i] = slots.get(i).decorative;
    }
    int furnished = CqSlotMath.furnishedRegular(occupants, decorative);
    info.addPara(CqStrings.intel("intel_status"), 3f, CqUi.hl(),
        CqUi.i(furnished), CqUi.i(CqSlots.slotCount()));
  }

  @Override
  public boolean hasLargeDescription() {
    return true;
  }

  @Override
  public boolean hasSmallDescription() {
    return false;
  }

  @Override
  public boolean isHidden() {
    return cq.topbar.CqMgmtInjector.isAlive();
  }

  @Override
  public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
    if (!CqData.exists()) {
      TooltipMakerAPI e = panel.createUIElement(width, height, false);
      e.addPara(CqStrings.intel("intel_no_data"), CqUi.gray(), 0f);
      panel.addUIElement(e).inTL(0f, 0f);
      return;
    }
    CqData data = CqData.getInstance();
    try {
      CqFrame fr = CqFrame.of(width, height, CqPickerRail.RAIL_W);
      if (uiAsk != null && uiAskDrawn) {
        uiAsk = null;
      }
      uiAskDrawn = uiAsk != null;
      CqQuartersView.add(panel, 0f, fr, data, uiSelectedSlot);
      CqPickerRail.add(panel, fr.shelfX, fr.shelfW, height, data, uiSelectedSlot, uiPickerOpen,
          uiViewerOpen, uiAsk);
    } catch (Throwable t) {
      buildFailWarn(t);
      TooltipMakerAPI e = panel.createUIElement(width, height, false);
      e.addPara("The quarters view failed to build.", CqUi.gray(), 0f);
      panel.addUIElement(e).inTL(0f, 0f);
    }
  }

  private static volatile boolean buildFailWarned;

  private static void buildFailWarn(Throwable t) {
    if (buildFailWarned) {
      return;
    }
    buildFailWarned = true;
    Global.getLogger(CqIntel.class).warn("Captain's Quarters: the quarters screen failed to build"
        + " (likely an API mismatch or bad settings data). The pane shows a notice instead;"
        + " the tab and the intel entry stay usable after the cause is fixed.", t);
  }

  public boolean handleButton(Object buttonId, IntelUIAPI ui) {
    if (!(buttonId instanceof String) || !CqData.exists()) {
      return false;
    }
    String id = (String) buttonId;
    String asked = uiAsk;
    uiAsk = null;
    boolean rebuild = dispatch(id, CqData.getInstance(), asked);
    return rebuild || asked != null;
  }

  private static boolean canAsk(String kind) {
    Long price = CqPickerRail.ASK_DECO.equals(kind)
        ? CqSlots.nextDecorativePrice() : CqSlots.nextPrice();
    if (price == null) {
      return false;
    }
    try {
      return (long) Global.getSector().getPlayerFleet().getCargo().getCredits().get()
          >= price.longValue();
    } catch (Throwable t) {
      return false;
    }
  }

  private boolean dispatch(String id, CqData data, String asked) {
    if (BTN_REBUILD.equals(id)) {
      return true;
    }

    if (id.startsWith(CqQuartersView.BTN_STYLE_PREFIX)) {
      String styleId = id.substring(CqQuartersView.BTN_STYLE_PREFIX.length());
      if (CqData.STYLE_IDS.contains(styleId) && !styleId.equals(data.getStyle())) {
        data.setStyle(styleId);
      }
      return true;
    }

    if (BTN_DESELECT.equals(id)) {
      if (uiSelectedSlot == null) {
        return false;
      }
      uiSelectedSlot = null;
      uiPickerOpen = false;
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_MOVE_PREFIX)) {
      return CqQuartersView.toggleCarry(id.substring(CqPickerRail.BTN_MOVE_PREFIX.length()));
    }

    if (id.startsWith(CqQuartersView.BTN_STICKER_PREFIX)) {
      String slotId = id.substring(CqQuartersView.BTN_STICKER_PREFIX.length());
      if (CqPickerRail.slotNumber(slotId) == null) {
        return false;
      }
      selectionChangePutBack(slotId);
      uiSelectedSlot = slotId;
      uiViewerOpen = false;
      return true;
    }

    if (id.startsWith(CqQuartersView.BTN_SLOT_PREFIX)) {
      String slotId = id.substring(CqQuartersView.BTN_SLOT_PREFIX.length());
      if (CqPickerRail.slotNumber(slotId) == null) {
        return false;
      }
      selectionChangePutBack(slotId);
      uiSelectedSlot = slotId;
      uiPickerOpen = CqPickerRail.occupantOf(slotId) == null;
      uiViewerOpen = false;
      CqSlots.Entry pressed = CqSlots.byId(slotId);
      if (pressed != null && pressed.occupant != null && !pressed.placed()) {
        placeAtCentre(slotId, pressed.occupant);
      }
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_CHANGE_PREFIX)) {
      String slotId = id.substring(CqPickerRail.BTN_CHANGE_PREFIX.length());
      if (CqPickerRail.slotNumber(slotId) == null) {
        return false;
      }
      selectionChangePutBack(slotId);
      uiSelectedSlot = slotId;
      uiPickerOpen = true;
      uiViewerOpen = false;
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_PICK_PREFIX)) {
      if (uiSelectedSlot == null) {
        return false;
      }
      String itemId = id.substring(CqPickerRail.BTN_PICK_PREFIX.length());
      CqSlots.Entry slot = CqSlots.byId(uiSelectedSlot);
      if (slot == null || itemId.equals(slot.occupant)) {
        return false;
      }
      boolean wasUnplaced = !slot.placed();
      boolean applied;
      if (!slot.decorative && CqSlots.slotNumberOf(itemId) != null) {
        applied = CqSlots.moveItemToSlot(itemId, uiSelectedSlot);
      } else {
        String previous = slot.occupant;
        if (previous != null) {
          CqSlots.resetSlot(uiSelectedSlot);
        }
        applied = CqSlots.setOccupant(uiSelectedSlot, itemId);
        if (!applied && previous != null) {
          CqSlots.setOccupant(uiSelectedSlot, previous);
        }
      }
      if (!applied) {
        return true;
      }
      if (wasUnplaced) {
        placeAtCentre(uiSelectedSlot, itemId);
      } else {
        CqQuartersView.clampIntoCanvas(uiSelectedSlot);
      }
      uiPickerOpen = false;
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_RESET_PREFIX)) {
      String slotId = id.substring(CqPickerRail.BTN_RESET_PREFIX.length());
      if (CqPickerRail.occupantOf(slotId) == null) {
        return false;
      }
      CqSlots.resetSlot(slotId);
      return true;
    }

    if (CqPickerRail.BTN_ADD_SLOT.equals(id) || CqPickerRail.BTN_ADD_DECORATIVE.equals(id)) {
      String kind = CqPickerRail.BTN_ADD_SLOT.equals(id)
          ? CqPickerRail.ASK_SLOT : CqPickerRail.ASK_DECO;
      if (canAsk(kind)) {
        uiAsk = kind;
        uiAskDrawn = false;
      }
      return true;
    }

    if (CqPickerRail.BTN_ADD_CONFIRM.equals(id)) {
      if (CqPickerRail.ASK_SLOT.equals(asked)) {
        CqSlots.purchaseSlot();
      } else if (CqPickerRail.ASK_DECO.equals(asked)) {
        CqSlots.addDecorativeSlot();
      }
      return true;
    }

    if (CqPickerRail.BTN_ADD_CANCEL.equals(id)) {
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_SCALE_UP_PREFIX)) {
      CqQuartersView.stepScale(id.substring(CqPickerRail.BTN_SCALE_UP_PREFIX.length()), 1);
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_SCALE_DOWN_PREFIX)) {
      CqQuartersView.stepScale(id.substring(CqPickerRail.BTN_SCALE_DOWN_PREFIX.length()), -1);
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_FLIP_PREFIX)) {
      CqQuartersView.toggleFlip(id.substring(CqPickerRail.BTN_FLIP_PREFIX.length()));
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_ROT_CCW_PREFIX)) {
      CqQuartersView.stepRotate(id.substring(CqPickerRail.BTN_ROT_CCW_PREFIX.length()), 1);
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_ROT_CW_PREFIX)) {
      CqQuartersView.stepRotate(id.substring(CqPickerRail.BTN_ROT_CW_PREFIX.length()), -1);
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_Z_FWD_PREFIX)) {
      CqQuartersView.stepZ(id.substring(CqPickerRail.BTN_Z_FWD_PREFIX.length()), -1);
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_Z_BACK_PREFIX)) {
      CqQuartersView.stepZ(id.substring(CqPickerRail.BTN_Z_BACK_PREFIX.length()), 1);
      return true;
    }

    if (CqPickerRail.BTN_COLLECTION.equals(id)) {
      selectionChangePutBack(null);
      uiViewerOpen = true;
      uiSelectedSlot = null;
      uiPickerOpen = false;
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_LAYOUT_SAVE_PREFIX)) {
      int index = CqPickerRail.layoutIndexOf(id, CqPickerRail.BTN_LAYOUT_SAVE_PREFIX);
      if (index < 0) {
        return false;
      }
      data.saveLayout(index);
      return true;
    }

    if (id.startsWith(CqPickerRail.BTN_LAYOUT_LOAD_PREFIX)) {
      int index = CqPickerRail.layoutIndexOf(id, CqPickerRail.BTN_LAYOUT_LOAD_PREFIX);
      if (index < 0 || data.isLayoutEmpty(index)) {
        return false;
      }
      selectionChangePutBack(null);
      List<String> restored = new ArrayList<String>();
      for (CqData.Slot entry : data.getLayout(index).getSlots()) {
        restored.add(entry.getId());
      }
      data.loadLayout(index);
      for (String slotId : restored) {
        CqQuartersView.clampIntoCanvas(slotId);
      }
      uiSelectedSlot = null;
      uiPickerOpen = false;
      return true;
    }

    if (CqPickerRail.BTN_BACK.equals(id)) {
      selectionChangePutBack(null);
      uiSelectedSlot = null;
      uiPickerOpen = false;
      uiViewerOpen = false;
      return true;
    }

    return false;
  }

  private static void placeAtCentre(String slotId, String itemId) {
    float[] dims = CqQuartersView.stickerDims(itemId);
    CqSlots.setStickerPos(slotId,
        (CqQuartersView.FRAME_W - dims[0]) / 2f, (CqQuartersView.FRAME_H - dims[1]) / 2f);
    CqQuartersView.pulseOnNextBuild(slotId);
  }

  private static void selectionChangePutBack(String incomingSelection) {
    String carried = CqQuartersView.carriedSlot();
    if (carried != null && !carried.equals(incomingSelection)) {
      CqQuartersView.putBackForSelectionChange();
    }
  }

  public static void requestRebuild() {
    CqIntel intel = get();
    if (intel == null) {
      return;
    }
    cq.ui.CqConsolePanel.routeButton(BTN_REBUILD);
    if (intel.lastIntelUi != null) {
      try {
        intel.lastIntelUi.updateUIForItem(intel);
      } catch (Throwable ignore) {
      }
    }
  }

  public static void requestDeselect() {
    CqIntel intel = get();
    if (intel == null) {
      return;
    }
    cq.ui.CqConsolePanel.routeButton(BTN_DESELECT);
    if (intel.handleButton(BTN_DESELECT, null) && intel.lastIntelUi != null) {
      try {
        intel.lastIntelUi.updateUIForItem(intel);
      } catch (Throwable ignore) {
      }
    }
  }

  public static void requestClusterVerb(String buttonId) {
    CqIntel intel = get();
    if (intel == null) {
      return;
    }
    if (cq.ui.CqConsolePanel.hasLive()) {
      cq.ui.CqConsolePanel.routeButton(buttonId);
      return;
    }
    intel.saveConsoleScroll();
    if (intel.handleButton(buttonId, null) && intel.lastIntelUi != null) {
      try {
        intel.lastIntelUi.updateUIForItem(intel);
      } catch (Throwable ignore) {
      }
    }
  }

  @Override
  public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
    if (!CqData.exists()) {
      return;
    }
    lastIntelUi = ui;
    saveConsoleScroll();
    if (handleButton(buttonId, ui)) {
      ui.updateUIForItem(this);
    }
  }

  public void saveConsoleScroll() {
    CqPickerRail.saveScroll();
  }

  @Override
  public String getName() {
    return "Captain's Quarters";
  }

  @Override
  public String getSmallDescriptionTitle() {
    return getName();
  }

  @Override
  public String getIcon() {
    String own = CqUi.sprite("icons", "cq_intel");
    if (own != null) {
      return own;
    }
    return Global.getSettings().getSpriteName("intel", "fleet_log");
  }

  @Override
  public Set<String> getIntelTags(SectorMapAPI map) {
    Set<String> tags = super.getIntelTags(map);
    tags.add(TAG);
    return tags;
  }

  @Override
  public String getSortString() {
    return getName();
  }

  @Override
  public boolean shouldRemoveIntel() {
    return false;
  }

  @Override
  public IntelSortTier getSortTier() {
    return IntelSortTier.TIER_2;
  }
}
