package cq.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import cq.campaign.CqData;
import cq.campaign.CqSlotMath;
import cq.campaign.CqStrings;
import cq.effects.pure.CqRailMarks;
import cq.items.CqCatalog;

public final class CqQuartersView {

  public static final String BTN_SLOT_PREFIX = "cq_btn_slot*";
  public static final String BTN_STICKER_PREFIX = "cq_btn_sticker*";
  public static final String BTN_STYLE_PREFIX = "cq_btn_style*";

  public static final float FRAME_W = 1600f;
  public static final float FRAME_H = 1000f;

  private static final float EL_INSET = 5f;
  private static final float TEXT_SLACK = 10f;
  private static final float MARK_GAP = 12f;
  private static final float STYLE_DIM = 0.6f;

  private static String pulseSlot;

  private static String pendingGrab;
  private static String followSlot;
  private static String followItem;
  private static float followDX;
  private static float followDY;
  private static float followFx;
  private static float followFy;

  private CqQuartersView() {
  }

  public static void pulseOnNextBuild(String slotId) {
    pulseSlot = slotId;
  }

  public static boolean isCarrying() {
    return followSlot != null || pendingGrab != null;
  }

  public static String carriedSlot() {
    return followSlot;
  }

  static String carriedOrPending() {
    return followSlot != null ? followSlot : pendingGrab;
  }

  public static float[] carryPos() {
    return new float[] { followFx, followFy };
  }

  public static boolean requestGrab(String slotId) {
    CqSlots.Entry e = CqSlots.byId(slotId);
    if (isCarrying() || e == null || e.occupant == null || !e.placed()) {
      return false;
    }
    pendingGrab = slotId;
    return true;
  }

  public static volatile int moveDispatchCount;

  public static boolean toggleCarry(String slotId) {
    moveDispatchCount++;
    if (slotId == null) {
      return false;
    }
    if (slotId.equals(followSlot)) {
      dropCarry();
      return false;
    }
    if (slotId.equals(pendingGrab)) {
      pendingGrab = null;
      return true;
    }
    return requestGrab(slotId);
  }

  static void dropCarry() {
    if (followSlot != null) {
      CqSlots.setStickerPos(followSlot, followFx, followFy);
    }
    clearCarry();
    cq.intel.CqIntel.requestRebuild();
  }

  static void cancelCarry() {
    clearCarry();
    cq.intel.CqIntel.requestRebuild();
  }

  private static void clearCarry() {
    pendingGrab = null;
    followSlot = null;
    followItem = null;
  }

  public static void putBackForSelectionChange() {
    clearCarry();
  }

  public static void resetFollowForLoad() {
    clearCarry();
  }

  public static void add(CustomPanelAPI host, float x, CqFrame fr, CqData data,
      String selectedSlot) {
    String style = data.getStyle();
    CanvasPlugin canvas = new CanvasPlugin(selectedSlot);
    CustomPanelAPI vp = host.createCustomPanel(fr.leftW, fr.ch, canvas);

    float scale = fr.cabinW / FRAME_W;
    float imgX = 0f;
    float imgY = 0f;
    canvas.setImage(imgX, imgY, fr.cabinW, fr.cabinH, scale, CqUi.backgroundSprite(style));

    List<CqSlots.Entry> zOrder = CqSlots.list();
    String pulse = pulseSlot;
    pulseSlot = null;
    for (int i = zOrder.size() - 1; i >= 0; i--) {
      CqSlots.Entry e = zOrder.get(i);
      if (e.occupant == null || !e.placed() || CqCatalog.get(e.occupant) == null) {
        continue;
      }
      addSticker(vp, canvas, e, selectedSlot, imgX, imgY, scale, e.id.equals(pulse));
    }

    CqSlots.Entry sel = selectedSlot == null ? null : CqSlots.byId(selectedSlot);
    if (sel != null && sel.occupant != null && sel.placed()) {
      clusterStrip(vp, canvas, data, sel, fr);
    }

    styleStrip(vp, fr, style);
    CqPickerRail.layoutStrip(vp, fr.layoutsX - EL_INSET, fr.layoutsY, data);

    float w = fr.leftW;
    float h = fr.ch;
    TooltipMakerAPI holder = host.createUIElement(w, h, false);
    holder.addCustom(vp, 0f);
    host.addUIElement(holder).inTL(x, 0f);
  }

