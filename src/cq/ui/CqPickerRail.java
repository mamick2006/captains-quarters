package cq.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import cq.campaign.CqData;
import cq.campaign.CqStrings;
import cq.effects.CqOccasions;
import cq.effects.pure.CqRailMarks;
import cq.effects.pure.CqWhen;
import cq.items.CqCatalog;

public final class CqPickerRail {

  public static final String BTN_BACK = "cq_btn_back";
  public static final String BTN_RESET_PREFIX = "cq_btn_reset*";
  public static final String BTN_PICK_PREFIX = "cq_btn_pick*";
  public static final String BTN_CHANGE_PREFIX = "cq_btn_change*";
  public static final String BTN_MOVE_PREFIX = "cq_btn_move*";
  public static final String BTN_Z_FWD_PREFIX = "cq_btn_zfwd*";
  public static final String BTN_Z_BACK_PREFIX = "cq_btn_zback*";
  public static final String BTN_SCALE_UP_PREFIX = "cq_btn_scaleup*";
  public static final String BTN_SCALE_DOWN_PREFIX = "cq_btn_scaledown*";
  public static final String BTN_ADD_SLOT = "cq_btn_add_slot";
  public static final String BTN_ADD_DECORATIVE = "cq_btn_add_decorative";
  public static final String BTN_FLIP_PREFIX = "cq_btn_flip*";
  public static final String BTN_ROT_CCW_PREFIX = "cq_btn_rotccw*";
  public static final String BTN_ROT_CW_PREFIX = "cq_btn_rotcw*";
  public static final String BTN_COLLECTION = "cq_btn_collection";
  public static final String BTN_LAYOUT_SAVE_PREFIX = "cq_btn_layout_save*";
  public static final String BTN_LAYOUT_LOAD_PREFIX = "cq_btn_layout_load*";
  public static final String BTN_ADD_CONFIRM = "cq_btn_add_confirm";
  public static final String BTN_ADD_CANCEL = "cq_btn_add_cancel";

  public static final String ASK_SLOT = "slot";
  public static final String ASK_DECO = "deco";

  private static final int MAX_TIP_ROWS = 12;

  private static final float PAD = 6f;
  private static final float ROW_PAD = 4f;
  private static final float LINE_H = 15f;
  private static final float CHAR_W = 7.5f;
  private static final float SCROLLER_W = 14f;

  private static final float TILE_W = 180f;
  private static final float TILE_H = 70f;
  private static final float TILE_GAP = 6f;
  private static final float PIC = 64f;
  private static final float PIC_INSET = 3f;
  private static final float NAME_X = 73f;
  private static final float NAME_W = 105f;
  private static final float DECO_CELL = 40f;
  private static final float DECO_GAP = 4f;
  private static final int DECO_PER_ROW = 8;
  private static final float HEADING_H = 18f;
  private static final float FOOT_PAD = 8f;
  private static final float FOOT_H = 84f;
  private static final float ASK_BTN_Y = 62f;
  private static final float ASK_LINE_Y = ASK_BTN_Y - 22f;
  private static final float VIEW_SLACK = 0f;
  private static final float PARA_SLACK = 10f;
  private static final float IMG_INSET = 10f;
  private static final float EL_INSET = 5f;

  public static final float MIN_W = 380f;

  public static final float RAIL_W = 380f;

  private static final float LAYOUT_BTN_W = CqFrame.LAYOUT_BTN_W;

  public static final float LAYOUT_STRIP_W = CqFrame.LAYOUTS_W;
  public static final float LAYOUT_STRIP_H = 44f;

  private static TooltipMakerAPI liveBody;
  private static float savedScroll;
  private static String savedKey;

  private CqPickerRail() {
  }

  public static void saveScroll() {
    if (liveBody != null) {
      ScrollPanelAPI s = liveBody.getExternalScroller();
      savedScroll = s == null ? 0f : s.getYOffset();
    }
  }

  public static void dropLiveBody() {
    liveBody = null;
  }

  public static void resetStaticsForLoad() {
    liveBody = null;
    savedKey = null;
    savedScroll = 0f;
  }

  public static void add(CustomPanelAPI host, float x, float w, float h, CqData data,
      String selectedSlot, boolean pickerOpen, boolean viewer) {
    add(host, x, w, h, data, selectedSlot, pickerOpen, viewer, null);
  }

