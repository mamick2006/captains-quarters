package cq.newgame.pure;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CqStartRules {

  private CqStartRules() {
  }

  public static final int LONE_BUDGET = 40000;
  public static final int SMALL_BUDGET = 44000;
  public static final int SMALL_OFFICER_BUDGET = 40000;
  public static final String LONE_BUDGET_KEY = "cq_fleet_budget_one_ship";
  public static final String SMALL_BUDGET_KEY = "cq_fleet_budget_small_fleet";

  public static final int OFFICER_COST = SMALL_BUDGET - SMALL_OFFICER_BUDGET;

  public static final int BUDGET_MIN = 10000;
  public static final int BUDGET_MAX = 1000000;

  public static final class Budgets {
    public static final Budgets DEFAULTS = new Budgets(LONE_BUDGET, SMALL_BUDGET);

    public final int lone;
    public final int small;

    private Budgets(int lone, int small) {
      this.lone = lone;
      this.small = small;
    }

    public static Budgets of(Number lone, Number small) {
      return new Budgets(sane(lone, LONE_BUDGET), sane(small, SMALL_BUDGET));
    }

    public static int sane(Number raw, int fallback) {
      if (raw == null) {
        return fallback;
      }
      double v = raw.doubleValue();
      if (Double.isNaN(v) || Double.isInfinite(v)) {
        return fallback;
      }
      if (v < BUDGET_MIN) {
        return BUDGET_MIN;
      }
      if (v > BUDGET_MAX) {
        return BUDGET_MAX;
      }
      return (int) Math.round(v);
    }

    public int officer() {
      return small - OFFICER_COST;
    }

    public int budget(Flavour f, boolean officer) {
      if (f == Flavour.LONE) {
        return lone;
      }
      return officer ? officer() : small;
    }
  }

  public static final String FRIGATE = "FRIGATE";
  public static final String DESTROYER = "DESTROYER";

  public enum Flavour {
    LONE(1, 0.6f, -1, false),
    SMALL(3, 0.2f, 1, true);

    public final int maxShips;
    public final float quality;
    public final int maxTier;
    public final boolean officerAllowed;

    Flavour(int maxShips, float quality, int maxTier, boolean officerAllowed) {
      this.maxShips = maxShips;
      this.quality = quality;
      this.maxTier = maxTier;
      this.officerAllowed = officerAllowed;
    }

    public int budget(boolean officer) {
      return Budgets.DEFAULTS.budget(this, officer);
    }

    public boolean allowsSize(String size) {
      if (FRIGATE.equals(size)) {
        return true;
      }
      return this == LONE && DESTROYER.equals(size);
    }
  }

  public static final class Hull {
    public final String id;
    public final String size;
    public final int baseValue;
    public final Set<String> tags;
    public final Set<String> hints;
    public final Set<String> builtInMods;
    public final boolean defaultDHull;
    public final boolean dHull;
    public final Set<String> baseHullTags;

    public Hull(String id, String size, int baseValue, Set<String> tags, Set<String> hints,
        Set<String> builtInMods, boolean defaultDHull, boolean dHull, Set<String> baseHullTags) {
      this.id = id;
      this.size = size;
      this.baseValue = baseValue;
      this.tags = tags == null ? Collections.<String>emptySet() : tags;
      this.hints = hints == null ? Collections.<String>emptySet() : hints;
      this.builtInMods = builtInMods == null ? Collections.<String>emptySet() : builtInMods;
      this.defaultDHull = defaultDHull;
      this.dHull = dHull;
      this.baseHullTags = baseHullTags;
    }
  }

  public static String excluded(Hull h) {
    if (h == null || h.id == null) {
      return "no hull";
    }
    if ("FIGHTER".equals(h.size) && !h.tags.contains("show_in_codex_as_ship")) {
      return "fighter";
    }
    if (h.hints.contains("HIDE_IN_CODEX") || h.tags.contains("hide_in_codex")
        || h.tags.contains("invisible_in_codex")) {
      return "hidden from the Codex";
    }
    if (h.defaultDHull || "shuttlepod".equals(h.id)) {
      return "not in the Codex";
    }
    if (h.tags.contains("codex_unlockable")) {
      return "locked in the Codex";
    }
    if (h.baseHullTags != null && h.baseHullTags.contains("codex_unlockable")) {
      return "locked in the Codex";
    }
    if (h.hints.contains("STATION") || h.hints.contains("MODULE") || h.hints.contains("UNBOARDABLE")) {
      return "not a ship";
    }
    if (h.tags.contains("restricted")) {
      return "restricted";
    }
    if (h.builtInMods.contains("automated") || h.tags.contains("automated")) {
      return "automated";
    }
    for (String t : SPECIAL_TAGS) {
      if (h.tags.contains(t)) {
        return "special";
      }
    }
    if (!FRIGATE.equals(h.size) && !DESTROYER.equals(h.size)) {
      return "too large";
    }
    if (h.baseValue <= 0) {
      return "no value";
    }
    return null;
  }

  private static final String[] SPECIAL_TAGS = {
      "no_dealer", "no_sell", "no_dealer_restricted", "no_drop" };

  public static boolean fits(Hull h, Flavour f, boolean officer) {
    return fits(h, f, officer, Budgets.DEFAULTS);
  }

  public static boolean fits(Hull h, Flavour f, boolean officer, Budgets b) {
    return excluded(h) == null && f.allowsSize(h.size) && h.baseValue <= b.budget(f, officer);
  }

  public enum Problem { EMPTY, TOO_MANY, WRONG_SIZE, OVER_BUDGET, OFFICER_NEEDS_SHIP }

  public static Problem problem(Flavour f, boolean officer, List<String> sizes, List<Integer> values) {
    return problem(f, officer, sizes, values, Budgets.DEFAULTS);
  }

  public static Problem problem(Flavour f, boolean officer, List<String> sizes, List<Integer> values,
      Budgets b) {
    if (sizes.isEmpty()) {
      return Problem.EMPTY;
    }
    if (sizes.size() > f.maxShips) {
      return Problem.TOO_MANY;
    }
    for (String s : sizes) {
      if (!f.allowsSize(s)) {
        return Problem.WRONG_SIZE;
      }
    }
    if (total(values) > b.budget(f, officer)) {
      return Problem.OVER_BUDGET;
    }
    if (officer && f.officerAllowed && sizes.size() < 2) {
      return Problem.OFFICER_NEEDS_SHIP;
    }
    return null;
  }

  public static int total(List<Integer> values) {
    int t = 0;
    for (Integer v : values) {
      t += v == null ? 0 : v.intValue();
    }
    return t;
  }

  public static final Map<String, String> VANILLA_VARIANTS;
  static {
    Map<String, String> m = new HashMap<String, String>();
    m.put("shrike", "shrike_Attack");
    m.put("scarab", "scarab_Starting");
    m.put("tempest", "tempest_Starting");
    m.put("shade", "shade_Assault");
    m.put("wolf", "wolf_Starting");
    m.put("lasher", "lasher_Starting");
    m.put("centurion", "centurion_Starting");
    m.put("brawler", "brawler_Starting");
    m.put("wayfarer", "wayfarer_Starting");
    m.put("vigilance", "vigilance_Starting");
    m.put("vanguard", "vanguard_Attack");
    m.put("shepherd", "shepherd_Starting");
    m.put("gremlin", "gremlin_Strike");
    m.put("kite_hegemony", "kite_Starting");
    m.put("hound", "hound_Starting");
    m.put("cerberus", "cerberus_Starting");
    m.put("mudskipper", "mudskipper_Standard");
    m.put("hermes", "hermes_Standard");
    m.put("mercury", "mercury_Standard");
    m.put("mudskipper2", "mudskipper2_Starting");
    VANILLA_VARIANTS = Collections.unmodifiableMap(m);
  }

  public static int startCrew(int minCrew, int maxPersonnel) {
    int crew = minCrew;
    int maxAdd = Math.min(maxPersonnel - crew - 10, 15);
    float add = Math.min(maxAdd, 100f);
    if (add > 0) {
      crew += (int) add;
    }
    return Math.min(crew, maxPersonnel);
  }

  public static float startFuel(float placeholderCapacity, float maxFuel) {
    float fuel = placeholderCapacity * 0.5f;
    float add = Math.min(maxFuel - fuel - 10f, 70f);
    if (add > 0) {
      fuel += add;
    }
    if (fuel > maxFuel) {
      fuel -= (int) (fuel - maxFuel);
    }
    return fuel;
  }

  public static float[] startMachineryAndSupplies(float spaceLeft) {
    float free = spaceLeft;
    float machinery = Math.min(free, 15f);
    if (machinery > 0) {
      free -= machinery;
    }
    float supplies = Math.min(free, 70f);
    return new float[] { Math.max(0f, machinery), Math.max(0f, supplies) };
  }

  public static Set<String> set(String... items) {
    Set<String> s = new HashSet<String>();
    for (String i : items) {
      s.add(i);
    }
    return s;
  }
}
