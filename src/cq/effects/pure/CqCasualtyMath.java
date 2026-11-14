package cq.effects.pure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CqCasualtyMath {

  public static final String ARG_HYPERSPACE = "hyperspace";
  public static final String ARG_NO_SHIP_DESTROYED = "no_ship_destroyed";

  public enum Event {
    BATTLE_WON,
    BATTLE_LOST,
    RAID
  }

  private CqCasualtyMath() {
  }

  public static boolean pays(CqWhen when, String arg, Event event, boolean inHyperspace) {
    return pays(when, arg, event, inHyperspace, false);
  }

  public static boolean pays(CqWhen when, String arg, Event event, boolean inHyperspace,
      boolean shipDestroyed) {
    if (event == null) {
      return false;
    }
    if (arg != null && !arg.isEmpty()) {
      if (ARG_NO_SHIP_DESTROYED.equals(arg)) {
        if (shipDestroyed) {
          return false;
        }
      } else if (!ARG_HYPERSPACE.equals(arg) || !inHyperspace) {
        return false;
      }
    }
    if (when == null) {
      return true;
    }
    switch (when) {
      case AFTER_BATTLE:
        return event != Event.RAID;
      case AFTER_BATTLE_WON:
        return event == Event.BATTLE_WON;
      case AFTER_BATTLE_LOST:
        return event == Event.BATTLE_LOST;
      case AFTER_RAID:
        return event == Event.RAID;
      default:
        return false;
    }
  }

  public static int lost(int before, int after) {
    return Math.max(0, before - after);
  }

  public static float share(int lost, float pct, float cap) {
    if (lost <= 0 || pct <= 0f || cap <= 0f) {
      return 0f;
    }
    return Math.min(lost * pct / 100f, cap);
  }

  public static int returned(int lost, float shares) {
    if (lost <= 0 || shares <= 0f) {
      return 0;
    }
    return Math.min(lost, (int) Math.floor(shares + 0.5f));
  }

  public static int returned(int lost, float[] pcts, float[] caps) {
    float shares = 0f;
    for (int i = 0; i < pcts.length; i++) {
      shares += share(lost, pcts[i], caps[i]);
    }
    return returned(lost, shares);
  }

  public static float crCost(float before, float after) {
    return Math.max(0f, before - after);
  }

  public static float crRegained(float before, float after, float pct) {
    if (pct <= 0f) {
      return 0f;
    }
    return crCost(before, after) * Math.min(pct, CqEffectLimits.WHOLE) / 100f;
  }

  public static final class Encounter {

    private int engagements;
    private int crewBefore;
    private int marinesBefore;
    private boolean hyperspace;
    private boolean won;
    private boolean shipDestroyed;
    private Map<String, Float> pilotedCr;

    public void engagement(int crew, int marines, boolean inHyperspace, boolean playerWon) {
      if (engagements == 0) {
        crewBefore = crew;
        marinesBefore = marines;
        hyperspace = inHyperspace;
      }
      engagements++;
      won = playerWon;
    }

    public void down(boolean own, boolean destroyed) {
      if (own && destroyed) {
        shipDestroyed = true;
      }
    }

    public boolean shipDestroyed() {
      return shipDestroyed;
    }

    public void piloted(String shipId, float cr) {
      if (shipId == null) {
        return;
      }
      if (pilotedCr == null) {
        pilotedCr = new LinkedHashMap<String, Float>();
      }
      if (!pilotedCr.containsKey(shipId)) {
        pilotedCr.put(shipId, Float.valueOf(cr));
      }
    }

    public int engagements() {
      return engagements;
    }

    public int crewLost(int crewNow) {
      return lost(crewBefore, crewNow);
    }

    public int marinesLost(int marinesNow) {
      return lost(marinesBefore, marinesNow);
    }

    public boolean inHyperspace() {
      return hyperspace;
    }

    public Event event() {
      return won ? Event.BATTLE_WON : Event.BATTLE_LOST;
    }

    public Map<String, Float> pilotedCr() {
      if (pilotedCr == null) {
        return Collections.<String, Float>emptyMap();
      }
      return Collections.unmodifiableMap(pilotedCr);
    }
  }
}