  public static void add(CustomPanelAPI host, float x, float w, float h, CqData data,
      String selectedSlot, boolean pickerOpen, boolean viewer, String ask) {
    RailPlugin plugin = new RailPlugin();
    CustomPanelAPI rail = host.createCustomPanel(w, h, plugin);
    float rowW = w - SCROLLER_W;
    boolean picking = pickerOpen && selectedSlot != null;
    boolean onShelf = !picking && !viewer;
    List<CqSlots.Entry> slots = onShelf ? CqSlots.list() : null;
    float viewH = onShelf
        ? Math.max(DECO_CELL,
            Math.min(shelfHeight(slots, rowW) + VIEW_SLACK, h - FOOT_H - FOOT_PAD)) : h;
    TooltipMakerAPI body = rail.createUIElement(w + EL_INSET, viewH, true);
    String key;
    if (picking) {
      key = "picker*" + selectedSlot;
      picker(rail, body, rowW, data, selectedSlot, false);
    } else if (viewer) {
      key = "viewer";
      picker(rail, body, rowW, data, null, true);
    } else {
      key = "slots";
      shelf(rail, plugin, body, rowW, data, slots, selectedSlot);
    }
    rail.addUIElement(body).inTL(-EL_INSET, 0f);
    if (onShelf) {
      foot(rail, data, Math.min(rowW, 2f * TILE_W + TILE_GAP), viewH + FOOT_PAD, ask);
    }

    if (key.equals(savedKey) && savedScroll > 0f) {
      ScrollPanelAPI s = body.getExternalScroller();
      if (s != null) {
        s.setYOffset(Math.min(savedScroll, Math.max(0f, body.getHeightSoFar() - viewH)));
      }
    } else {
      savedScroll = 0f;
    }
    savedKey = key;
    liveBody = body;

    TooltipMakerAPI holder = host.createUIElement(w, h, false);
    holder.addCustom(rail, 0f);
    host.addUIElement(holder).inTL(x, 0f);
  }

  private static int tileColumns(float rowW) {
    return rowW >= 2f * TILE_W + TILE_GAP ? 2 : 1;
  }

  private static int decoColumns(float rowW) {
    int fit = (int) Math.floor((rowW + DECO_GAP) / (DECO_CELL + DECO_GAP));
    return Math.max(1, Math.min(DECO_PER_ROW, fit));
  }

  private static float shelfHeight(List<CqSlots.Entry> slots, float rowW) {
    int regular = 0;
    int decorative = 0;
    for (CqSlots.Entry e : slots) {
      if (e.decorative) {
        decorative++;
      } else {
        regular++;
      }
    }
    if (regular + decorative == 0) {
      return 2f * LINE_H + ROW_PAD;
    }
    int cols = tileColumns(rowW);
    int tileRows = (regular + cols - 1) / cols;
    float height = tileRows * TILE_H + Math.max(0, tileRows - 1) * ROW_PAD;
    if (decorative > 0) {
      int perRow = decoColumns(rowW);
      int decoRows = (decorative + perRow - 1) / perRow;
      height += (tileRows > 0 ? 10f : 0f) + HEADING_H + ROW_PAD
          + decoRows * DECO_CELL + (decoRows - 1) * DECO_GAP;
    }
    return height;
  }

  private static void shelf(CustomPanelAPI rail, RailPlugin plugin, TooltipMakerAPI body,
      float rowW, CqData data, List<CqSlots.Entry> slots, String selectedSlot) {
    if (slots.isEmpty()) {
      body.addPara(CqStrings.mgmt("mgmt_rail_no_slots"), CqUi.gray(), 0f);
      return;
    }
    List<CqSlots.Entry> regular = new ArrayList<CqSlots.Entry>();
    List<CqSlots.Entry> decorative = new ArrayList<CqSlots.Entry>();
    for (CqSlots.Entry e : slots) {
      (e.decorative ? decorative : regular).add(e);
    }

    int cols = tileColumns(rowW);
    for (int i = 0; i < regular.size(); i += cols) {
      tileRow(rail, plugin, body, rowW, data,
          regular.subList(i, Math.min(regular.size(), i + cols)), selectedSlot,
          i == 0 ? 0f : ROW_PAD);
    }

    if (decorative.isEmpty()) {
      return;
    }
    CustomPanelAPI head = rail.createCustomPanel(rowW, HEADING_H, null);
    TooltipMakerAPI heading = head.createUIElement(rowW, HEADING_H, false);
    heading.addSectionHeading(String.format(CqStrings.mgmt("mgmt_deco_heading"),
        CqSlots.decorativeSlotCount(), CqSlots.maxDecorativeSlotCount()),
        CqUi.base(), CqUi.dark(), Alignment.LMID, 0f);
    head.addUIElement(heading).inTL(0f, 0f);
    body.addCustom(head, regular.isEmpty() ? 0f : 10f);
    int perRow = decoColumns(rowW);
    for (int i = 0; i < decorative.size(); i += perRow) {
      decoRow(rail, body, rowW, decorative.subList(i, Math.min(decorative.size(), i + perRow)),
          selectedSlot, i == 0 ? ROW_PAD : DECO_GAP);
    }
  }

