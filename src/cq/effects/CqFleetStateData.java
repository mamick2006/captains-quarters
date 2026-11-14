package cq.effects;

import cq.effects.pure.CqFleetStateMath;

public class CqFleetStateData {

  private boolean dark;
  private long darkSince;

  void noteDark(boolean activeNow, long now) {
    darkSince = CqFleetStateMath.darkSince(activeNow, dark, darkSince, now);
    dark = activeNow;
  }

  public boolean isDark() {
    return dark;
  }

  public long getDarkSince() {
    return darkSince;
  }
}
