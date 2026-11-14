package cq.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import cq.campaign.CqData;
import cq.campaign.CqSettings;
import cq.campaign.CqStrings;
import cq.effects.CqOccasions;
import cq.effects.pure.CqRailMarks;
import cq.effects.pure.CqWhen;
import cq.items.CqCatalog;
import cq.items.CqEffectText;

public final class CqUi {

  private CqUi() {
  }

  public static Color base() {
    return Misc.getBasePlayerColor();
  }

  public static Color dark() {
    return Misc.getDarkPlayerColor();
  }

  public static Color bright() {
    return Misc.getBrightPlayerColor();
  }

  public static Color text() {
    return Misc.getTextColor();
  }

  public static Color gray() {
    return Misc.getGrayColor();
  }

  public static Color hl() {
    return Misc.getHighlightColor();
  }

  public static Color good() {
    return Misc.getPositiveHighlightColor();
  }

  public static Color bad() {
    return Misc.getNegativeHighlightColor();
  }

  public static Color warn() {
    return new Color(255, 160, 60);
  }

  public static CustomUIPanelPlugin consoleRow() {
    return CqConsoleRoute.INSTANCE;
  }

  public static String i(float v) {
    return "" + Math.round(v);
  }

  public static String f1(float v) {
    return String.format("%.1f", v);
  }

  public static String pct(float frac) {
    return Math.round(frac * 100f) + "%";
  }

  public static String credits(float v) {
    return Misc.getDGSCredits(v);
  }

