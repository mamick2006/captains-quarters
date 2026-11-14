package cq.effects.pure;

import java.util.Locale;

public enum CqWhen {

  MOVING_SLOWLY(Family.FLEET_STATE),
  SUSTAINED_BURN(Family.FLEET_STATE),
  IN_SLIPSTREAM(Family.FLEET_STATE),
  STOPPED(Family.FLEET_STATE),
  TRANSPONDER_OFF(Family.FLEET_STATE),
  DARK_DAY(Family.FLEET_STATE),
  NO_MARKET_SYSTEM(Family.FLEET_STATE),
  IN_HYPERSPACE(Family.FLEET_STATE),
  RUNNING_DARK(Family.FLEET_STATE),

  SUPPLIES_SHORT(Family.FLEET_WINDOW),
  CREW_SHORT(Family.FLEET_WINDOW),
  RECENTLY_DOCKED(Family.FLEET_WINDOW),

  BATTLE_OPENING(Family.COMBAT),
  AFTER_OVERLOAD(Family.COMBAT),
  OUTNUMBERED(Family.COMBAT),
  PEAK_TIME_OUT(Family.COMBAT),

  AFTER_BATTLE(Family.CASUALTY),
  AFTER_BATTLE_WON(Family.CASUALTY),
  AFTER_BATTLE_LOST(Family.CASUALTY),
  AFTER_RAID(Family.CASUALTY),

  ;

  public enum Family {
    FLEET_STATE,
    FLEET_WINDOW,
    COMBAT,
    TARIFF,
    CASUALTY,
    CONTACT,
    RECOVERY,
    MONTH;

    public boolean isFleet() {
      return this == FLEET_STATE || this == FLEET_WINDOW;
    }
  }

  private final Family family;

  CqWhen(Family family) {
    this.family = family;
  }

  public Family family() {
    return family;
  }

  public String csvName() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static CqWhen parse(String cell) {
    if (cell == null) {
      return null;
    }
    String name = cell.trim();
    if (name.isEmpty()) {
      return null;
    }
    try {
      return valueOf(name.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static String mismatch(CqWhen when, boolean has1, Family home1, boolean piloted1,
      boolean has2, Family home2, boolean piloted2) {
    if (when == null) {
      return null;
    }
    Family f = when.family();
    String name = "when '" + when.csvName() + "'";
    if (f.isFleet()) {
      if ((has1 && home1 != null) || (has2 && home2 != null)) {
        return name + " gates stat effects only, and the row carries an event kind";
      }
      return null;
    }
    if (f == Family.COMBAT) {
      if ((has1 && !combatKind(home1, piloted1)) || (has2 && !combatKind(home2, piloted2))) {
        return name + " needs every kind on the row to be a piloted-ship kind";
      }
      return null;
    }
    boolean anchored = (has1 && home1 == f) || (has2 && home2 == f);
    if (!anchored) {
      return name + " needs a kind that pays on that event";
    }
    if ((has1 && home1 != null && home1 != f) || (has2 && home2 != null && home2 != f)) {
      return name + " shares its row with a kind from another event family";
    }
    return null;
  }

  private static boolean combatKind(Family home, boolean piloted) {
    return home == Family.COMBAT || (home == null && piloted);
  }
}
