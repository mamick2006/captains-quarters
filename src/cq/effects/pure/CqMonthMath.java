package cq.effects.pure;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

public final class CqMonthMath {

  private CqMonthMath() {
  }

  public static final double WHOLE_MONTH_TOLERANCE_DAYS = 1.0;

  public static final int BELOW_LEVEL = 3;

  public static final int REPAIR_DMODS_LEFT = 0;

  public static final int REPAIR_CHANCE_DMODS = 1;

  public static final int OFFICERS_PAID = 2;

  public static final double ROUNDING_SLACK = 1e-9;

  public static final double MAG_SNAP = 1e6;

  public enum Mode {
    YIELD,
    DAY,
    REPAIR
  }

  public enum Recipient {
    PLAYER,
    OFFICERS,
    OFFICERS_BELOW_3,
    OFFICER_LOWEST,
    OFFICER_HIGHEST,
    MARINES;

    public static Recipient parse(String arg) {
      if (arg == null || arg.trim().isEmpty()) {
        return null;
      }
      try {
        return valueOf(arg.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
  }

  public static double snap(float mag) {
    return Math.round((double) mag * MAG_SNAP) / MAG_SNAP;
  }

  public static double accrue(Mode mode, double mag, double days) {
    if (mode == null || days <= 0) {
      return 0;
    }
    return mag * days;
  }

  public static boolean restamp(Map<String, Long> since, Collection<String> activeNow, long now) {
    boolean changed = since.keySet().retainAll(activeNow);
    for (String id : activeNow) {
      if (id != null && !since.containsKey(id)) {
        since.put(id, now);
        changed = true;
      }
    }
    return changed;
  }

  public static double creditedDays(double stretchDays, double daysSinceStamp) {
    if (stretchDays <= 0 || daysSinceStamp <= 0) {
      return 0;
    }
    return Math.min(stretchDays, daysSinceStamp);
  }

  public static double accrueAtTick(Mode mode, double mag, double stretchDays,
      double daysSinceStamp) {
    return accrue(mode, mag, creditedDays(stretchDays, daysSinceStamp));
  }

  public static double monthDivisor(double observedDays, double calendarDays) {
    if (observedDays >= calendarDays - WHOLE_MONTH_TOLERANCE_DAYS && observedDays > 0) {
      return observedDays;
    }
    return calendarDays;
  }

  public static double monthAmount(Mode mode, double accrued, double divisor) {
    if (mode == null || accrued <= 0) {
      return 0;
    }
    if (mode == Mode.DAY) {
      return accrued;
    }
    if (divisor <= 0) {
      return 0;
    }
    return accrued / divisor;
  }

  public static long whole(double amount, double carry) {
    double total = amount + carry;
    if (total <= 0) {
      return 0;
    }
    return (long) Math.floor(total + ROUNDING_SLACK);
  }

  public static double carryAfter(double amount, double carry) {
    double rest = amount + carry - whole(amount, carry);
    return rest > 0 ? rest : 0;
  }

  public static double marinePoints(double percent, double marines) {
    if (percent <= 0 || marines <= 0) {
      return 0;
    }
    return percent / 100.0 * marines;
  }

  public static String percentText(double percent) {
    long hundredths = Math.round(percent * 100.0);
    long whole = hundredths / 100;
    long frac = Math.abs(hundredths % 100);
    if (frac == 0) {
      return String.valueOf(whole);
    }
    String digits = frac < 10 ? "0" + frac : String.valueOf(frac);
    if (digits.endsWith("0")) {
      digits = digits.substring(0, 1);
    }
    return whole + "." + digits;
  }

  public static int repairPick(float[] baseValues, int[] spare, boolean[] restorable) {
    if (baseValues == null || spare == null || restorable == null) {
      return -1;
    }
    int n = Math.min(baseValues.length, Math.min(spare.length, restorable.length));
    int best = -1;
    for (int i = 0; i < n; i++) {
      if (!restorable[i] || spare[i] <= 0) {
        continue;
      }
      if (best < 0 || baseValues[i] > baseValues[best]) {
        best = i;
      }
    }
    return best;
  }

  public static int[] pick(Recipient who, int[] levels, int[] maxLevels) {
    if (who == null || levels == null || maxLevels == null) {
      return new int[0];
    }
    int n = Math.min(levels.length, maxLevels.length);
    int[] chosen = new int[n];
    int count = 0;
    switch (who) {
      case OFFICERS:
      case OFFICERS_BELOW_3:
        for (int i = 0; i < n; i++) {
          if (levels[i] < maxLevels[i] && (who == Recipient.OFFICERS || levels[i] < BELOW_LEVEL)) {
            chosen[count++] = i;
          }
        }
        break;
      case OFFICER_LOWEST:
      case OFFICER_HIGHEST:
        boolean[] taken = new boolean[n];
        for (int k = 0; k < OFFICERS_PAID; k++) {
          int best = -1;
          for (int i = 0; i < n; i++) {
            if (taken[i] || levels[i] >= maxLevels[i]) {
              continue;
            }
            if (best < 0 || (who == Recipient.OFFICER_LOWEST ? levels[i] < levels[best]
                : levels[i] > levels[best])) {
              best = i;
            }
          }
          if (best < 0) {
            break;
          }
          taken[best] = true;
        }
        for (int i = 0; i < n; i++) {
          if (taken[i]) {
            chosen[count++] = i;
          }
        }
        break;
      default:
        break;
    }
    int[] out = new int[count];
    System.arraycopy(chosen, 0, out, 0, count);
    return out;
  }
}