  private static void tileRow(CustomPanelAPI rail, RailPlugin plugin, TooltipMakerAPI body,
      float rowW, CqData data, List<CqSlots.Entry> entries, String selectedSlot, float pad) {
    ShelfRowPlugin draw = new ShelfRowPlugin(entries.size(), TILE_W, TILE_H, PIC_INSET, PIC, 18f);
    CustomPanelAPI row = rail.createCustomPanel(rowW, TILE_H, draw);
    for (int c = 0; c < entries.size(); c++) {
      CqSlots.Entry e = entries.get(c);
      CqCatalog.Furnishing f = e.occupant == null ? null : CqCatalog.get(e.occupant);
      float x = c * (TILE_W + TILE_GAP);
      draw.cell(c, x, e.id.equals(selectedSlot), f == null);

      if (f != null) {
        TooltipMakerAPI icon = row.createUIElement(PIC, PIC, false);
        CqUi.imageByName(row, icon, PIC, PIC, specIcon(f.getId()), 0f);
        row.addUIElement(icon).inTL(x + PIC_INSET - IMG_INSET, PIC_INSET);
      }

      TooltipMakerAPI text = row.createUIElement(NAME_W + PARA_SLACK, TILE_H - 2f, false);
      text.addPara(f == null ? "Empty" : f.getName(),
          f == null ? CqUi.gray() : f.getRarity().getColor(), 0f);
      CqRailMarks.Mark mark = CqUi.markOf(data, e);
      if (mark != CqRailMarks.Mark.NONE) {
        LabelAPI markLine = text.addPara(CqUi.markText(mark), CqUi.markColor(mark), 0f);
        plugin.watch(markLine, f.getWhen(), mark == CqRailMarks.Mark.ACTIVE);
      }
      row.addUIElement(text).inTL(x + NAME_X, 2f);

      TooltipMakerAPI ov = row.createUIElement(TILE_W, TILE_H, false);
      ov.addButton("", CqQuartersView.BTN_SLOT_PREFIX + e.id, CqUi.CLEAR, CqUi.CLEAR,
          Alignment.MID, (CutStyle) null, TILE_W, TILE_H, 0f);
      ov.addTooltipToPrevious(f != null
          ? CqUi.furnishingTip(e.occupant, CqStrings.mgmt("mgmt_row_select_tip"), false, e.id)
          : CqUi.tip(CqStrings.mgmt("mgmt_row_empty_tip")), TooltipMakerAPI.TooltipLocation.LEFT);
      row.addUIElement(ov).inTL(x, 0f);
    }
    body.addCustom(row, pad);
  }

  private static void decoRow(CustomPanelAPI rail, TooltipMakerAPI body, float rowW,
      List<CqSlots.Entry> entries, String selectedSlot, float pad) {
    ShelfRowPlugin draw = new ShelfRowPlugin(entries.size(), DECO_CELL, DECO_CELL, 0f, DECO_CELL,
        12f);
    CustomPanelAPI row = rail.createCustomPanel(rowW, DECO_CELL, draw);
    for (int c = 0; c < entries.size(); c++) {
      CqSlots.Entry e = entries.get(c);
      CqCatalog.Furnishing f = e.occupant == null ? null : CqCatalog.get(e.occupant);
      float x = c * (DECO_CELL + DECO_GAP);
      draw.cell(c, x, e.id.equals(selectedSlot), f == null);
      if (f != null) {
        TooltipMakerAPI icon = row.createUIElement(DECO_CELL, DECO_CELL, false);
        CqUi.imageByName(row, icon, DECO_CELL, DECO_CELL, specIcon(f.getId()), 0f);
        row.addUIElement(icon).inTL(x - IMG_INSET, 0f);
      }
      TooltipMakerAPI ov = row.createUIElement(DECO_CELL, DECO_CELL, false);
      ov.addButton("", CqQuartersView.BTN_SLOT_PREFIX + e.id, CqUi.CLEAR, CqUi.CLEAR,
          Alignment.MID, (CutStyle) null, DECO_CELL, DECO_CELL, 0f);
      ov.addTooltipToPrevious(f != null
          ? CqUi.furnishingTip(e.occupant, CqStrings.mgmt("mgmt_row_select_tip"), true)
          : CqUi.tip(CqStrings.mgmt("mgmt_row_empty_tip")), TooltipMakerAPI.TooltipLocation.LEFT);
      row.addUIElement(ov).inTL(x, 0f);
    }
    body.addCustom(row, pad);
  }

