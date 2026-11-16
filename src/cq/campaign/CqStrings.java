package cq.campaign;

import com.fs.starfarer.api.Global;

public final class CqStrings {

  private CqStrings() {
  }

  public static String effects(String key) {
    return get("cq_effects", key);
  }

  public static String effects(String key, Object... args) {
    return fill("cq_effects", key, args);
  }

  public static String items(String key) {
    return get("cq_items", key);
  }

  public static String mgmt(String key) {
    return get("cq_mgmt", key);
  }

  public static String intel(String key) {
    return get("cq_intel", key);
  }

  public static String console(String key) {
    return get("cq_console", key);
  }

  public static String fleet(String key) {
    return get("cq_fleet", key);
  }

  private static String get(String category, String key) {
    String s;
    try {
      s = Global.getSettings().getString(category, key);
    } catch (RuntimeException e) {
      throw new IllegalStateException(missing(category, key), e);
    }
    if (s == null) {
      throw new IllegalStateException(missing(category, key));
    }
    return s;
  }

  private static String missing(String category, String key) {
    return "Captain's Quarters: missing string " + category + ":" + key
        + " (data/strings/strings.json)";
  }

  private static String fill(String category, String key, Object[] args) {
    String template = get(category, key);
    int slots = 0;
    for (int at = template.indexOf("%s"); at >= 0; at = template.indexOf("%s", at + 2)) {
      slots++;
    }
    if (slots != args.length) {
      throw new IllegalStateException("Captain's Quarters: strings.json entry " + category
          + ":" + key + " has " + slots + " %s slot(s) but " + args.length + " arg(s)");
    }
    StringBuilder out = new StringBuilder(template.length() + 32);
    int from = 0;
    for (Object arg : args) {
      int at = template.indexOf("%s", from);
      out.append(template, from, at).append(arg);
      from = at + 2;
    }
    out.append(template, from, template.length());
    return out.toString();
  }
}
