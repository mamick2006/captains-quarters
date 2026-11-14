package cq.effects.pure;

public final class CqEffectLimits {

  public static final float CUT_MAX = 90f;
  public static final float TARIFF_CUT_MAX = 60f;
  public static final float WHOLE = 100f;

  private CqEffectLimits() {
  }

  public static float mag(String kind, float value) {
    if (kind == null) {
      return value;
    }
    switch (kind) {
      case "SHIELD_DAMAGE":
      case "ARMOR_DAMAGE_TAKEN":
      case "EMP_TAKEN":
      case "DETECTED_AT":
      case "GO_DARK_DETECT":
        return Math.min(value, CUT_MAX);
      case "EBURN_CR_COST":
      case "CR_REGAINED":
        return Math.min(value, WHOLE);
      default:
        return value;
    }
  }

  public static float pct(String kind, float value) {
    if (kind == null) {
      return value;
    }
    switch (kind) {
      case "TARIFF_CAPPED":
        return Math.min(value, TARIFF_CUT_MAX);
      case "CREW_RETURNED":
      case "MARINES_RETURNED":
        return Math.min(value, WHOLE);
      default:
        return value;
    }
  }
}