  public static String sprite(String category, String id) {
    try {
      String s = Global.getSettings().getSpriteName(category, id);
      return s == null || s.isEmpty() ? null : s;
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean image(CustomPanelAPI outer, TooltipMakerAPI el, float w, float h,
      String category, String spriteId, float pad) {
    String sprite = sprite(category, spriteId);
    if (sprite == null) {
      return false;
    }
    CustomPanelAPI holder = outer.createCustomPanel(w, h, null);
    TooltipMakerAPI img = holder.createUIElement(w, h, false);
    img.addImage(sprite, w, h, 0f);
    holder.addUIElement(img).inTL(0f, 0f);
    el.addCustom(holder, pad);
    return true;
  }

  public static boolean imageByName(CustomPanelAPI outer, TooltipMakerAPI el, float w, float h,
      String spriteName, float pad) {
    if (spriteName == null || spriteName.isEmpty()) {
      return false;
    }
    CustomPanelAPI holder = outer.createCustomPanel(w, h, null);
    TooltipMakerAPI img = holder.createUIElement(w, h, false);
    img.addImage(spriteName, w, h, 0f);
    holder.addUIElement(img).inTL(0f, 0f);
    el.addCustom(holder, pad);
    return true;
  }

  public static SpriteAPI spriteObj(String category, String id) {
    if (sprite(category, id) == null) {
      return null;
    }
    try {
      return Global.getSettings().getSprite(category, id);
    } catch (Exception e) {
      return null;
    }
  }

  public static SpriteAPI backgroundSprite(String styleId) {
    return spriteObj("cq", "cq_bg_" + trimPrefix(styleId, "cq_style_"));
  }

  public static SpriteAPI furnishingSprite(String itemId) {
    return spriteObj("cq", "cq_f_" + trimPrefix(itemId, "cq_"));
  }

  public static final Color CLEAR = new Color(0, 0, 0, 0);

  public static Color alpha(Color c, int a) {
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
  }

  public static Color widgetBright() {
    try {
      Color c = Global.getSettings().getColor("widgetBorderColorBright");
      if (c != null) {
        return c;
      }
    } catch (Exception ignore) {
    }
    return bright();
  }

  public static String styleName(String styleId) {
    return CqData.styleDisplayName(styleId);
  }

  private static String trimPrefix(String s, String prefix) {
    return s != null && s.startsWith(prefix) ? s.substring(prefix.length()) : s;
  }

  private static final Map<String, float[]> STICKER_SIZE_CACHE = new HashMap<String, float[]>();
  private static final float[] NO_SIZE = new float[0];

  public static float[] stickerSize(String categoryId) {
    float[] hit = STICKER_SIZE_CACHE.get(categoryId);
    if (hit == null) {
      float[] raw = CqSettings.getFloatArray(
          "cq_sticker_size_" + trimPrefix(categoryId, "cq_slot_"));
      hit = raw != null && raw.length >= 2 && raw[0] > 0f && raw[1] > 0f
          ? new float[] { raw[0], raw[1] } : NO_SIZE;
      STICKER_SIZE_CACHE.put(categoryId, hit);
    }
    return hit == NO_SIZE ? null : hit;
  }

  public abstract static class Tip implements TooltipMakerAPI.TooltipCreator {

    private final float width;

    public Tip() {
      this(360f);
    }

    public Tip(float width) {
      this.width = width;
    }

    @Override
    public boolean isTooltipExpandable(Object param) {
      return false;
    }

    @Override
    public float getTooltipWidth(Object param) {
      return width;
    }
  }

  public static void setEnabledSayingWhy(ButtonAPI button, boolean enabled) {
    button.setEnabled(enabled);
    button.setShowTooltipWhileInactive(true);
  }

  public static Tip tip(final String text) {
    return new Tip(300f) {
      @Override
      public void createTooltip(TooltipMakerAPI t, boolean expanded, Object param) {
        t.addPara(text, text(), 0f);
      }
    };
  }

  public static LabelAPI addEffectLine(TooltipMakerAPI el, CqCatalog.Furnishing f, float pad) {
    LabelAPI l = el.addPara(f.getEffectText(), text(), pad);
    String[] hls = f.getEffectHighlights();
    if (hls != null && hls.length > 0) {
      l.setHighlight(hls);
      l.setHighlightColor(hl());
    }
    return l;
  }

  public static CqRailMarks.Mark markOf(CqData data, CqSlots.Entry e) {
    if (data == null || e == null || e.occupant == null || e.decorative) {
      return CqRailMarks.Mark.NONE;
    }
    CqCatalog.Furnishing f = CqCatalog.get(e.occupant);
    if (f == null || !f.hasEffect()) {
      return CqRailMarks.Mark.NONE;
    }
    CqWhen when = CqEffectText.statesOccasion(f) ? f.getWhen() : null;
    return CqRailMarks.mark(e.decorative, when, when != null && CqOccasions.holds(data, when));
  }

  public static String markText(CqRailMarks.Mark mark) {
    switch (mark) {
      case ACTIVE:
        return CqStrings.effects("rail_mark_active");
      case INACTIVE:
        return CqStrings.effects("rail_mark_inactive");
      default:
        return "";
    }
  }

  public static Color markColor(CqRailMarks.Mark mark) {
    return mark == CqRailMarks.Mark.ACTIVE ? hl() : gray();
  }

  public static LabelAPI markLine(TooltipMakerAPI el, CqData data, CqSlots.Entry e, float pad) {
    CqRailMarks.Mark mark = markOf(data, e);
    if (mark == CqRailMarks.Mark.NONE) {
      return null;
    }
    return el.addPara(markText(mark), markColor(mark), pad);
  }

  public static void furnishingCardCore(TooltipMakerAPI t, String itemId) {
    furnishingCardCore(t, itemId, false);
  }

  public static void furnishingCardCore(TooltipMakerAPI t, String itemId, boolean noEffect) {
    furnishingCardCore(t, itemId, noEffect, null);
  }

  public static void furnishingCardCore(TooltipMakerAPI t, String itemId, boolean noEffect,
      String markSlot) {
    float pad = 3f;
    float opad = 10f;
    String desc = specDesc(itemId);
    if (desc != null) {
      t.addPara(desc, gray(), opad);
    }
    CqCatalog.Furnishing f = CqCatalog.get(itemId);
    if (f == null) {
      return;
    }
    if (!noEffect && f.hasEffect()) {
      t.addSectionHeading("Effect", Alignment.MID, opad);
      addEffectLine(t, f, opad);
      if (markSlot != null && CqData.exists()) {
        markLine(t, CqData.getInstance(), CqSlots.byId(markSlot), pad);
      }
    }
    t.addPara("Category: %s", pad, hl(), CqCatalog.slotName(f.getSlotId()));
    t.addPara(f.getRarity().getDisplayName() + " furnishing", f.getRarity().getColor(), pad);
  }

  private static String specDesc(String itemId) {
    try {
      SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(itemId);
      if (spec != null && spec.getDesc() != null && !spec.getDesc().isEmpty()) {
        return spec.getDesc();
      }
    } catch (Throwable ignore) {
    }
    return null;
  }

  public static Tip furnishingTip(final String itemId, final String footer) {
    return furnishingTip(itemId, footer, false);
  }

  public static Tip furnishingTip(final String itemId, final String footer, final boolean noEffect) {
    return furnishingTip(itemId, footer, noEffect, null);
  }

  public static Tip furnishingTip(final String itemId, final String footer, final boolean noEffect,
      final String markSlot) {
    return new Tip() {
      @Override
      public void createTooltip(TooltipMakerAPI t, boolean expanded, Object param) {
        CqCatalog.Furnishing f = CqCatalog.get(itemId);
        t.addTitle(f == null ? "Furnishing" : f.getName());
        furnishingCardCore(t, itemId, noEffect, markSlot);
        if (footer != null) {
          t.addPara(footer, gray(), 10f);
        }
      }
    };
  }

}
