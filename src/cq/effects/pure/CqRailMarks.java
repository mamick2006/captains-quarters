package cq.effects.pure;

public final class CqRailMarks {

  public static final float CHECK_SECONDS = 1f;

  private CqRailMarks() {
  }

  public enum Mark {
    NONE,
    ACTIVE,
    INACTIVE
  }

  public static Mark mark(boolean decorativeSlot, CqWhen when, boolean holds) {
    if (decorativeSlot) {
      return Mark.NONE;
    }
    if (when == null || !when.family().isFleet()) {
      return Mark.NONE;
    }
    return holds ? Mark.ACTIVE : Mark.INACTIVE;
  }
}
