package cq.effects.pure;

public final class CqRoll {

  private static final float WHOLE_EPS = 1e-4f;

  private CqRoll() {
  }

  public static float unit(long seed) {
    long z = seed + 0x9E3779B97F4A7C15L;
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    z = z ^ (z >>> 31);
    return (z >>> 40) / (float) (1 << 24);
  }

  public static long seed(String... parts) {
    long h = 0xCBF29CE484222325L;
    for (int p = 0; p < parts.length; p++) {
      if (p > 0) {
        h = (h ^ '|') * 0x100000001B3L;
      }
      String part = parts[p] == null ? "" : parts[p];
      for (int i = 0; i < part.length(); i++) {
        h = (h ^ part.charAt(i)) * 0x100000001B3L;
      }
    }
    return h;
  }

  public static int pay(float value, float u) {
    float v = snapped(value);
    if (!(v > 0f)) {
      return 0;
    }
    int whole = (int) Math.floor(v);
    return whole + (u < v - whole ? 1 : 0);
  }

  public static boolean fractional(float value) {
    float v = snapped(value);
    return v > 0f && v != Math.floor(v);
  }

  public static String range(float value) {
    float v = snapped(value);
    if (!fractional(v)) {
      return CqEffectLine.num(v);
    }
    return CqEffectLine.num((float) Math.floor(v)) + "-" + CqEffectLine.num((float) Math.ceil(v));
  }

  private static float snapped(float value) {
    float r = Math.round(value);
    return Math.abs(value - r) < WHOLE_EPS ? r : value;
  }
}