  private static void clusterStrip(CustomPanelAPI vp, CanvasPlugin canvas, CqData data,
      CqSlots.Entry e, CqFrame fr) {
    float h = CqFrame.BUTTON_H;
    float bw = 24f;
    CqCatalog.Furnishing f = CqCatalog.get(e.occupant);
    boolean canGrow = e.scale + 0.001f < scaleMax();
    boolean canShrink = e.scale - 0.001f > scaleMin();

    String name = f == null ? "?" : f.getName();
    TooltipMakerAPI nameEl = vp.createUIElement(fr.leftW, CqFrame.EFFECT_DY, false);
    LabelAPI nameLabel = nameEl.addPara(name,
        f == null ? CqUi.gray() : f.getRarity().getColor(), 0f);
    vp.addUIElement(nameEl).inTL(-EL_INSET, fr.nameY);
    CqRailMarks.Mark mark = CqUi.markOf(data, e);
    if (mark != CqRailMarks.Mark.NONE) {
      float markW = Math.max(
          nameLabel.computeTextWidth(CqUi.markText(CqRailMarks.Mark.ACTIVE)),
          nameLabel.computeTextWidth(CqUi.markText(CqRailMarks.Mark.INACTIVE))) + TEXT_SLACK;
      TooltipMakerAPI markEl = vp.createUIElement(markW, CqFrame.EFFECT_DY, false);
      LabelAPI markLabel = CqUi.markLine(markEl, data, e, 0f);
      vp.addUIElement(markEl).inTL(
          nameLabel.computeTextWidth(name) + MARK_GAP - EL_INSET, fr.nameY);
      canvas.watchMark(markLabel, e.id, mark);
    }
    if (f != null && !e.decorative && f.hasEffect()) {
      float effectW = Math.max(120f, fr.leftW - 12f);
      TooltipMakerAPI effectEl = vp.createUIElement(effectW + TEXT_SLACK,
          CqFrame.STRIP_DY, false);
      CqUi.addEffectLine(effectEl, f, 0f);
      vp.addUIElement(effectEl).inTL(-EL_INSET, fr.effectY);
    }

    boolean carryingThis = e.id.equals(carriedOrPending());
    TooltipMakerAPI move = vp.createUIElement(56f, h, false);
    ButtonAPI moveB = move.addButton("Move", CqPickerRail.BTN_MOVE_PREFIX + e.id,
        CqUi.base(), CqUi.dark(), 54f, 22f, 0f);
    moveB.setShortcut(org.lwjgl.input.Keyboard.KEY_O, false);
    if (carryingThis) {
      moveB.setChecked(true);
      moveB.highlight();
    }
    move.addTooltipToPrevious(CqUi.tip(carryingThis
        ? CqStrings.mgmt("mgmt_move_tip_drop")
        : CqStrings.mgmt("mgmt_move_tip_lift")),
        TooltipMakerAPI.TooltipLocation.BELOW);
    vp.addUIElement(move).inTL(fr.buttonX(0) - EL_INSET, fr.buttonY(0));

    TooltipMakerAPI minus = vp.createUIElement(bw, h, false);
    ButtonAPI mb = minus.addButton("-", CqPickerRail.BTN_SCALE_DOWN_PREFIX + e.id,
        CqUi.base(), CqUi.dark(), bw - 2f, 22f, 0f);
    CqUi.setEnabledSayingWhy(mb, canShrink);
    minus.addTooltipToPrevious(CqUi.tip(canShrink
        ? CqStrings.mgmt("mgmt_scale_down_tip") : "Minimum size."),
        TooltipMakerAPI.TooltipLocation.BELOW);
    vp.addUIElement(minus).inTL(fr.buttonX(1) - EL_INSET, fr.buttonY(1));

    TooltipMakerAPI plus = vp.createUIElement(bw, h, false);
    ButtonAPI pb = plus.addButton("+", CqPickerRail.BTN_SCALE_UP_PREFIX + e.id,
        CqUi.base(), CqUi.dark(), bw - 2f, 22f, 0f);
    CqUi.setEnabledSayingWhy(pb, canGrow);
    plus.addTooltipToPrevious(CqUi.tip(canGrow
        ? CqStrings.mgmt("mgmt_scale_up_tip")
        : "Maximum size."), TooltipMakerAPI.TooltipLocation.BELOW);
    vp.addUIElement(plus).inTL(fr.buttonX(2) - EL_INSET, fr.buttonY(2));

    TooltipMakerAPI flip = vp.createUIElement(40f, h, false);
    ButtonAPI flipB = flip.addButton("Flip", CqPickerRail.BTN_FLIP_PREFIX + e.id,
        CqUi.base(), CqUi.dark(), 38f, 22f, 0f);
    flipB.setShortcut(org.lwjgl.input.Keyboard.KEY_P, false);
    flip.addTooltipToPrevious(CqUi.tip(CqStrings.mgmt("mgmt_flip_tip")),
        TooltipMakerAPI.TooltipLocation.BELOW);
    vp.addUIElement(flip).inTL(fr.buttonX(3) - EL_INSET, fr.buttonY(3));

    float rotW = bw + 40f;
    TooltipMakerAPI ccw = vp.createUIElement(rotW + 4f, h, false);
    ButtonAPI ccwB = ccw.addButton("CCW (Z)", CqPickerRail.BTN_ROT_CCW_PREFIX + e.id,
        CqUi.base(), CqUi.dark(), rotW, 22f, 0f);
    ccwB.setShortcut(org.lwjgl.input.Keyboard.KEY_Z, false);
    ccw.addTooltipToPrevious(CqUi.tip(CqStrings.mgmt("mgmt_rotate_ccw_tip")),
        TooltipMakerAPI.TooltipLocation.BELOW);
    vp.addUIElement(ccw).inTL(fr.buttonX(4) - EL_INSET, fr.buttonY(4));

    TooltipMakerAPI cw = vp.createUIElement(rotW + 4f, h, false);
    ButtonAPI cwB = cw.addButton("CW (X)", CqPickerRail.BTN_ROT_CW_PREFIX + e.id,
        CqUi.base(), CqUi.dark(), rotW, 22f, 0f);
    cwB.setShortcut(org.lwjgl.input.Keyboard.KEY_X, false);
    cw.addTooltipToPrevious(CqUi.tip(CqStrings.mgmt("mgmt_rotate_cw_tip")),
        TooltipMakerAPI.TooltipLocation.BELOW);
    vp.addUIElement(cw).inTL(fr.buttonX(5) - EL_INSET, fr.buttonY(5));

    int zi = zIndexOf(e.id);
    int[] band = bandRange(e);
    boolean canFwd = zi > band[0];
    boolean canBack = zi < band[1];

    TooltipMakerAPI fwd = vp.createUIElement(102f, h, false);
    ButtonAPI fwdB = fwd.addButton("Bring forward", CqPickerRail.BTN_Z_FWD_PREFIX + e.id,
        CqUi.base(), CqUi.dark(), 100f, 22f, 0f);
    CqUi.setEnabledSayingWhy(fwdB, canFwd);
    fwdB.setShortcut(org.lwjgl.input.Keyboard.KEY_W, false);
    fwd.addTooltipToPrevious(CqUi.tip(canFwd
        ? CqStrings.mgmt("mgmt_zfwd_tip") : CqStrings.mgmt("mgmt_zfwd_at_front")),
        TooltipMakerAPI.TooltipLocation.BELOW);
    vp.addUIElement(fwd).inTL(fr.buttonX(6) - EL_INSET, fr.buttonY(6));

    TooltipMakerAPI back = vp.createUIElement(84f, h, false);
    ButtonAPI backB = back.addButton("Send back", CqPickerRail.BTN_Z_BACK_PREFIX + e.id,
        CqUi.base(), CqUi.dark(), 82f, 22f, 0f);
    CqUi.setEnabledSayingWhy(backB, canBack);
    backB.setShortcut(org.lwjgl.input.Keyboard.KEY_S, false);
    back.addTooltipToPrevious(CqUi.tip(canBack
        ? CqStrings.mgmt("mgmt_zback_tip") : CqStrings.mgmt("mgmt_zback_at_back")),
        TooltipMakerAPI.TooltipLocation.BELOW);
    vp.addUIElement(back).inTL(fr.buttonX(7) - EL_INSET, fr.buttonY(7));

    float changeW = CqFrame.buttonW(8);
    TooltipMakerAPI change = vp.createUIElement(changeW, h, false);
    ButtonAPI changeB = change.addButton("Change", CqPickerRail.BTN_CHANGE_PREFIX + e.id,
        CqUi.base(), CqUi.dark(), changeW, 22f, 0f);
    changeB.setShortcut(org.lwjgl.input.Keyboard.KEY_H, false);
    change.addTooltipToPrevious(CqUi.tip(CqStrings.mgmt("mgmt_change_tip")),
        TooltipMakerAPI.TooltipLocation.BELOW);
    vp.addUIElement(change).inTL(fr.buttonX(8) - EL_INSET, fr.buttonY(8));

    float resetW = CqFrame.buttonW(9);
    TooltipMakerAPI reset = vp.createUIElement(resetW, h, false);
    ButtonAPI resetB = reset.addButton("Reset", CqPickerRail.BTN_RESET_PREFIX + e.id,
        CqUi.base(), CqUi.dark(), resetW, 22f, 0f);
    resetB.setShortcut(org.lwjgl.input.Keyboard.KEY_T, false);
    reset.addTooltipToPrevious(CqUi.tip(CqStrings.mgmt("mgmt_reset_tip")),
        TooltipMakerAPI.TooltipLocation.BELOW);
    vp.addUIElement(reset).inTL(fr.buttonX(9) - EL_INSET, fr.buttonY(9));
  }

