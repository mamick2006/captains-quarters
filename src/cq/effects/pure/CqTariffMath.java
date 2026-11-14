package cq.effects.pure;

public final class CqTariffMath {

  public static final String LUDDIC_MAJORITY = "luddic_majority";
  public static final String INDEPENDENT = "independent";
  public static final String MILITARY_BASE = "military_base";
  public static final String PERSEAN_LEAGUE = "persean_league";
  public static final String COMMISSION = "commission";
  public static final String HEAVY_INDUSTRY = "heavy_industry";
  public static final String LUDDIC_CHURCH = "luddic_church";
  public static final String HEGEMONY = "hegemony";

  private static final String[] GROUPS = {LUDDIC_MAJORITY, INDEPENDENT, MILITARY_BASE,
      PERSEAN_LEAGUE, COMMISSION, HEAVY_INDUSTRY, LUDDIC_CHURCH, HEGEMONY};

  public static final int MONTHS_PER_CYCLE = 12;

  private static final double ROUNDING_GUARD = 1e-6;

  private CqTariffMath() {
  }

  public static boolean isNamedGroup(String arg) {
    for (String group : GROUPS) {
      if (group.equals(arg)) {
        return true;
      }
    }
    return false;
  }

  public static int monthIndex(int cycle, int month) {
    return cycle * MONTHS_PER_CYCLE + month;
  }

  public static boolean live(float pct, float cap, long returned) {
    return pct > 0f && (double) cap - returned >= 1.0;
  }

  public static long[] rebate(float paid, float[] pcts, float[] caps, long[] returned) {
    return rebate(paid, pcts, caps, returned, CqEffectLimits.TARIFF_CUT_MAX);
  }

  public static boolean stopped(float paid, float[] pcts, float[] caps, long[] returned) {
    return total(rebate(paid, pcts, caps, returned, Double.POSITIVE_INFINITY))
        > total(rebate(paid, pcts, caps, returned));
  }

  public static String limitPercent() {
    return String.valueOf(Math.round(CqEffectLimits.TARIFF_CUT_MAX));
  }

  private static long[] rebate(float paid, float[] pcts, float[] caps, long[] returned,
      double limit) {
    int n = pcts.length;
    long[] parts = new long[n];
    if (!(paid > 0f)) {
      return parts;
    }
    double sum = 0.0;
    for (int i = 0; i < n; i++) {
      if (live(pcts[i], caps[i], returned[i])) {
        sum += pcts[i];
      }
    }
    if (sum <= 0.0) {
      return parts;
    }
    double limited = Math.min(sum, limit);
    for (int i = 0; i < n; i++) {
      if (live(pcts[i], caps[i], returned[i])) {
        double share = (double) paid * limited * pcts[i] / (sum * 100.0);
        double owed = Math.min(share, (double) caps[i] - returned[i]);
        parts[i] = (long) Math.floor(owed + ROUNDING_GUARD);
      }
    }
    return parts;
  }

  public static long total(long[] parts) {
    long sum = 0L;
    for (long part : parts) {
      sum += part;
    }
    return sum;
  }

  public static float tariffInBuyPrice(float paid, float rate) {
    if (paid <= 0f || rate <= 0f) {
      return 0f;
    }
    return paid * rate / (1f + rate);
  }

  public static float tariffInSellPrice(float received, float rate) {
    if (received <= 0f || rate <= 0f || rate >= 1f) {
      return 0f;
    }
    return received * rate / (1f - rate);
  }
}
