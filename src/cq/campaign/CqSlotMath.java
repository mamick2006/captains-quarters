package cq.campaign;

import java.util.ArrayList;
import java.util.List;

public final class CqSlotMath {

  private CqSlotMath() {
  }

  public static final int DEFAULT_INITIAL_COUNT = 5;
  public static final int DEFAULT_MAX_COUNT = 15;
  public static final long[] DEFAULT_PRICES = {
      5000L, 10000L, 20000L, 50000L, 100000L, 200000L, 500000L, 1000000L, 2000000L, 5000000L};

  public static int maxPurchases(int initialCount, int maxCount) {
    return Math.max(0, maxCount - initialCount);
  }

  public static int slotCount(int purchases, int initialCount, int maxCount) {
    int clamped = Math.max(0, Math.min(purchases, maxPurchases(initialCount, maxCount)));
    return initialCount + clamped;
  }

  public static boolean canPurchase(int purchases, int initialCount, int maxCount) {
    return purchases >= 0 && purchases < maxPurchases(initialCount, maxCount);
  }

  public static long priceOf(int purchaseIndex, long[] ladder) {
    if (ladder == null || purchaseIndex < 0 || purchaseIndex >= ladder.length) {
      return -1L;
    }
    return ladder[purchaseIndex];
  }

  public static long priceOf(int purchaseIndex, long[] ladder, Long override) {
    long base = priceOf(purchaseIndex, ladder);
    if (base < 0) {
      return -1L;
    }
    return override != null ? override.longValue() : base;
  }

  public static List<Long> remainingPrices(int purchases, int initialCount, int maxCount,
      long[] ladder, Long[] overrides) {
    List<Long> out = new ArrayList<Long>();
    int max = maxPurchases(initialCount, maxCount);
    for (int i = Math.max(0, purchases); i < max; i++) {
      Long override = overrides != null && i < overrides.length ? overrides[i] : null;
      long price = priceOf(i, ladder, override);
      if (price >= 0) {
        out.add(Long.valueOf(price));
      }
    }
    return out;
  }

  public static long cumulative(long[] ladder) {
    long sum = 0L;
    if (ladder != null) {
      for (long price : ladder) {
        sum += price;
      }
    }
    return sum;
  }

  public static float clampScale(float scale, float min, float max) {
    if (Float.isNaN(scale) || Float.isInfinite(scale) || scale <= 0f) {
      return 1f;
    }
    return Math.max(min, Math.min(max, scale));
  }

  public static int normalizeRotSteps(int steps, int stepsPerCircle) {
    if (stepsPerCircle <= 0) {
      return 0;
    }
    return Math.floorMod(steps, stepsPerCircle);
  }

  public static float[] rotatedAabb(float w, float h, float degrees) {
    double rad = Math.toRadians(degrees);
    float cos = (float) Math.abs(Math.cos(rad));
    float sin = (float) Math.abs(Math.sin(rad));
    return new float[] {w * cos + h * sin, w * sin + h * cos};
  }

  public static float clampCenterInside(float topLeft, float aabbSpan, float frameSpan) {
    float lo = -aabbSpan / 2f;
    float hi = frameSpan - aabbSpan / 2f;
    return topLeft < lo ? lo : (topLeft > hi ? hi : topLeft);
  }

  public static float[] visibleSubRect(float x, float y, float w, float h,
      float frameW, float frameH) {
    float x0 = Math.max(x, 0f);
    float y0 = Math.max(y, 0f);
    float x1 = Math.min(x + w, frameW);
    float y1 = Math.min(y + h, frameH);
    return new float[] {x0, y0, Math.max(0f, x1 - x0), Math.max(0f, y1 - y0)};
  }

  public static int furnishedRegular(String[] occupants, boolean[] decorative) {
    int n = occupants == null ? 0 : occupants.length;
    int furnished = 0;
    for (int i = 0; i < n; i++) {
      boolean deco = decorative != null && i < decorative.length && decorative[i];
      if (occupants[i] != null && !deco) {
        furnished++;
      }
    }
    return furnished;
  }

  public static boolean isActiveElsewhere(List<String> occupants, int slotIndex, String itemId) {
    if (occupants == null || itemId == null) {
      return false;
    }
    for (int i = 0; i < occupants.size(); i++) {
      if (i != slotIndex && itemId.equals(occupants.get(i))) {
        return true;
      }
    }
    return false;
  }

  public static boolean[] dedupeClearMask(String[] occupants, boolean[] placed) {
    int n = occupants == null ? 0 : occupants.length;
    boolean[] clear = new boolean[n];
    for (int i = 0; i < n; i++) {
      String id = occupants[i];
      if (id == null) {
        continue;
      }
      boolean firstOfId = true;
      for (int j = 0; j < i; j++) {
        if (id.equals(occupants[j])) {
          firstOfId = false;
          break;
        }
      }
      if (!firstOfId) {
        continue;
      }
      int keep = -1;
      int deepest = -1;
      for (int j = i; j < n; j++) {
        if (id.equals(occupants[j])) {
          deepest = j;
          if (placed[j]) {
            keep = j;
          }
        }
      }
      if (keep < 0) {
        keep = deepest;
      }
      for (int j = i; j < n; j++) {
        if (j != keep && id.equals(occupants[j])) {
          clear[j] = true;
        }
      }
    }
    return clear;
  }
}
