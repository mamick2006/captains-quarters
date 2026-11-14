package cq.effects.pure;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CqEffectScale {

  private static final Set<String> ROLLED_KINDS = Collections.unmodifiableSet(
      new LinkedHashSet<String>(Arrays.asList("RECOVERY_DMODS", "RELATIONSHIP",
          "NEW_CONTACT_REL", "COMMAND_POINTS", "DMOD_REPAIRED", "MOVE_SLOW_BONUS",
          "DMOD_REDUCTION")));

  private static final Set<String> ROUNDED_UP_KINDS = Collections.unmodifiableSet(
      new LinkedHashSet<String>(Arrays.asList("MAX_CONTACTS")));

  private static final float ROUND_UP_EPS = 1e-4f;

  private static final Set<String> ROUNDED_KINDS = Collections.unmodifiableSet(
      new LinkedHashSet<String>(Arrays.asList("MONTHLY_YIELD", "RECOVERY_CARGO")));

  private static final Set<String> WHOLE_NUMBER_KINDS;

  static {
    Set<String> all = new LinkedHashSet<String>(ROUNDED_KINDS);
    all.addAll(ROLLED_KINDS);
    all.addAll(ROUNDED_UP_KINDS);
    WHOLE_NUMBER_KINDS = Collections.unmodifiableSet(all);
  }

  private CqEffectScale() {
  }

  public static Set<String> wholeNumberKinds() {
    return WHOLE_NUMBER_KINDS;
  }

  public static Set<String> roundedKinds() {
    return ROUNDED_KINDS;
  }

  public static Set<String> roundedUpKinds() {
    return ROUNDED_UP_KINDS;
  }

  public static Set<String> rolledKinds() {
    return ROLLED_KINDS;
  }

  public static boolean wholeNumber(String kind) {
    return kind != null && WHOLE_NUMBER_KINDS.contains(kind);
  }

  public static boolean rolled(String kind) {
    return kind != null && ROLLED_KINDS.contains(kind);
  }

  public static float mag(String kind, float raw, float mult) {
    float v = raw * mult;
    if (kind != null && ROUNDED_KINDS.contains(kind)) {
      v = Math.round(v);
    } else if (kind != null && ROUNDED_UP_KINDS.contains(kind)) {
      v = (float) Math.ceil(v - ROUND_UP_EPS);
    }
    return kind == null ? v : CqEffectLimits.mag(kind, v);
  }

  public static float pct(String kind, float raw, float mult) {
    float v = raw * mult;
    return kind == null ? v : CqEffectLimits.pct(kind, v);
  }
}