  public static int zIndexOf(String slotId) {
    List<CqSlots.Entry> list = CqSlots.list();
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).id.equals(slotId)) {
        return i;
      }
    }
    return -1;
  }

  private static int[] bandRange(CqSlots.Entry target) {
    List<CqSlots.Entry> list = CqSlots.list();
    int first = -1;
    int last = -1;
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).decorative == target.decorative) {
        if (first < 0) {
          first = i;
        }
        last = i;
      }
    }
    return new int[] { first, last };
  }

  public static boolean stepZ(String slotId, int dir) {
    int zi = zIndexOf(slotId);
    if (zi < 0) {
      return false;
    }
    CqSlots.reorder(slotId, zi + dir);
    return true;
  }

  private static final float[] FALLBACK_STICKER_DIMS = { 200f, 200f };

  public static float[] stickerDims(String itemId) {
    CqCatalog.Furnishing f = CqCatalog.get(itemId);
    float[] wh = f == null ? null : CqUi.stickerSize(f.getSlotId());
    return wh == null ? FALLBACK_STICKER_DIMS : wh;
  }

  public static float rotateStepDegrees() {
    return setting("cq_rotate_step_degrees", 30f);
  }

  static float rotDegrees(int rotSteps) {
    return rotSteps * rotateStepDegrees();
  }

  public static float[] stickerAabb(String itemId, float scale, int rotSteps) {
    float[] dims = stickerDims(itemId);
    return CqSlotMath.rotatedAabb(dims[0] * scale, dims[1] * scale, rotDegrees(rotSteps));
  }

  public static float[] stickerAabb(CqSlots.Entry e) {
    return stickerAabb(e.occupant, e.scale, e.rotSteps);
  }

  private static void styleStrip(CustomPanelAPI vp, CqFrame fr, String activeStyle) {
    TooltipMakerAPI label = vp.createUIElement(52f, CqFrame.EFFECT_DY, false);
    label.addPara("Style", CqUi.gray(), 0f);
    vp.addUIElement(label).inTL(-EL_INSET, fr.stripY + 14f);

    int i = 0;
    for (String styleId : CqData.STYLE_IDS) {
      String name = CqUi.styleName(styleId);
      boolean active = styleId.equals(activeStyle);
      float w = CqFrame.STYLE_W;
      float h = CqFrame.STYLE_H;
      CustomPanelAPI pic = vp.createCustomPanel(w, h, new StylePicture(styleSprite(styleId), active));
      TooltipMakerAPI over = pic.createUIElement(w, h, false);
      over.addButton("", BTN_STYLE_PREFIX + styleId, CqUi.CLEAR, CqUi.CLEAR, Alignment.MID,
          (CutStyle) null, w, h, 0f);
      over.addTooltipToPrevious(CqUi.tip(String.format(CqStrings.mgmt(active
          ? "mgmt_style_current_named_tip" : "mgmt_style_refit_tip"), name)),
          TooltipMakerAPI.TooltipLocation.ABOVE);
      pic.addUIElement(over).inTL(-EL_INSET, 0f);

      TooltipMakerAPI holder = vp.createUIElement(w, h, false);
      holder.addCustom(pic, 0f);
      vp.addUIElement(holder).inTL(CqFrame.styleX(i) - EL_INSET, fr.stripY + 2f);
      i++;
    }
  }

  private static final String STYLE_THUMB_PREFIX = "cq_bg_thumb_";
  private static final java.util.Map<String, SpriteAPI> STYLE_SPRITES =
      new java.util.HashMap<String, SpriteAPI>();

  private static SpriteAPI styleSprite(String styleId) {
    if (STYLE_SPRITES.containsKey(styleId)) {
      return STYLE_SPRITES.get(styleId);
    }
    String suffix = styleId.startsWith("cq_style_")
        ? styleId.substring("cq_style_".length()) : styleId;
    SpriteAPI s = CqUi.spriteObj("cq", STYLE_THUMB_PREFIX + suffix);
    if (s == null) {
      s = CqUi.backgroundSprite(styleId);
    }
    STYLE_SPRITES.put(styleId, s);
    return s;
  }

  private static final class StylePicture extends BaseCustomUIPanelPlugin {

    private final SpriteAPI sprite;
    private final boolean current;
    private final Color border;
    private PositionAPI pos;

    StylePicture(SpriteAPI sprite, boolean current) {
      this.sprite = sprite;
      this.current = current;
      this.border = current ? CqUi.widgetBright() : CqUi.dark();
    }

    @Override
    public void positionChanged(PositionAPI position) {
      pos = position;
    }

    @Override
    public void buttonPressed(Object buttonId) {
      CqConsolePanel.routeButton(buttonId);
    }

    @Override
    public void render(float alphaMult) {
      if (pos == null || alphaMult <= 0.01f) {
        return;
      }
      float x = pos.getX();
      float y = pos.getY();
      float w = pos.getWidth();
      float h = pos.getHeight();
      float strength = (current ? 1f : STYLE_DIM) * alphaMult;
      fillQuad(x, y, w, h, Color.BLACK, alphaMult);
      if (sprite != null) {
        sprite.setNormalBlend();
        sprite.setColor(Color.WHITE);
        sprite.setAlphaMult(strength);
        sprite.setAngle(0f);
        sprite.setSize(w, h);
        sprite.render(x, y);
      } else {
        fillQuad(x, y, w, h, DECK_PLATE, strength);
      }
      lineRect(x, y, w, h, border, alphaMult);
      if (current) {
        lineRect(x + 1f, y + 1f, w - 2f, h - 2f, border, alphaMult);
      }
    }
  }

  private static void addSticker(CustomPanelAPI vp, CanvasPlugin canvas, CqSlots.Entry e,
      String selectedSlot, float imgX, float imgY, float scale, boolean pulse) {
    float[] dims = stickerDims(e.occupant);
    float[] aabb = stickerAabb(e);
    float fw = aabb[0];
    float fh = aabb[1];
    float w = fw * scale;
    float h = fh * scale;
    float x = imgX + e.x.floatValue() * scale;
    float y = imgY + e.y.floatValue() * scale;

    StickerPlugin plugin = new StickerPlugin(canvas, e.occupant, e.id.equals(selectedSlot), pulse,
        rotDegrees(e.rotSteps), e.flipX, dims[0] * e.scale * scale, dims[1] * e.scale * scale);
    CustomPanelAPI sp = vp.createCustomPanel(w, h, plugin);
    float[] vis = CqSlotMath.visibleSubRect(e.x.floatValue(), e.y.floatValue(), fw, fh,
        FRAME_W, FRAME_H);
    float visW = vis[2] * scale;
    float visH = vis[3] * scale;
    TooltipMakerAPI overlay = sp.createUIElement(visW, visH, false);
    ButtonAPI hotspot = overlay.addButton("", BTN_STICKER_PREFIX + e.id, CqUi.CLEAR, CqUi.CLEAR,
        Alignment.MID, (CutStyle) null, visW, visH, 0f);
    if (isCarrying()) {
      hotspot.setEnabled(false);
    }
    if (e.id.equals(followSlot)) {
      plugin.setDragHidden(true);
    }
    overlay.addTooltipToPrevious(CqUi.furnishingTip(e.occupant,
        CqStrings.mgmt("mgmt_sticker_tip"), e.decorative, e.id),
        TooltipMakerAPI.TooltipLocation.BELOW);
    sp.addUIElement(overlay).inTL((vis[0] - e.x.floatValue()) * scale,
        (vis[1] - e.y.floatValue()) * scale);

    TooltipMakerAPI holder = vp.createUIElement(w, h, false);
    holder.addCustom(sp, 0f);
    vp.addUIElement(holder).inTL(x, y);

    canvas.registerSticker(e.id, plugin, sp, holder, e.x.floatValue(), e.y.floatValue(), fw, fh,
        hotspot);
  }

  public static float scaleStep() {
    return setting("cq_sticker_scale_step", 0.10f);
  }

  public static float scaleMin() {
    return setting("cq_sticker_scale_min", 0.5f);
  }

  public static float scaleMax() {
    return setting("cq_sticker_scale_max", 2.0f);
  }

  private static float setting(String key, float fallback) {
    return cq.campaign.CqSettings.getFloat(key, fallback);
  }

  public static boolean stepScale(String slotId, int dir) {
    CqSlots.Entry e = CqSlots.byId(slotId);
    if (e == null || e.occupant == null || !e.placed()) {
      return false;
    }
    float lo = scaleMin();
    float target = clamp(e.scale + dir * scaleStep(), lo, Math.max(lo, scaleMax()));
    if (Math.abs(target - e.scale) < 0.001f) {
      return false;
    }
    float[] aabb = stickerAabb(e);
    float[] naabb = stickerAabb(e.occupant, target, e.rotSteps);
    float cx = e.x.floatValue() + aabb[0] / 2f;
    float cy = e.y.floatValue() + aabb[1] / 2f;
    float nx = CqSlotMath.clampCenterInside(cx - naabb[0] / 2f, naabb[0], FRAME_W);
    float ny = CqSlotMath.clampCenterInside(cy - naabb[1] / 2f, naabb[1], FRAME_H);
    CqSlots.setScale(slotId, target);
    CqSlots.setStickerPos(slotId, nx, ny);
    return true;
  }

  public static boolean stepRotate(String slotId, int dir) {
    CqSlots.Entry e = CqSlots.byId(slotId);
    if (e == null || e.occupant == null || !e.placed()) {
      return false;
    }
    float[] aabb = stickerAabb(e);
    float[] naabb = stickerAabb(e.occupant, e.scale, e.rotSteps + dir);
    float cx = e.x.floatValue() + aabb[0] / 2f;
    float cy = e.y.floatValue() + aabb[1] / 2f;
    CqSlots.setRotSteps(slotId, e.rotSteps + dir);
    CqSlots.setStickerPos(slotId,
        CqSlotMath.clampCenterInside(cx - naabb[0] / 2f, naabb[0], FRAME_W),
        CqSlotMath.clampCenterInside(cy - naabb[1] / 2f, naabb[1], FRAME_H));
    return true;
  }

  public static boolean toggleFlip(String slotId) {
    CqSlots.Entry e = CqSlots.byId(slotId);
    if (e == null || e.occupant == null || !e.placed()) {
      return false;
    }
    CqSlots.setFlipX(slotId, !e.flipX);
    return true;
  }

  public static void clampIntoCanvas(String slotId) {
    CqSlots.Entry e = CqSlots.byId(slotId);
    if (e == null || e.occupant == null || !e.placed()) {
      return;
    }
    float[] aabb = stickerAabb(e);
    float nx = CqSlotMath.clampCenterInside(e.x.floatValue(), aabb[0], FRAME_W);
    float ny = CqSlotMath.clampCenterInside(e.y.floatValue(), aabb[1], FRAME_H);
    if (Math.abs(nx - e.x.floatValue()) > 0.01f || Math.abs(ny - e.y.floatValue()) > 0.01f) {
      CqSlots.setStickerPos(slotId, nx, ny);
    }
  }

  private static final class Sticker {
    final String slotId;
    final StickerPlugin plugin;
    final CustomPanelAPI panel;
    final UIComponentAPI holder;
    final ButtonAPI hotspot;
    final float fx;
    final float fy;
    final float fw;
    final float fh;
    final float[] vis;

    Sticker(String slotId, StickerPlugin plugin, CustomPanelAPI panel, UIComponentAPI holder,
        float fx, float fy, float fw, float fh, ButtonAPI hotspot) {
      this.slotId = slotId;
      this.plugin = plugin;
      this.panel = panel;
      this.holder = holder;
      this.fx = fx;
      this.fy = fy;
      this.fw = fw;
      this.fh = fh;
      this.hotspot = hotspot;
      this.vis = CqSlotMath.visibleSubRect(fx, fy, fw, fh, FRAME_W, FRAME_H);
    }
  }

  private static final class CanvasPlugin extends BaseCustomUIPanelPlugin {

    private final Color frame = CqUi.dark();
    private final Color ghostBright = CqUi.widgetBright();
    private final String selectedSlot;
    private PositionAPI pos;
    private float imgX;
    private float imgY;
    private float imgW;
    private float imgH;
    private float scale = 1f;
    private SpriteAPI bg;
    private boolean ambientScissorCached;
    private boolean ambientScissorEnabled;
    private int ambientX0, ambientY0, ambientX1, ambientY1;
    private final List<Sticker> stickers = new ArrayList<Sticker>();

    private boolean anchorsTrued;

    private LabelAPI markLabel;
    private String markSlot;
    private CqRailMarks.Mark markShown;
    private float sinceMarkCheck;

    CanvasPlugin(String selectedSlot) {
      this.selectedSlot = selectedSlot;
    }

    void watchMark(LabelAPI label, String slotId, CqRailMarks.Mark shown) {
      markLabel = label;
      markSlot = slotId;
      markShown = shown;
    }

    private void followMark(float amount) {
      sinceMarkCheck += amount;
      if (sinceMarkCheck < CqRailMarks.CHECK_SECONDS) {
        return;
      }
      sinceMarkCheck = 0f;
      if (!CqData.exists()) {
        return;
      }
      CqRailMarks.Mark now = CqUi.markOf(CqData.getInstance(), CqSlots.byId(markSlot));
      if (now != markShown && now != CqRailMarks.Mark.NONE) {
        markShown = now;
        markLabel.setText(CqUi.markText(now));
        markLabel.setColor(CqUi.markColor(now));
      }
    }

    void setImage(float imgX, float imgY, float imgW, float imgH, float scale, SpriteAPI bg) {
      this.imgX = imgX;
      this.imgY = imgY;
      this.imgW = imgW;
      this.imgH = imgH;
      this.scale = scale;
      this.bg = bg;
    }

    void registerSticker(String slotId, StickerPlugin plugin, CustomPanelAPI panel,
        UIComponentAPI holder, float fx, float fy, float fw, float fh, ButtonAPI hotspot) {
      stickers.add(0, new Sticker(slotId, plugin, panel, holder, fx, fy, fw, fh, hotspot));
    }

    private void trueUpAnchors() {
      if (stickers.isEmpty()) {
        anchorsTrued = true;
        return;
      }
      for (Sticker s : stickers) {
        PositionAPI pp = s.panel.getPosition();
        if (pp == null || pp.getWidth() <= 0f) {
          return;
        }
      }
      for (Sticker s : stickers) {
        PositionAPI pp = s.panel.getPosition();
        float dxs = screenX(s.fx) - pp.getX();
        float dys = screenY(s.fy, s.fh) - pp.getY();
        if (Math.abs(dxs) > 0.01f || Math.abs(dys) > 0.01f) {
          s.holder.getPosition().inTL(imgX + s.fx * scale + dxs, imgY + s.fy * scale - dys);
        }
      }
      anchorsTrued = true;
    }

    @Override
    public void positionChanged(PositionAPI position) {
      pos = position;
    }

    @Override
    public void buttonPressed(Object buttonId) {
      CqConsolePanel.routeButton(buttonId);
    }

    private float screenX(float fx) {
      return pos.getX() + imgX + fx * scale;
    }

    private float screenY(float fy, float fh) {
      return pos.getY() + pos.getHeight() - imgY - (fy + fh) * scale;
    }

    private float mouseFrameX() {
      return (com.fs.starfarer.api.Global.getSettings().getMouseX() - pos.getX() - imgX) / scale;
    }

    private float mouseFrameY() {
      return (pos.getY() + pos.getHeight() - imgY
          - com.fs.starfarer.api.Global.getSettings().getMouseY()) / scale;
    }

    private boolean insideImage(float sx, float sy) {
      float x0 = pos.getX() + imgX;
      float y0 = pos.getY() + pos.getHeight() - imgY - imgH;
      return sx >= x0 && sx < x0 + imgW && sy >= y0 && sy < y0 + imgH;
    }

    private Sticker bySlot(String slotId) {
      for (Sticker s : stickers) {
        if (s.slotId.equals(slotId)) {
          return s;
        }
      }
      return null;
    }

    private Sticker hit(float sx, float sy) {
      for (Sticker s : stickers) {
        float x = screenX(s.vis[0]);
        float y = screenY(s.vis[1], s.vis[3]);
        if (sx >= x && sx < x + s.vis[2] * scale && sy >= y && sy < y + s.vis[3] * scale) {
          return s;
        }
      }
      return null;
    }

    @Override
    public void advance(float amount) {
      if (pos == null) {
        return;
      }
      if (!anchorsTrued) {
        trueUpAnchors();
      }
      if (markLabel != null) {
        followMark(amount);
      }
      armPendingGrab();
      if (followSlot != null) {
        updateCarry();
      }
    }

    private void armPendingGrab() {
      if (pendingGrab == null || bySlot(pendingGrab) == null) {
        return;
      }
      Sticker s = bySlot(pendingGrab);
      CqSlots.Entry e = CqSlots.byId(pendingGrab);
      pendingGrab = null;
      if (e == null || e.occupant == null || !e.placed()) {
        return;
      }
      followSlot = s.slotId;
      followItem = e.occupant;
      float mx = mouseFrameX();
      float my = mouseFrameY();
      boolean cursorInside = mx >= s.fx && mx < s.fx + s.fw && my >= s.fy && my < s.fy + s.fh;
      followDX = cursorInside ? mx - s.fx : s.fw / 2f;
      followDY = cursorInside ? my - s.fy : s.fh / 2f;
      followFx = s.fx;
      followFy = s.fy;
      setCarryChrome(true);
    }

    private void updateCarry() {
      Sticker s = bySlot(followSlot);
      if (s == null) {
        return;
      }
      CqSlots.Entry e = CqSlots.byId(followSlot);
      if (e == null || e.occupant == null || !e.placed() || !e.occupant.equals(followItem)) {
        setCarryChrome(false);
        clearCarry();
        return;
      }
      followFx = CqSlotMath.clampCenterInside(mouseFrameX() - followDX, s.fw, FRAME_W);
      followFy = CqSlotMath.clampCenterInside(mouseFrameY() - followDY, s.fh, FRAME_H);
    }

    private void setCarryChrome(boolean carrying) {
      for (Sticker s : stickers) {
        if (s.hotspot != null) {
          s.hotspot.setEnabled(!carrying);
        }
        if (carrying) {
          if (s.slotId.equals(followSlot)) {
            s.plugin.setDragHidden(true);
          }
        } else {
          s.plugin.setDragHidden(false);
        }
      }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
      if (pos == null) {
        return;
      }
      for (InputEventAPI e : events) {
        if (e.isConsumed()) {
          continue;
        }
        if (e.isKeyDownEvent() && selectedSlot != null
            && (e.getEventValue() == org.lwjgl.input.Keyboard.KEY_MINUS
                || e.getEventValue() == org.lwjgl.input.Keyboard.KEY_EQUALS)) {
          CqSlots.Entry sel = CqSlots.byId(selectedSlot);
          if (sel != null && sel.occupant != null && sel.placed()) {
            boolean shrink = e.getEventValue() == org.lwjgl.input.Keyboard.KEY_MINUS;
            cq.intel.CqIntel.requestClusterVerb((shrink
                ? CqPickerRail.BTN_SCALE_DOWN_PREFIX
                : CqPickerRail.BTN_SCALE_UP_PREFIX) + selectedSlot);
            e.consume();
          }
          continue;
        }
        if (followSlot == null) {
          if (selectedSlot != null && e.isMouseEvent() && e.isLMBDownEvent()
              && insideImage(e.getX(), e.getY()) && hit(e.getX(), e.getY()) == null) {
            cq.intel.CqIntel.requestDeselect();
            e.consume();
          }
          continue;
        }
        if (e.isMouseEvent()) {
          if (e.isLMBDownEvent() && insideImage(e.getX(), e.getY())) {
            Sticker s = bySlot(followSlot);
            if (s != null) {
              float fx = (e.getX() - pos.getX() - imgX) / scale - followDX;
              float fy = (pos.getY() + pos.getHeight() - imgY - e.getY()) / scale - followDY;
              followFx = CqSlotMath.clampCenterInside(fx, s.fw, FRAME_W);
              followFy = CqSlotMath.clampCenterInside(fy, s.fh, FRAME_H);
            }
            setCarryChrome(false);
            dropCarry();
            e.consume();
          } else if (e.isRMBDownEvent()) {
            setCarryChrome(false);
            cancelCarry();
            e.consume();
          }
        } else if (e.isKeyDownEvent()
            && e.getEventValue() == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
          setCarryChrome(false);
          cancelCarry();
          e.consume();
        }
      }
    }

    @Override
    public void renderBelow(float alphaMult) {
      if (pos == null || alphaMult <= 0.01f) {
        return;
      }
      if (imgW <= 0f) {
        return;
      }
      float x = pos.getX() + imgX;
      float y = pos.getY() + pos.getHeight() - imgY - imgH;
      if (bg != null) {
        bg.setNormalBlend();
        bg.setColor(Color.WHITE);
        bg.setAlphaMult(alphaMult);
        bg.setSize(imgW, imgH);
        bg.render(x, y);
      } else {
        fillQuad(x, y, imgW, imgH, DECK_PLATE, alphaMult);
      }
      lineRect(x, y, imgW, imgH, frame, 0.8f * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
      if (followSlot == null || alphaMult <= 0.01f) {
        return;
      }
      Sticker s = bySlot(followSlot);
      if (s == null) {
        return;
      }
      if (!pushImageScissor(this)) {
        return;
      }
      float w = s.fw * scale;
      float h = s.fh * scale;
      float x = screenX(followFx);
      float y = screenY(followFy, s.fh);
      bracketsGlow(x, y, w, h, ghostBright, 0.55f * alphaMult);
      if (s.plugin.sprite != null) {
        drawTransformed(s.plugin.sprite, x + w / 2f, y + h / 2f, s.plugin.spriteW,
            s.plugin.spriteH, s.plugin.angleDeg, s.plugin.flipX, alphaMult);
      } else {
        fillQuad(x, y, w, h, s.plugin.placeholder, 0.4f * alphaMult);
        lineRect(x, y, w, h, s.plugin.placeholder, 0.9f * alphaMult);
      }
      brackets(x, y, w, h, ghostBright, 0.95f * alphaMult);
      popImageScissor();
    }
  }

  private static final Color DECK_PLATE = new Color(12, 16, 20);

  private static float clamp(float v, float lo, float hi) {
    return v < lo ? lo : (v > hi ? hi : v);
  }

  private static final class StickerPlugin extends BaseCustomUIPanelPlugin {

    final String itemId;
    final SpriteAPI sprite;
    final Color placeholder;
    final float angleDeg;
    final boolean flipX;
    final float spriteW;
    final float spriteH;
    private final CanvasPlugin canvas;
    private final boolean selected;
    private final Color bracketBright = CqUi.widgetBright();
    private PositionAPI pos;
    private boolean hovered;
    private boolean dragHidden;
    private float pulse;

    StickerPlugin(CanvasPlugin canvas, String itemId, boolean selected, boolean pulse,
        float angleDeg, boolean flipX, float spriteW, float spriteH) {
      this.canvas = canvas;
      this.itemId = itemId;
      this.sprite = CqUi.furnishingSprite(itemId);
      CqCatalog.Furnishing f = CqCatalog.get(itemId);
      this.placeholder = f == null ? CqUi.gray() : f.getRarity().getColor();
      this.selected = selected;
      this.pulse = pulse ? 1f : 0f;
      this.angleDeg = angleDeg;
      this.flipX = flipX;
      this.spriteW = spriteW;
      this.spriteH = spriteH;
    }

    void setDragHidden(boolean hidden) {
      dragHidden = hidden;
    }

    @Override
    public void positionChanged(PositionAPI position) {
      pos = position;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
      if (pos == null) {
        return;
      }
      for (InputEventAPI e : events) {
        if (e.isMouseMoveEvent()) {
          hovered = pos.containsEvent(e);
        }
      }
    }

    @Override
    public void buttonPressed(Object buttonId) {
      CqConsolePanel.routeButton(buttonId);
    }

    @Override
    public void advance(float amount) {
      if (pulse > 0f) {
        pulse = Math.max(0f, pulse - amount * 1.25f);
      }
    }

    @Override
    public void render(float alphaMult) {
      if (pos == null || alphaMult <= 0.01f || dragHidden) {
        return;
      }
      if (!pushImageScissor(canvas)) {
        return;
      }
      boolean lit = hovered || selected;
      float x = pos.getX();
      float y = pos.getY();
      float w = pos.getWidth();
      float h = pos.getHeight();
      float glow = lit ? 0.55f : 0f;
      if (pulse > 0f) {
        glow = Math.max(glow, 0.8f * pulse);
      }
      if (glow > 0f) {
        bracketsGlow(x, y, w, h, bracketBright, glow * alphaMult);
      }
      if (sprite != null) {
        drawTransformed(sprite, x + w / 2f, y + h / 2f, spriteW, spriteH, angleDeg, flipX,
            alphaMult);
      } else {
        fillQuad(x, y, w, h, placeholder, (lit ? 0.4f : 0.24f) * alphaMult);
        lineRect(x, y, w, h, placeholder, (lit ? 0.9f : 0.55f) * alphaMult);
      }
      if (lit) {
        brackets(x, y, w, h, bracketBright, 0.95f * alphaMult);
      }
      popImageScissor();
    }
  }

  static void drawTransformed(SpriteAPI sprite, float cx, float cy, float w, float h,
      float angleDeg, boolean flipX, float alphaMult) {
    sprite.setNormalBlend();
    sprite.setColor(Color.WHITE);
    sprite.setAlphaMult(alphaMult);
    sprite.setAngle(angleDeg);
    sprite.setSize(flipX ? -w : w, h);
    sprite.renderAtCenter(cx, cy);
    sprite.setAngle(0f);
    sprite.setSize(w, h);
  }

  private static final java.nio.IntBuffer SCISSOR_BOX = org.lwjgl.BufferUtils.createIntBuffer(16);

  private static int scissorPushCount;
  private static int ambientScissorReadCount;

  public static int scissorPushesForTest() {
    return scissorPushCount;
  }

  public static int ambientScissorReadsForTest() {
    return ambientScissorReadCount;
  }

  public static void resetScissorCountsForTest() {
    scissorPushCount = 0;
    ambientScissorReadCount = 0;
  }

  private static boolean pushImageScissor(CanvasPlugin c) {
    if (c == null || c.pos == null || c.imgW <= 0f) {
      return false;
    }
    float mult = com.fs.starfarer.api.Global.getSettings().getScreenScaleMult();
    float vx = c.pos.getX() + c.imgX;
    float vy = c.pos.getY() + c.pos.getHeight() - c.imgY - c.imgH;
    int x0 = (int) Math.floor(vx * mult);
    int y0 = (int) Math.floor(vy * mult);
    int x1 = (int) Math.ceil((vx + c.imgW) * mult);
    int y1 = (int) Math.ceil((vy + c.imgH) * mult);
    GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
    scissorPushCount++;
    if (!c.ambientScissorCached) {
      ambientScissorReadCount++;
      c.ambientScissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
      if (c.ambientScissorEnabled) {
        SCISSOR_BOX.clear();
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, SCISSOR_BOX);
        c.ambientX0 = SCISSOR_BOX.get(0);
        c.ambientY0 = SCISSOR_BOX.get(1);
        c.ambientX1 = SCISSOR_BOX.get(0) + SCISSOR_BOX.get(2);
        c.ambientY1 = SCISSOR_BOX.get(1) + SCISSOR_BOX.get(3);
      }
      c.ambientScissorCached = true;
    }
    if (c.ambientScissorEnabled) {
      x0 = Math.max(x0, c.ambientX0);
      y0 = Math.max(y0, c.ambientY0);
      x1 = Math.min(x1, c.ambientX1);
      y1 = Math.min(y1, c.ambientY1);
    } else {
      GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }
    GL11.glScissor(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
    return true;
  }

  private static void popImageScissor() {
    GL11.glPopAttrib();
  }

  private static void glSetup(Color c, float alpha, boolean additive) {
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, additive ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha);
  }

  private static void fillQuad(float x, float y, float w, float h, Color c, float alpha) {
    glSetup(c, alpha, false);
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(x, y + h);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(x + w, y);
    GL11.glEnd();
    GL11.glPopAttrib();
  }

  private static void lineRect(float x, float y, float w, float h, Color c, float alpha) {
    glSetup(c, alpha, false);
    GL11.glBegin(GL11.GL_LINE_LOOP);
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(x, y + h);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(x + w, y);
    GL11.glEnd();
    GL11.glPopAttrib();
  }

  private static void brackets(float x, float y, float w, float h, Color c, float alpha) {
    float len = Math.max(6f, Math.min(14f, Math.min(w, h) / 5f));
    glSetup(c, alpha, false);
    GL11.glBegin(GL11.GL_LINES);
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(x + len, y);
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(x, y + len);
    GL11.glVertex2f(x + w, y);
    GL11.glVertex2f(x + w - len, y);
    GL11.glVertex2f(x + w, y);
    GL11.glVertex2f(x + w, y + len);
    GL11.glVertex2f(x, y + h);
    GL11.glVertex2f(x + len, y + h);
    GL11.glVertex2f(x, y + h);
    GL11.glVertex2f(x, y + h - len);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(x + w - len, y + h);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(x + w, y + h - len);
    GL11.glEnd();
    GL11.glPopAttrib();
  }

  static void bracketsGlow(float x, float y, float w, float h, Color c, float alpha) {
    float len = Math.max(6f, Math.min(14f, Math.min(w, h) / 5f));
    float r = 2.5f * len;
    glowQuad(x - r, y - r, 2f * r, 2f * r, c, alpha);
    glowQuad(x + w - r, y - r, 2f * r, 2f * r, c, alpha);
    glowQuad(x - r, y + h - r, 2f * r, 2f * r, c, alpha);
    glowQuad(x + w - r, y + h - r, 2f * r, 2f * r, c, alpha);
  }

  static void glowQuad(float x, float y, float w, float h, Color c, float alpha) {
    float cx = x + w / 2f;
    float cy = y + h / 2f;
    glSetup(c, alpha, true);
    GL11.glBegin(GL11.GL_TRIANGLE_FAN);
    GL11.glVertex2f(cx, cy);
    GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 0f);
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(cx, y);
    GL11.glVertex2f(x + w, y);
    GL11.glVertex2f(x + w, cy);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(cx, y + h);
    GL11.glVertex2f(x, y + h);
    GL11.glVertex2f(x, cy);
    GL11.glVertex2f(x, y);
    GL11.glEnd();
    GL11.glPopAttrib();
  }
}
