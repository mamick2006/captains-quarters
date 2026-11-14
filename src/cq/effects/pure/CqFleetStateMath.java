package cq.effects.pure;

public final class CqFleetStateMath {

  private CqFleetStateMath() {
  }

  public static final float DARK_DAY_DAYS = 1.0f;

  public static final float STOPPED_SPEED = 10f;

  public static final float MARKET_RECOUNT_DAYS = 1.0f;

  public static long darkSince(boolean activeNow, boolean wasDark, long prevStamp, long now) {
    if (activeNow && !wasDark) {
      return now;
    }
    return prevStamp;
  }

  public static boolean darkDay(boolean activeNow, boolean dark, float daysSinceStamp) {
    return activeNow && dark && daysSinceStamp >= DARK_DAY_DAYS;
  }

  public static boolean stopped(float speed) {
    return speed < STOPPED_SPEED;
  }

  public static boolean noMarketSystem(boolean inHyperspace, int visibleMarkets) {
    return !inHyperspace && visibleMarkets == 0;
  }

  public static boolean sameLocation(String countedId, String currentId) {
    return countedId != null && countedId.equals(currentId);
  }

  public static boolean recountMarkets(boolean sameLocation, float daysSinceCount) {
    return !sameLocation || daysSinceCount >= MARKET_RECOUNT_DAYS;
  }
}