  private static void foot(CustomPanelAPI rail, CqData data, float footW, float top,
      String ask) {
    Long price = CqSlots.nextPrice();
    boolean atCap = price == null;
    boolean canAfford = !atCap && playerCredits() >= price.longValue();
    Long decoPrice = CqSlots.nextDecorativePrice();
    boolean decoAtCap = decoPrice == null;
    boolean decoAfford = !decoAtCap && playerCredits() >= decoPrice.longValue();

    boolean askSlot = ASK_SLOT.equals(ask) && canAfford;
    boolean askDeco = ASK_DECO.equals(ask) && decoAfford;
    if (askSlot || askDeco) {
      Long cost = askSlot ? price : decoPrice;
      TooltipMakerAPI line = rail.createUIElement(footW + PARA_SLACK, 20f, false);
      String costText = CqUi.credits(cost.longValue());
      LabelAPI question = line.addPara(String.format(
          CqStrings.mgmt(askSlot ? "mgmt_add_slot_ask" : "mgmt_add_deco_ask"), costText),
          CqUi.text(), 0f);
      question.setHighlight(costText);
      question.setHighlightColor(CqUi.hl());
      rail.addUIElement(line).inTL(-EL_INSET, top + ASK_LINE_Y);

      float half = (footW - TILE_GAP) / 2f;
      TooltipMakerAPI yes = rail.createUIElement(half, 22f, false);
      yes.addButton(askSlot ? "Add slot" : "Add decorative slot", BTN_ADD_CONFIRM,
          CqUi.base(), CqUi.dark(), half, 22f, 0f);
      yes.addTooltipToPrevious(askSlot ? addSlotTip(false, true, price)
          : decoTip(false, true, decoPrice), TooltipMakerAPI.TooltipLocation.LEFT);
      rail.addUIElement(yes).inTL(-EL_INSET, top + ASK_BTN_Y);

      TooltipMakerAPI no = rail.createUIElement(half, 22f, false);
      no.addButton(CqStrings.mgmt("mgmt_cancel"), BTN_ADD_CANCEL, CqUi.base(), CqUi.dark(),
          half, 22f, 0f);
      rail.addUIElement(no).inTL(half + TILE_GAP - EL_INSET, top + ASK_BTN_Y);
      return;
    }

    TooltipMakerAPI foot = rail.createUIElement(footW + PARA_SLACK, FOOT_H - FOOT_PAD, false);

    ButtonAPI add = foot.addButton(atCap ? "Add slot"
        : "Add slot: " + CqUi.credits(price.longValue()), BTN_ADD_SLOT, CqUi.base(), CqUi.dark(),
        footW, 24f, 0f);
    CqUi.setEnabledSayingWhy(add, !atCap && canAfford);
    foot.addTooltipToPrevious(addSlotTip(atCap, canAfford, price),
        TooltipMakerAPI.TooltipLocation.LEFT);

    ButtonAPI addDeco = foot.addButton(decoAtCap ? "Add decorative slot"
        : "Add decorative slot: " + CqUi.credits(decoPrice.longValue()), BTN_ADD_DECORATIVE,
        CqUi.base(), CqUi.dark(), footW, 22f, ROW_PAD);
    CqUi.setEnabledSayingWhy(addDeco, !decoAtCap && decoAfford);
    foot.addTooltipToPrevious(decoTip(decoAtCap, decoAfford, decoPrice),
        TooltipMakerAPI.TooltipLocation.LEFT);

    ButtonAPI colB = foot.addButton(String.format(CqStrings.mgmt("mgmt_collection_face"),
        CqUi.i(data.getOwned().size()), CqUi.i(CqCatalog.all().size())), BTN_COLLECTION,
        CqUi.base(), CqUi.dark(), footW, 22f, ROW_PAD);
    colB.setShortcut(org.lwjgl.input.Keyboard.KEY_L, false);
    foot.addTooltipToPrevious(CqUi.tip(CqStrings.mgmt("mgmt_collection_tip")),
        TooltipMakerAPI.TooltipLocation.LEFT);

    rail.addUIElement(foot).inTL(-EL_INSET, top + FOOT_PAD);
  }

  public static void layoutStrip(CustomPanelAPI host, float x, float y, CqData data) {
    TooltipMakerAPI label = host.createUIElement(60f + PARA_SLACK, 20f, false);
    label.addPara("Layouts", CqUi.gray(), 0f);
    host.addUIElement(label).inTL(x, y + 14f);

    boolean canSave = !data.isArrangementEmpty();
    for (int i = 0; i < CqData.LAYOUT_SLOTS; i++) {
      String name = CqData.layoutName(i);
      float saveX = x + CqFrame.layoutButtonX(2 * i);
      float loadX = x + CqFrame.layoutButtonX(2 * i + 1);

      TooltipMakerAPI saveEl = host.createUIElement(LAYOUT_BTN_W, 22f, false);
      ButtonAPI saveB = saveEl.addButton("Save " + name, BTN_LAYOUT_SAVE_PREFIX + i,
          CqUi.base(), CqUi.dark(), LAYOUT_BTN_W, 22f, 0f);
      CqUi.setEnabledSayingWhy(saveB, canSave);
      saveEl.addTooltipToPrevious(layoutSaveTip(name, canSave),
          TooltipMakerAPI.TooltipLocation.ABOVE);
      host.addUIElement(saveEl).inTL(saveX, y + 11f);

      TooltipMakerAPI loadEl = host.createUIElement(LAYOUT_BTN_W, 22f, false);
      ButtonAPI loadB = loadEl.addButton("Load " + name, BTN_LAYOUT_LOAD_PREFIX + i,
          CqUi.base(), CqUi.dark(), LAYOUT_BTN_W, 22f, 0f);
      CqUi.setEnabledSayingWhy(loadB, !data.isLayoutEmpty(i));
      loadEl.addTooltipToPrevious(layoutLoadTip(data, i, name),
          TooltipMakerAPI.TooltipLocation.ABOVE);
      host.addUIElement(loadEl).inTL(loadX, y + 11f);
    }
  }

