package cq.effects.pure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CqContactMath {

  public static final float POINT = 0.01f;

  public static final int DEFAULT_PERIOD_DAYS = 30;

  public static final String WHO_CONTACTS = "contacts";
  public static final String WHO_FACTION = "faction";

  public static final String GROUP_TRADE = "trade";
  public static final String GROUP_UNDERWORLD = "underworld";
  public static final String GROUP_MILITARY = "military";

  private static final char ARG_SEPARATOR = ':';

  private CqContactMath() {
  }

  public static String who(String arg) {
    if (arg == null) {
      return "";
    }
    int colon = arg.indexOf(ARG_SEPARATOR);
    return (colon < 0 ? arg : arg.substring(0, colon)).trim();
  }

  public static boolean isContactGroup(String who) {
    return GROUP_TRADE.equals(who) || GROUP_UNDERWORLD.equals(who) || GROUP_MILITARY.equals(who);
  }

  public static boolean namesSomeone(String arg) {
    String who = who(arg);
    return WHO_CONTACTS.equals(who) || WHO_FACTION.equals(who) || isContactGroup(who);
  }

  public static int days(String arg) {
    if (arg == null) {
      return 0;
    }
    int colon = arg.indexOf(ARG_SEPARATOR);
    if (colon < 0) {
      return 0;
    }
    try {
      return Math.max(0, Integer.parseInt(arg.substring(colon + 1).trim()));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public static int periodDays(String arg) {
    int days = days(arg);
    return days > 0 ? days : DEFAULT_PERIOD_DAYS;
  }

  public static boolean ready(boolean stamped, float daysSince, int periodDays) {
    return !stamped || daysSince >= periodDays;
  }

  public static int stacked(int... rowPoints) {
    int total = 0;
    for (int points : rowPoints) {
      if (points > 0) {
        total += points;
      }
    }
    return total;
  }

  public static List<String> payers(List<String> names, int[] points) {
    List<String> result = new ArrayList<String>();
    for (int i = 0; i < points.length && i < names.size(); i++) {
      if (points[i] > 0 && !result.contains(names.get(i))) {
        result.add(names.get(i));
      }
    }
    return result;
  }

  public static int underCap(int current, int gain, int cap) {
    return Math.max(0, Math.min(gain, cap - current));
  }

  public static final int HOSTILE_AT = -50;

  public static boolean hostile(int current) {
    return current <= HOSTILE_AT;
  }

  public static int taken(int current, int gain, int cap) {
    return hostile(current) ? 0 : underCap(current, gain, cap);
  }

  public static boolean keepStamp(int periodDays, float daysSince, boolean targetHeld) {
    return targetHeld && periodDays > 0 && daysSince < periodDays;
  }

  public static List<String> unseen(Collection<String> known, List<String> held) {
    List<String> result = new ArrayList<String>();
    for (String id : held) {
      if (id != null && (known == null || !known.contains(id)) && !result.contains(id)) {
        result.add(id);
      }
    }
    return result;
  }
}
