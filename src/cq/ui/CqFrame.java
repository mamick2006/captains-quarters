package cq.ui;

public final class CqFrame {

  public static final float GUTTER = 10f;
  public static final float MIN_CABIN_W = 400f;
  private static final double CABIN_ASPECT = 1.6;

  public static final float BUTTONS_DY = 6f;
  public static final float BUTTON_H = 22f;
  public static final float BUTTON_PITCH = 26f;
  public static final float NAME_DY = 28f;
  public static final float EFFECT_DY = 17f;
  public static final float STRIP_DY = 60f;
  public static final float STRIP_H = 44f;
  public static final float UNDER_H = 154f;

  public static final int BUTTON_COUNT = 10;
  private static final float[] BUTTON_W = { 54f, 22f, 22f, 38f, 64f, 64f, 100f, 82f, 60f, 56f };
  public static final float BUTTON_GAP = 4f;
  public static final float ONE_ROW_W = 598f;

  public static final float STYLE_X = 46f;
  public static final float STYLE_PITCH = 70f;
  public static final float STYLE_W = 64f;
  public static final float STYLE_H = 40f;
  public static final float LAYOUTS_X = 414f;
  public static final float LAYOUTS_W = 436f;
  public static final float LAYOUT_BTN_W = 58f;
  public static final float LAYOUT_BTNS_X = 60f;
  public static final float LAYOUT_PAIR_GAP = 8f;

  public static final float TILE_PITCH = 74f;
  public static final float FOOT_PAD = 8f;
  public static final float FOOT_H = 84f;

  public final float cw;
  public final float ch;
  public final float leftW;
  public final float cabinW;
  public final float cabinH;
  public final float shelfX;
  public final float shelfW;
  private final float[] buttonX = new float[BUTTON_COUNT];
  private final float[] buttonY = new float[BUTTON_COUNT];
  public final int buttonRows;
  public final float nameY;
  public final float effectY;
  public final float stripY;
  public final float layoutsX;
  public final float layoutsY;
  public final float underBottom;

  private CqFrame(float cw, float ch, float railW) {
    this.cw = cw;
    this.ch = ch;
    float widest = cw - railW - GUTTER;
    float fitted = Math.min(widest, fitWidth(ch, UNDER_H));
    if (fitted < MIN_CABIN_W) {
      leftW = Math.round(cw * 0.62f) - GUTTER / 2f;
      shelfW = cw - leftW - GUTTER;
      cabinW = Math.max(0f, Math.min(leftW, fitWidth(ch, underHeight(leftW))));
    } else {
      float w = fitted;
      for (int pass = 0; pass < 4; pass++) {
        float next = Math.min(widest, fitWidth(ch, underHeight(w)));
        if (next >= w) {
          break;
        }
        w = next;
      }
      leftW = w;
      shelfW = railW;
      cabinW = leftW;
    }
    cabinH = (float) Math.floor(cabinW * 0.625);
    shelfX = leftW + GUTTER;

    int rows = 1;
    if (leftW >= ONE_ROW_W) {
      float x = 0f;
      for (int i = 0; i < BUTTON_COUNT - 2; i++) {
        buttonX[i] = x;
        x += BUTTON_W[i] + BUTTON_GAP;
      }
      buttonX[8] = leftW - BUTTON_W[9] - BUTTON_GAP - BUTTON_W[8];
      buttonX[9] = leftW - BUTTON_W[9];
      for (int i = 0; i < BUTTON_COUNT; i++) {
        buttonY[i] = cabinH + BUTTONS_DY;
      }
    } else {
      float x = 0f;
      for (int i = 0; i < BUTTON_COUNT; i++) {
        if (x > 0f && x + BUTTON_W[i] > leftW) {
          rows++;
          x = 0f;
        }
        buttonX[i] = x;
        buttonY[i] = cabinH + BUTTONS_DY + (rows - 1) * BUTTON_PITCH;
        x += BUTTON_W[i] + BUTTON_GAP;
      }
    }
    buttonRows = rows;
    nameY = cabinH + BUTTONS_DY + (rows - 1) * BUTTON_PITCH + NAME_DY;
    effectY = nameY + EFFECT_DY;
    stripY = effectY + STRIP_DY;
    boolean wrap = layoutsWrap(leftW);
    layoutsX = wrap ? 0f : LAYOUTS_X;
    layoutsY = wrap ? stripY + STRIP_H : stripY;
    underBottom = layoutsY + STRIP_H;
  }

  public static CqFrame of(float cw, float ch, float railW) {
    return new CqFrame(cw, ch, railW);
  }

  private static float fitWidth(float ch, float under) {
    return (float) Math.floor((ch - under) * CABIN_ASPECT);
  }

  private static boolean layoutsWrap(float leftW) {
    return leftW < LAYOUTS_X + LAYOUTS_W;
  }

  private static int buttonRowsAt(float leftW) {
    if (leftW >= ONE_ROW_W) {
      return 1;
    }
    int rows = 1;
    float x = 0f;
    for (int i = 0; i < BUTTON_COUNT; i++) {
      if (x > 0f && x + BUTTON_W[i] > leftW) {
        rows++;
        x = 0f;
      }
      x += BUTTON_W[i] + BUTTON_GAP;
    }
    return rows;
  }

  private static float underHeight(float leftW) {
    return UNDER_H + (buttonRowsAt(leftW) - 1) * BUTTON_PITCH
        + (layoutsWrap(leftW) ? STRIP_H : 0f);
  }

  public float buttonX(int i) {
    return buttonX[i];
  }

  public float buttonY(int i) {
    return buttonY[i];
  }

  public static float buttonW(int i) {
    return BUTTON_W[i];
  }

  public static float styleX(int i) {
    return STYLE_X + i * STYLE_PITCH;
  }

  public static float layoutButtonX(int i) {
    float layout = 2f * LAYOUT_BTN_W + BUTTON_GAP + LAYOUT_PAIR_GAP;
    return LAYOUT_BTNS_X + (i / 2) * layout + (i % 2) * (LAYOUT_BTN_W + BUTTON_GAP);
  }

  public float shelfViewH() {
    return ch - FOOT_H - FOOT_PAD;
  }

  public float footY(float contentH) {
    return Math.min(contentH + FOOT_PAD, ch - FOOT_H);
  }

  public int tileRows() {
    return (int) Math.floor(shelfViewH() / TILE_PITCH);
  }

  public float groundUnder() {
    return Math.max(0f, ch - underBottom);
  }
}
