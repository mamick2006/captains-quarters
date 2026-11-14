package cq.effects.pure;

public final class CqCombatWindows {

  private CqCombatWindows() {
  }

  public static final float OPENING_SECONDS = 60f;
  public static final float AFTER_OVERLOAD_SECONDS = 30f;
  public static final float PEAK_POLL_SECONDS = 0.5f;
  public static final float NEVER = -1f;
  public static final float TIME_FLOW_MIN = 0.1f;

  public static final int OPENING = 1;
  public static final int AFTER_OVERLOAD = 2;
  public static final int OUTNUMBERED = 4;
  public static final int PEAK_TIME_OUT = 8;
  public static final int ALWAYS = 16;

  public static int bit(CqWhen when) {
    if (when == null) {
      return ALWAYS;
    }
    switch (when) {
      case BATTLE_OPENING:
        return OPENING;
      case AFTER_OVERLOAD:
        return AFTER_OVERLOAD;
      case OUTNUMBERED:
        return OUTNUMBERED;
      case PEAK_TIME_OUT:
        return PEAK_TIME_OUT;
      default:
        return 0;
    }
  }

  public static float left(float openedAt, float length, float now) {
    if (openedAt < 0f || now < openedAt) {
      return 0f;
    }
    float left = length - (now - openedAt);
    return left > 0f ? left : 0f;
  }

  public static boolean open(float openedAt, float length, float now) {
    return left(openedAt, length, now) > 0f;
  }

  public static boolean stalled(boolean overloaded, boolean venting) {
    return overloaded || venting;
  }

  public static float overloadEndedAt(boolean wasOverloaded, boolean overloaded, float endedAt,
      float now) {
    return wasOverloaded && !overloaded ? now : endedAt;
  }

  public static int sidePoints(int[] fleetPoints) {
    int sum = 0;
    for (int points : fleetPoints) {
      sum += points;
    }
    return sum;
  }

  public static boolean outnumbered(int[] playerSide, int[] enemySide) {
    if (playerSide == null || enemySide == null) {
      return false;
    }
    return sidePoints(enemySide) > sidePoints(playerSide);
  }

  public static boolean peakTimeOut(boolean losesReadinessInCombat, float peakTimeRemaining) {
    return losesReadinessInCombat && peakTimeRemaining <= 0f;
  }

  public static int holding(boolean piloted, float enteredAt, float overloadEndedAt,
      boolean outnumbered, boolean peakTimeOut, float now) {
    if (!piloted) {
      return 0;
    }
    int bits = ALWAYS;
    if (open(enteredAt, OPENING_SECONDS, now)) {
      bits |= OPENING;
    }
    if (open(overloadEndedAt, AFTER_OVERLOAD_SECONDS, now)) {
      bits |= AFTER_OVERLOAD;
    }
    if (outnumbered) {
      bits |= OUTNUMBERED;
    }
    if (peakTimeOut) {
      bits |= PEAK_TIME_OUT;
    }
    return bits;
  }

  public static float nextChange(float enteredAt, float overloadEndedAt, boolean readsPeakTime,
      float now) {
    float next = Float.POSITIVE_INFINITY;
    if (open(enteredAt, OPENING_SECONDS, now)) {
      next = Math.min(next, enteredAt + OPENING_SECONDS);
    }
    if (open(overloadEndedAt, AFTER_OVERLOAD_SECONDS, now)) {
      next = Math.min(next, overloadEndedAt + AFTER_OVERLOAD_SECONDS);
    }
    if (readsPeakTime) {
      next = Math.min(next, now + PEAK_POLL_SECONDS);
    }
    return next;
  }

  public static float timeFlowMult(float percentFaster) {
    return Math.max(TIME_FLOW_MIN, 1f + percentFaster / 100f);
  }
}
