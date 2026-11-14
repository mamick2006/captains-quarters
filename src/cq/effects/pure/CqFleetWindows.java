package cq.effects.pure;

public final class CqFleetWindows {

  public static final float SUPPLIES_SHORT_DAYS = 10f;
  public static final float CREW_SHORT_SHARE = 0.75f;

  public static final float RECENTLY_DOCKED_DAYS = 2f;

  private CqFleetWindows() {
  }

  public static boolean suppliesShort(float supplies, float perDay) {
    return supplies < perDay * SUPPLIES_SHORT_DAYS;
  }

  public static boolean crewShort(float crew, float needed) {
    return crew < needed * CREW_SHORT_SHARE;
  }

  public static boolean windowOpen(boolean stamped, float daysSince, float windowDays) {
    return stamped && daysSince <= windowDays;
  }

  public static boolean opensWindow(boolean stamped, float daysSince, float windowDays) {
    return !windowOpen(stamped, daysSince, windowDays);
  }

  public static boolean isDocking(boolean conditionsOnly, boolean inEconomy,
      boolean neutralFaction, boolean refused) {
    return !conditionsOnly && inEconomy && !neutralFaction && !refused;
  }
}
