package cq.effects.pure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CqRecoveryMath {

  public static final String GROUP_LOW_TECH = "low_tech";
  public static final String GROUP_MIDLINE = "midline";
  public static final String GROUP_PIRATE = "pirate";
  public static final String GROUP_DESTROYER = "destroyer";
  public static final String GROUP_CAPITAL = "capital";
  public static final String GROUP_CRUISER = "cruiser";
  public static final String GROUP_HIGH_TECH = "high_tech";
  public static final String GROUP_CIVILIAN = "civilian";
  public static final String GROUP_DERELICT = "derelict";
  public static final String GROUP_DMODS_3 = "dmods_3";

  public static final String MANUFACTURER_LOW_TECH = "Low Tech";
  public static final String MANUFACTURER_MIDLINE = "Midline";
  public static final String MANUFACTURER_PIRATE = "Pirate";
  public static final String MANUFACTURER_HIGH_TECH = "High Tech";

  public static final String SIZE_DESTROYER = "DESTROYER";
  public static final String SIZE_CRUISER = "CRUISER";
  public static final String SIZE_CAPITAL = "CAPITAL_SHIP";

  public static final int DMODS_3_MIN = 3;

  public static final float HULL_MAX = 1f;

  public static final int MIN_DMODS_LEFT = 1;

  public static final float RECOVERY_CARGO_DAYS = 30f;

  private CqRecoveryMath() {
  }

  public static final class Ship {
    public final String manufacturer;
    public final String hullSize;
    public final boolean civilian;
    public final boolean fromDerelict;
    public final int dmods;
    public final boolean hullUnrestorable;
    public final boolean variantUnrestorable;

    public Ship(String manufacturer, String hullSize, boolean civilian, boolean fromDerelict,
        int dmods) {
      this(manufacturer, hullSize, civilian, fromDerelict, dmods, false, false);
    }

    public Ship(String manufacturer, String hullSize, boolean civilian, boolean fromDerelict,
        int dmods, boolean hullUnrestorable, boolean variantUnrestorable) {
      this.manufacturer = manufacturer;
      this.hullSize = hullSize;
      this.civilian = civilian;
      this.fromDerelict = fromDerelict;
      this.dmods = dmods;
      this.hullUnrestorable = hullUnrestorable;
      this.variantUnrestorable = variantUnrestorable;
    }

    public boolean restorable() {
      return !hullUnrestorable && !variantUnrestorable;
    }
  }

  public static final class Row {
    public final String group;
    public final float hullPoints;
    public final float crPoints;
    public final int dmods;

    public Row(String group, float hullPoints, float crPoints, int dmods) {
      this.group = group;
      this.hullPoints = hullPoints;
      this.crPoints = crPoints;
      this.dmods = dmods;
    }
  }

  public static final class Benefit {
    public final float hullPoints;
    public final float crPoints;
    public final int dmods;

    Benefit(float hullPoints, float crPoints, int dmods) {
      this.hullPoints = hullPoints;
      this.crPoints = crPoints;
      this.dmods = dmods;
    }

    public boolean isNone() {
      return hullPoints <= 0f && crPoints <= 0f && dmods <= 0;
    }
  }

  public static boolean knownGroup(String group) {
    return GROUP_LOW_TECH.equals(group) || GROUP_MIDLINE.equals(group)
        || GROUP_PIRATE.equals(group) || GROUP_DESTROYER.equals(group)
        || GROUP_CAPITAL.equals(group) || GROUP_CIVILIAN.equals(group)
        || GROUP_DERELICT.equals(group) || GROUP_DMODS_3.equals(group)
        || GROUP_CRUISER.equals(group) || GROUP_HIGH_TECH.equals(group);
  }

  public static boolean inGroup(String group, Ship ship) {
    if (group == null || ship == null) {
      return false;
    }
    if (GROUP_LOW_TECH.equals(group)) {
      return madeBy(ship, MANUFACTURER_LOW_TECH);
    }
    if (GROUP_MIDLINE.equals(group)) {
      return madeBy(ship, MANUFACTURER_MIDLINE);
    }
    if (GROUP_PIRATE.equals(group)) {
      return madeBy(ship, MANUFACTURER_PIRATE);
    }
    if (GROUP_DESTROYER.equals(group)) {
      return SIZE_DESTROYER.equals(ship.hullSize);
    }
    if (GROUP_CAPITAL.equals(group)) {
      return SIZE_CAPITAL.equals(ship.hullSize);
    }
    if (GROUP_CRUISER.equals(group)) {
      return SIZE_CRUISER.equals(ship.hullSize);
    }
    if (GROUP_HIGH_TECH.equals(group)) {
      return madeBy(ship, MANUFACTURER_HIGH_TECH);
    }
    if (GROUP_CIVILIAN.equals(group)) {
      return ship.civilian;
    }
    if (GROUP_DERELICT.equals(group)) {
      return ship.fromDerelict;
    }
    if (GROUP_DMODS_3.equals(group)) {
      return ship.dmods >= DMODS_3_MIN;
    }
    return false;
  }

  private static boolean madeBy(Ship ship, String manufacturer) {
    return ship.manufacturer != null && manufacturer.equalsIgnoreCase(ship.manufacturer.trim());
  }

  public static Benefit total(Ship ship, List<Row> rows) {
    float hull = 0f;
    float cr = 0f;
    int dmods = 0;
    for (Row row : rows) {
      if (inGroup(row.group, ship)) {
        hull += row.hullPoints;
        cr += row.crPoints;
        dmods += row.dmods;
      }
    }
    return new Benefit(hull, cr, ship.restorable() ? dmods : 0);
  }

  public static float raised(float current, float points, float max) {
    if (points <= 0f) {
      return current;
    }
    return Math.max(current, Math.min(max, current + points / 100f));
  }

  public static List<String> dmodsToRemove(Collection<String> dmods, Collection<String> builtIn,
      int fewer) {
    return dmodsToRemove(dmods, builtIn, fewer, MIN_DMODS_LEFT);
  }

  public static List<String> dmodsToRemove(Collection<String> dmods, Collection<String> builtIn,
      int fewer, int minLeft) {
    List<String> onShip = new ArrayList<String>();
    List<String> removable = new ArrayList<String>();
    for (String id : dmods) {
      if (id == null || onShip.contains(id)) {
        continue;
      }
      onShip.add(id);
      if (!builtIn.contains(id)) {
        removable.add(id);
      }
    }
    Collections.sort(removable);
    int spare = onShip.size() - Math.max(0, minLeft);
    int n = Math.max(0, Math.min(fewer, Math.min(removable.size(), spare)));
    return new ArrayList<String>(removable.subList(0, n));
  }

  public static String cargoGroup(String arg) {
    int colon = arg == null ? -1 : arg.indexOf(':');
    return colon < 0 ? "" : arg.substring(0, colon).trim();
  }

  public static String cargoCommodity(String arg) {
    int colon = arg == null ? -1 : arg.indexOf(':');
    return colon < 0 ? "" : arg.substring(colon + 1).trim();
  }

  public static boolean cargoArg(String arg) {
    return knownGroup(cargoGroup(arg)) && !cargoCommodity(arg).isEmpty();
  }

  public static final class CargoRow {
    public final String id;
    public final String group;
    public final String commodity;
    public final int units;

    public CargoRow(String id, String group, String commodity, int units) {
      this.id = id;
      this.group = group;
      this.commodity = commodity;
      this.units = units;
    }
  }

  public interface CargoClock {
    Float daysSincePaid(String rowId);

    void paid(String rowId);
  }

  public static boolean cargoDue(Float daysSince) {
    return daysSince == null || daysSince.floatValue() >= RECOVERY_CARGO_DAYS;
  }

  public static List<CargoRow> cargo(Ship ship, List<CargoRow> rows, CargoClock clock) {
    List<CargoRow> paid = new ArrayList<CargoRow>();
    for (CargoRow row : rows) {
      if (row.units > 0 && inGroup(row.group, ship) && cargoDue(clock.daysSincePaid(row.id))) {
        clock.paid(row.id);
        paid.add(row);
      }
    }
    return paid;
  }

  public static final class Ledger {
    private final Set<String> settled = new HashSet<String>();

    public void noteMembers(Collection<String> memberIds) {
      for (String id : memberIds) {
        if (id != null) {
          settled.add(id);
        }
      }
    }

    public boolean claim(String memberId) {
      return memberId != null && settled.add(memberId);
    }

    public List<String> claimAll(Collection<String> memberIds) {
      List<String> due = new ArrayList<String>();
      for (String id : memberIds) {
        if (claim(id)) {
          due.add(id);
        }
      }
      return due;
    }
  }
}