  public static int layoutIndexOf(String buttonId, String prefix) {
    if (buttonId == null || !buttonId.startsWith(prefix)) {
      return -1;
    }
    try {
      int index = Integer.parseInt(buttonId.substring(prefix.length()));
      return index >= 0 && index < CqData.LAYOUT_SLOTS ? index : -1;
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private static CqUi.Tip layoutSaveTip(final String name, final boolean canSave) {
    return new CqUi.Tip(300f) {
      @Override
      public void createTooltip(TooltipMakerAPI t, boolean expanded, Object param) {
        if (!canSave) {
          t.addPara(CqStrings.mgmt("mgmt_layout_save_nothing"), CqUi.gray(), 0f);
          return;
        }
        t.addPara(CqStrings.mgmt("mgmt_layout_save_tip"), 0f, CqUi.hl(), name);
      }
    };
  }

  private static CqUi.Tip layoutLoadTip(final CqData data, final int index, final String name) {
    return new CqUi.Tip(300f) {
      @Override
      public void createTooltip(TooltipMakerAPI t, boolean expanded, Object param) {
        CqData.Layout layout = data.getLayout(index);
        if (layout == null || layout.isEmpty()) {
          t.addPara(CqStrings.mgmt("mgmt_layout_empty"), CqUi.gray(), 0f);
          return;
        }
        t.addPara(CqStrings.mgmt("mgmt_layout_load_tip"), 0f, CqUi.hl(), name);
        int shown = 0;
        int hidden = 0;
        for (int pass = 0; pass < 2; pass++) {
          for (CqData.Slot entry : layout.getSlots()) {
            boolean resolves = data.canApplyLayoutEntry(entry);
            if (resolves != (pass == 1)) {
              continue;
            }
            if (shown >= MAX_TIP_ROWS) {
              hidden++;
              continue;
            }
            shown++;
            CqCatalog.Furnishing f = CqCatalog.get(entry.getOccupant());
            String label = f == null ? String.valueOf(entry.getOccupant()) : f.getName();
            if (resolves) {
              t.addPara("- " + label, CqUi.text(), 3f);
            } else if (f == null || !data.isOwned(entry.getOccupant())) {
              t.addPara(String.format(CqStrings.mgmt("mgmt_layout_missing_entry"), label),
                  CqUi.warn(), 3f);
            } else {
              t.addPara(String.format(CqStrings.mgmt("mgmt_layout_missing_slot"), label),
                  CqUi.warn(), 3f);
            }
          }
        }
        if (hidden > 0) {
          t.addPara(String.format(CqStrings.mgmt("mgmt_layout_more"), hidden), CqUi.gray(), 3f);
        }
        String style = layout.getStyle();
        if (style != null && !style.equals(data.getStyle()) && CqData.STYLE_IDS.contains(style)) {
          t.addPara(String.format(CqStrings.mgmt("mgmt_layout_style_note"),
              CqUi.styleName(style)), CqUi.text(), 8f);
        }
      }
    };
  }

  private static CqUi.Tip decoTip(final boolean atCap, final boolean canAfford, final Long price) {
    return new CqUi.Tip() {
      @Override
      public void createTooltip(TooltipMakerAPI t, boolean expanded, Object param) {
        if (atCap) {
          t.addPara(CqStrings.mgmt("mgmt_add_deco_cap"), CqUi.gray(), 0f);
          return;
        }
        if (!canAfford) {
          t.addPara(CqStrings.mgmt("mgmt_requires"), 0f, CqUi.hl(), CqUi.credits(price.longValue()));
          return;
        }
        t.addPara(CqStrings.mgmt("mgmt_add_deco_tip"), 0f, CqUi.hl(),
            CqUi.credits(price.longValue()));
      }
    };
  }

  private static CqUi.Tip addSlotTip(final boolean atCap, final boolean canAfford, final Long price) {
    return new CqUi.Tip() {
      @Override
      public void createTooltip(TooltipMakerAPI t, boolean expanded, Object param) {
        if (atCap) {
          t.addPara(CqStrings.mgmt("mgmt_add_slot_cap"), CqUi.gray(), 0f);
          return;
        }
        if (!canAfford) {
          t.addPara(CqStrings.mgmt("mgmt_requires"), 0f, CqUi.hl(), CqUi.credits(price.longValue()));
        } else {
          t.addPara(CqStrings.mgmt("mgmt_add_slot_tip"), 0f, CqUi.hl(),
              CqUi.credits(price.longValue()));
        }
        List<Long> rest = CqSlots.remainingLadderPrices();
        if (rest.size() > 1) {
          StringBuilder sb = new StringBuilder();
          for (int i = 0; i < rest.size(); i++) {
            if (i > 0) {
              sb.append(", ");
            }
            sb.append(CqUi.credits(rest.get(i).longValue()));
          }
          t.addPara(String.format(CqStrings.mgmt("mgmt_ladder_rest"), sb), CqUi.gray(), 8f);
          t.addPara(CqStrings.mgmt("mgmt_ladder_total"), 4f, CqUi.hl(),
              CqUi.credits(CqSlots.remainingLadderCost()));
        }
      }
    };
  }

  private static long playerCredits() {
    try {
      return (long) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
    } catch (Throwable t) {
      return 0L;
    }
  }

  private static void picker(CustomPanelAPI rail, TooltipMakerAPI body, float rowW, CqData data,
      String selectedSlot, boolean viewer) {
    float headH = 24f;
    CqSlots.Entry selEntry = selectedSlot == null ? null : CqSlots.byId(selectedSlot);
    boolean decorative = selEntry != null && selEntry.decorative;
    String title = viewer ? "Collection"
        : CqStrings.mgmt(decorative ? "mgmt_picker_title_deco" : "mgmt_picker_title");
    CustomPanelAPI head = rail.createCustomPanel(rowW, headH, CqUi.consoleRow());
    TooltipMakerAPI heading = head.createUIElement(rowW - 66f, headH, false);
    heading.addSectionHeading(title, CqUi.base(), CqUi.dark(), Alignment.LMID, 0f);
    head.addUIElement(heading).inTL(0f, 0f);
    TooltipMakerAPI back = head.createUIElement(60f, headH, false);
    ButtonAPI backB = back.addButton("Back", BTN_BACK, CqUi.base(), CqUi.dark(), 56f, 20f, 0f);
    backB.setShortcut(org.lwjgl.input.Keyboard.KEY_B, false);
    back.addTooltipToPrevious(CqUi.tip(CqStrings.mgmt("mgmt_back_tip")),
        TooltipMakerAPI.TooltipLocation.BELOW);
    head.addUIElement(back).inTL(rowW - 60f, 1f);
    body.addCustom(head, 0f);

    if (data.getOwned().isEmpty()) {
      body.addPara(CqStrings.mgmt("mgmt_collection_empty"), CqUi.text(), 8f);
      body.addPara(CqStrings.mgmt("mgmt_collection_empty_hint"), CqUi.gray(), 4f);
      return;
    }

    String current = selectedSlot == null ? null : occupantOf(selectedSlot);

    Map<String, List<CqCatalog.Furnishing>> groups = ownedByCategory(data);
    for (Map.Entry<String, List<CqCatalog.Furnishing>> g : groups.entrySet()) {
      categoryHeader(rail, body, rowW, g.getKey());
      for (CqCatalog.Furnishing f : g.getValue()) {
        collectionRow(rail, body, rowW, f, current, selectedSlot, viewer, decorative);
      }
    }
  }

  private static Map<String, List<CqCatalog.Furnishing>> ownedByCategory(CqData data) {
    Map<String, List<CqCatalog.Furnishing>> out = new LinkedHashMap<String, List<CqCatalog.Furnishing>>();
    for (String cat : CqData.SLOT_IDS) {
      List<CqCatalog.Furnishing> items = new ArrayList<CqCatalog.Furnishing>();
      for (CqCatalog.Furnishing f : CqCatalog.forSlot(cat)) {
        if (data.isOwned(f.getId())) {
          items.add(f);
        }
      }
      if (!items.isEmpty()) {
        out.put(cat, items);
      }
    }
    return out;
  }

  private static void categoryHeader(CustomPanelAPI rail, TooltipMakerAPI body, float rowW, String cat) {
    float h = 22f;
    CustomPanelAPI row = rail.createCustomPanel(rowW, h, null);
    TooltipMakerAPI icon = row.createUIElement(22f, h, false);
    String glyph = CqUi.sprite("cq_glyphs", cat);
    if (glyph != null) {
      icon.addImage(glyph, 18f, 18f, 2f);
    }
    row.addUIElement(icon).inTL(0f, 1f);
    TooltipMakerAPI label = row.createUIElement(rowW - 26f, h, false);
    label.addPara(CqCatalog.slotName(cat), CqUi.base(), 3f);
    row.addUIElement(label).inTL(26f, 0f);
    body.addCustom(row, 8f);
  }

  private static void collectionRow(CustomPanelAPI rail, TooltipMakerAPI body, float rowW,
      CqCatalog.Furnishing f, String currentOccupant, String selectedSlot, boolean viewer,
      boolean decorative) {
    boolean isCurrent = f.getId().equals(currentOccupant);
    Integer usedIn = CqSlots.slotNumberOf(f.getId());
    boolean inUseElsewhere = usedIn != null && !isCurrent;
    boolean move = !viewer && !decorative && inUseElsewhere;
    int decoCount = viewer ? decorativePlacements(f.getId()) : 0;

    float iconW = PIC;
    float inset = isCurrent ? 10f : 0f;
    float textW = rowW - iconW - 2f * PAD - inset;

    boolean rowEffect = !decorative && f.hasEffect();
    float rowH = 26f + (rowEffect ? LINE_H * lines(f.getEffectText(), textW) : 0f)
        + ((isCurrent || inUseElsewhere || decoCount > 0) ? LINE_H : 0f);
    rowH = Math.max(rowH, iconW + 4f);
    CustomPanelAPI row = rail.createCustomPanel(rowW, rowH, CqUi.consoleRow());

    TooltipMakerAPI icon = row.createUIElement(iconW, rowH, false);
    CqUi.imageByName(row, icon, iconW, iconW, specIcon(f.getId()), 2f);
    row.addUIElement(icon).inTL(inset - IMG_INSET, 2f);

    boolean greyed = !viewer && (isCurrent || inUseElsewhere);
    TooltipMakerAPI text = row.createUIElement(textW + PARA_SLACK, rowH, false);
    text.addPara(f.getName(), greyed ? CqUi.gray() : f.getRarity().getColor(), 2f);
    if (rowEffect) {
      if (greyed) {
        text.addPara(f.getEffectText(), CqUi.gray(), 1f);
      } else {
        CqUi.addEffectLine(text, f, 1f);
      }
    }
    if (isCurrent) {
      text.addPara(CqStrings.mgmt("mgmt_slotted_here"), CqUi.gray(), 1f);
    } else if (move) {
      text.addPara(CqStrings.mgmt("mgmt_move_consequence"), CqUi.gray(), 1f);
    } else if (viewer && (usedIn != null || decoCount > 0)) {
      text.addPara(placementLine(usedIn, decoCount), CqUi.gray(), 1f);
    } else if (inUseElsewhere) {
      text.addPara(CqStrings.mgmt("mgmt_in_slot"), CqUi.gray(), 1f);
    }
    row.addUIElement(text).inTL(inset + iconW + PAD, 2f);

    if (!viewer) {
      TooltipMakerAPI ov = row.createUIElement(rowW - inset, rowH, false);
      ButtonAPI b = ov.addButton("", BTN_PICK_PREFIX + f.getId(), CqUi.CLEAR, CqUi.CLEAR,
          Alignment.MID, (CutStyle) null, rowW - inset, rowH, 0f);
      CqUi.setEnabledSayingWhy(b, !isCurrent);
      String footer = isCurrent ? CqStrings.mgmt("mgmt_already_here")
          : (move ? CqStrings.mgmt("mgmt_move_consequence")
              : CqStrings.mgmt("mgmt_place_tip"));
      ov.addTooltipToPrevious(CqUi.furnishingTip(f.getId(), footer, decorative),
          TooltipMakerAPI.TooltipLocation.LEFT);
      row.addUIElement(ov).inTL(inset, 0f);
    } else {
      TooltipMakerAPI ov = row.createUIElement(rowW, rowH, false);
      ov.addButton("", "cq_btn_noop", CqUi.CLEAR, CqUi.CLEAR, Alignment.MID, (CutStyle) null,
          rowW, rowH, 0f);
      ov.addTooltipToPrevious(CqUi.furnishingTip(f.getId(),
          usedIn != null || decoCount > 0 ? placementLine(usedIn, decoCount) : null),
          TooltipMakerAPI.TooltipLocation.LEFT);
      row.addUIElement(ov).inTL(0f, 0f);
    }

    body.addCustom(row, ROW_PAD);
  }

  private static int decorativePlacements(String itemId) {
    int n = 0;
    for (CqSlots.Entry e : CqSlots.list()) {
      if (e.decorative && itemId.equals(e.occupant)) {
        n++;
      }
    }
    return n;
  }

  private static String placementLine(Integer usedIn, int decoCount) {
    StringBuilder sb = new StringBuilder();
    if (usedIn != null) {
      sb.append(CqStrings.mgmt("mgmt_in_slot"));
    }
    if (decoCount > 0) {
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(String.format(CqStrings.mgmt("mgmt_deco_count"), decoCount));
    }
    return sb.toString();
  }

  public static Integer slotNumber(String slotId) {
    List<CqSlots.Entry> list = CqSlots.list();
    CqSlots.Entry target = null;
    for (CqSlots.Entry e : list) {
      if (e.id.equals(slotId)) {
        target = e;
        break;
      }
    }
    if (target == null) {
      return null;
    }
    int n = 0;
    for (CqSlots.Entry e : list) {
      if (e.decorative == target.decorative) {
        n++;
        if (e.id.equals(slotId)) {
          return Integer.valueOf(n);
        }
      }
    }
    return null;
  }

  public static String occupantOf(String slotId) {
    for (CqSlots.Entry e : CqSlots.list()) {
      if (e.id.equals(slotId)) {
        return e.occupant;
      }
    }
    return null;
  }

  private static float lines(String text, float width) {
    if (text == null || width <= 20f) {
      return 1f;
    }
    return Math.max(1f, (float) Math.ceil(text.length() * CHAR_W / width));
  }

  private static String specIcon(String itemId) {
    try {
      com.fs.starfarer.api.campaign.SpecialItemSpecAPI spec =
          Global.getSettings().getSpecialItemSpec(itemId);
      if (spec != null && spec.getIconName() != null && !spec.getIconName().isEmpty()) {
        return spec.getIconName();
      }
    } catch (Throwable ignore) {
    }
    return null;
  }

  private static final class RailPlugin extends BaseCustomUIPanelPlugin {

    private static final class LiveMark {
      final LabelAPI label;
      final CqWhen when;
      boolean shown;

      LiveMark(LabelAPI label, CqWhen when, boolean shown) {
        this.label = label;
        this.when = when;
        this.shown = shown;
      }
    }

    private List<LiveMark> marks;
    private float sinceCheck;

    void watch(LabelAPI label, CqWhen when, boolean shown) {
      if (marks == null) {
        marks = new ArrayList<LiveMark>();
      }
      marks.add(new LiveMark(label, when, shown));
    }

    @Override
    public void advance(float amount) {
      if (marks == null) {
        return;
      }
      sinceCheck += amount;
      if (sinceCheck < CqRailMarks.CHECK_SECONDS) {
        return;
      }
      sinceCheck = 0f;
      if (!CqData.exists()) {
        return;
      }
      CqData data = CqData.getInstance();
      for (LiveMark m : marks) {
        boolean now = CqOccasions.holds(data, m.when);
        if (now != m.shown) {
          m.shown = now;
          CqRailMarks.Mark mark = now ? CqRailMarks.Mark.ACTIVE : CqRailMarks.Mark.INACTIVE;
          m.label.setText(CqUi.markText(mark));
          m.label.setColor(CqUi.markColor(mark));
        }
      }
    }

    @Override
    public void buttonPressed(Object buttonId) {
      CqConsolePanel.routeButton(buttonId);
    }
  }

  private static final class ShelfRowPlugin extends BaseCustomUIPanelPlugin {

    private final float cellW;
    private final float cellH;
    private final float picInset;
    private final float picSize;
    private final float plusLen;
    private final float[] cellX;
    private final boolean[] selected;
    private final boolean[] empty;
    private final Color dark = CqUi.dark();
    private final Color bright = CqUi.bright();
    private final Color plus = Misc.interpolateColor(CqUi.dark(), CqUi.base(), 0.25f);
    private PositionAPI pos;

    ShelfRowPlugin(int cells, float cellW, float cellH, float picInset, float picSize,
        float plusLen) {
      this.cellW = cellW;
      this.cellH = cellH;
      this.picInset = picInset;
      this.picSize = picSize;
      this.plusLen = plusLen;
      this.cellX = new float[cells];
      this.selected = new boolean[cells];
      this.empty = new boolean[cells];
    }

    void cell(int index, float x, boolean isSelected, boolean isEmpty) {
      cellX[index] = x;
      selected[index] = isSelected;
      empty[index] = isEmpty;
    }

    @Override
    public void positionChanged(PositionAPI position) {
      pos = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
      if (pos == null || alphaMult <= 0.01f) {
        return;
      }
      beginFlat();
      for (int i = 0; i < cellX.length; i++) {
        float left = Math.round(pos.getX() + cellX[i]);
        float top = Math.round(pos.getY() + pos.getHeight());
        if (selected[i]) {
          quad(left, top - cellH, cellW, cellH, dark, 0.55f * alphaMult);
        }
        float picLeft = left + picInset;
        float picBottom = top - picInset - picSize;
        quad(picLeft, picBottom, picSize, picSize, Color.BLACK, alphaMult);
        if (empty[i]) {
          float cx = picLeft + picSize / 2f;
          float cy = picBottom + picSize / 2f;
          quad(cx - plusLen / 2f, cy - 1f, plusLen, 2f, plus, alphaMult);
          quad(cx - 1f, cy - plusLen / 2f, 2f, plusLen, plus, alphaMult);
        }
      }
      endFlat();
    }

    @Override
    public void render(float alphaMult) {
      if (pos == null || alphaMult <= 0.01f) {
        return;
      }
      beginFlat();
      for (int i = 0; i < cellX.length; i++) {
        float left = Math.round(pos.getX() + cellX[i]);
        float top = Math.round(pos.getY() + pos.getHeight());
        border(left + picInset, top - picInset - picSize, picSize, picSize,
            selected[i] ? bright : dark, alphaMult);
        if (selected[i] && picInset > 0f) {
          border(left, top - cellH, cellW, cellH, bright, alphaMult);
        }
      }
      endFlat();
    }

    @Override
    public void buttonPressed(Object buttonId) {
      CqConsolePanel.routeButton(buttonId);
    }
  }

  private static void beginFlat() {
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
  }

  private static void endFlat() {
    GL11.glPopAttrib();
  }

  private static void quad(float x, float y, float w, float h, Color c, float alpha) {
    GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha);
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(x, y + h);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(x + w, y);
    GL11.glEnd();
  }

  private static void border(float x, float y, float w, float h, Color c, float alpha) {
    quad(x, y, w, 1f, c, alpha);
    quad(x, y + h - 1f, w, 1f, c, alpha);
    quad(x, y + 1f, 1f, h - 2f, c, alpha);
    quad(x + w - 1f, y + 1f, 1f, h - 2f, c, alpha);
  }
}
