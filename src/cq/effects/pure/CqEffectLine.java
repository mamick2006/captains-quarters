package cq.effects.pure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CqEffectLine {

  private static final String[] NO_TOKENS = new String[0];

  public interface Source {
    String effects(String key);
  }

  private final Source effectStrings;

  public CqEffectLine(Source effectStrings) {
    this.effectStrings = effectStrings;
  }

  public String line(String kind, String firstKind, CqWhen when, String arg, String argName,
      float mag, float pct, String stock) {
    String k = kind == null ? "" : kind;
    String a = arg == null ? "" : arg.trim().toLowerCase(Locale.ROOT);
    if (!CqArgs.readable(k, a)) {
      return "";
    }
    switch (k) {
      case "TARIFF_CAPPED": {
        String markets = markets(a);
        if (markets.isEmpty()) {
          return "";
        }
        return fill(effectStrings.effects("effect_tariff_capped"), markets, num(pct), num(mag));
      }
      case "CREW_RETURNED":
        return fill(effectStrings.effects("effect_crew_returned"), event(when, a), num(pct),
            num(mag));
      case "MARINES_RETURNED":
        return fill(effectStrings.effects("effect_marines_returned"), event(when, a), num(pct),
            num(mag));
      case "CR_REGAINED":
        return fill(effectStrings.effects("effect_cr_regained"), event(when, a), num(mag));
      case "RELATIONSHIP":
        return relationship(a, mag);
      case "NEW_CONTACT_REL":
        return fill(effectStrings.effects("effect_new_contact_rel"), count(k, mag));
      case "RECOVERY_HULL":
      case "RECOVERY_CR":
      case "RECOVERY_HULL_CR":
      case "RECOVERY_DMODS":
        return recovery(k, firstKind, a, mag);
      case "RECOVERY_CARGO":
        return cargo(a, argName, mag);
      case "MONTHLY_YIELD": {
        String commodity = commodity(a, argName);
        if (commodity.isEmpty()) {
          return "";
        }
        return fill(effectStrings.effects("effect_monthly_yield"), num(mag), commodity);
      }
      case "EXPERIENCE":
        return experience(a, mag, firstKind);
      case "DMOD_REPAIRED":
        return fill(dmodRepairedTemplate(mag), dmodRepairedNumber(mag));
      default:
        return stat(k, when, mag, stock == null ? "" : stock);
    }
  }

  public static List<String> shown(String... lines) {
    List<String> out = new ArrayList<String>(lines.length);
    for (String line : lines) {
      if (line != null && !line.isEmpty()) {
        out.add(line);
      }
    }
    return out;
  }

  public String[] tokens(String kind, CqWhen when, String arg, String argName, float mag,
      float pct, String[] stockTokens) {
    String k = kind == null ? "" : kind;
    String a = arg == null ? "" : arg.trim().toLowerCase(Locale.ROOT);
    if (!CqArgs.readable(k, a)) {
      return NO_TOKENS;
    }
    switch (k) {
      case "TARIFF_CAPPED": {
        if (markets(a).isEmpty()) {
          return NO_TOKENS;
        }
        String template = effectStrings.effects("effect_tariff_capped");
        return new String[] {token(template, 1, num(pct)), token(template, 2, num(mag))};
      }
      case "CREW_RETURNED": {
        String template = effectStrings.effects("effect_crew_returned");
        return new String[] {token(template, 1, num(pct)), token(template, 2, num(mag))};
      }
      case "MARINES_RETURNED": {
        String template = effectStrings.effects("effect_marines_returned");
        return new String[] {token(template, 1, num(pct)), token(template, 2, num(mag))};
      }
      case "CR_REGAINED":
        return new String[] {token(effectStrings.effects("effect_cr_regained"), 1, num(mag))};
      case "RELATIONSHIP":
        if (!CqContactMath.namesSomeone(a)) {
          return NO_TOKENS;
        }
        return new String[] {token(relationshipTemplate(a), 0, count(k, mag))};
      case "NEW_CONTACT_REL":
        return new String[] {token(effectStrings.effects("effect_new_contact_rel"), 0,
            count(k, mag))};
      case "RECOVERY_HULL":
      case "RECOVERY_CR":
      case "RECOVERY_HULL_CR":
      case "RECOVERY_DMODS":
        if (ships(a).isEmpty()) {
          return NO_TOKENS;
        }
        return new String[] {token(recoveryTemplate(k, mag), 0, count(k, mag))};
      case "RECOVERY_CARGO":
        if (cargo(a, argName, mag).isEmpty()) {
          return NO_TOKENS;
        }
        return new String[] {token(effectStrings.effects("effect_recovery_cargo"), 0, num(mag))};
      case "MONTHLY_YIELD":
        if (commodity(a, argName).isEmpty()) {
          return NO_TOKENS;
        }
        return new String[] {token(effectStrings.effects("effect_monthly_yield"), 0, num(mag))};
      case "EXPERIENCE":
        if ("marines".equals(a)) {
          return new String[] {token(effectStrings.effects("effect_experience_day_marines"), 0,
              num(mag))};
        }
        return new String[] {token(effectStrings.effects("effect_experience_day"), 1,
            num(mag))};
      case "DMOD_REPAIRED":
        return new String[] {token(dmodRepairedTemplate(mag), 0, dmodRepairedNumber(mag))};
      default:
        return statTokens(k, when, mag, stockTokens);
    }
  }

  public static boolean statesOccasion(String kind, CqWhen when) {
    if (kind == null || kind.isEmpty() || when == null || !gates(when) || eventKind(kind)) {
      return false;
    }
    return !stormStrikes(kind, when);
  }

  private static boolean eventKind(String kind) {
    switch (kind) {
      case "TARIFF_CAPPED":
      case "CREW_RETURNED":
      case "MARINES_RETURNED":
      case "CR_REGAINED":
      case "RELATIONSHIP":
      case "NEW_CONTACT_REL":
      case "MONTHLY_YIELD":
      case "EXPERIENCE":
      case "RECOVERY_CARGO":
      case "DMOD_REPAIRED":
        return true;
      default:
        return recoveryKind(kind);
    }
  }

  private static boolean stormStrikes(String kind, CqWhen when) {
    return "CORONA_RESIST".equals(kind) && when == CqWhen.IN_HYPERSPACE;
  }

  private String stat(String kind, CqWhen when, float mag, String stock) {
    if (stormStrikes(kind, when)) {
      return fill(effectStrings.effects("effect_storm_strike_cr"), num(mag));
    }
    String payoff = payoff(kind, mag, stock);
    if ("GO_DARK_DETECT".equals(kind) && when == CqWhen.RUNNING_DARK) {
      return payoff;
    }
    if (when == null || !gates(when) || payoff.isEmpty()) {
      return payoff;
    }
    String note = effectStrings.effects("effect_piloted_note");
    boolean piloted = payoff.endsWith(" " + note);
    String body = piloted ? payoff.substring(0, payoff.length() - note.length() - 1) : payoff;
    if (body.indexOf(',') >= 0) {
      body = body + ",";
    }
    String line = occasion(when, kind, piloted, body);
    return piloted ? line + " " + note : line;
  }

  private String[] statTokens(String kind, CqWhen when, float mag, String[] stockTokens) {
    if (stormStrikes(kind, when)) {
      return new String[] {token(effectStrings.effects("effect_storm_strike_cr"), 0, num(mag))};
    }
    String template = payoffTemplate(kind, mag);
    if (template == null) {
      return stockTokens == null ? NO_TOKENS : stockTokens.clone();
    }
    return template.contains("%s") ? new String[] {token(template, 0, count(kind, mag))}
        : NO_TOKENS;
  }

  private String payoff(String kind, float mag, String stock) {
    String template = payoffTemplate(kind, mag);
    if (template == null) {
      return stock;
    }
    return template.contains("%s") ? fill(template, count(kind, mag)) : template;
  }

  private String payoffTemplate(String kind, float mag) {
    switch (kind) {
      case "ARMOR_DAMAGE_TAKEN":
        return effectStrings.effects("effect_armor_damage_taken");
      case "WEAPON_RANGE":
        return effectStrings.effects("effect_weapon_range");
      case "MAX_CR":
        return effectStrings.effects("effect_max_cr");
      case "TIME_FLOW":
        return effectStrings.effects("effect_time_flow");
      case "RARE_SALVAGE":
        return effectStrings.effects("effect_rare_salvage");
      case "COMMAND_POINTS":
        if (one("COMMAND_POINTS", mag)) {
          return effectStrings.effects("effect_command_points_one");
        }
        return effectStrings.effects("effect_command_points_many");
      case "EBURN_CR_COST":
        if (mag >= CqEffectLimits.WHOLE) {
          return effectStrings.effects("effect_eburn_free");
        }
        return null;
      default:
        return null;
    }
  }

  private static boolean gates(CqWhen when) {
    return when.family().isFleet() || when.family() == CqWhen.Family.COMBAT;
  }

  private static boolean battleOnly(String kind, boolean piloted) {
    return piloted || "COMMAND_POINTS".equals(kind) || "COMMAND_POINT_RATE".equals(kind);
  }

  private String occasion(CqWhen when, String kind, boolean piloted, String body) {
    switch (when) {
      case MOVING_SLOWLY:
        return fill(effectStrings.effects("effect_when_moving_slowly"), body);
      case SUSTAINED_BURN:
        return fill(effectStrings.effects("effect_when_sustained_burn"), body);
      case IN_SLIPSTREAM:
        return fill(effectStrings.effects("effect_when_in_slipstream"), body);
      case STOPPED:
        return fill(effectStrings.effects("effect_when_stopped"), body);
      case TRANSPONDER_OFF:
        return fill(effectStrings.effects("effect_when_transponder_off"), body);
      case DARK_DAY:
        if ("EBURN_CR_COST".equals(kind)) {
          return fill(effectStrings.effects("effect_when_dark_day_eburn"), body);
        }
        if (battleOnly(kind, piloted)) {
          return fill(effectStrings.effects("effect_when_dark_day_battle"), body);
        }
        return fill(effectStrings.effects("effect_when_dark_day"), body);
      case NO_MARKET_SYSTEM:
        if (battleOnly(kind, piloted)) {
          return fill(effectStrings.effects("effect_when_no_market_system_battle"), body);
        }
        return fill(effectStrings.effects("effect_when_no_market_system"), body);
      case IN_HYPERSPACE:
        return fill(effectStrings.effects("effect_when_in_hyperspace"), body);
      case RUNNING_DARK:
        if (battleOnly(kind, piloted)) {
          return fill(effectStrings.effects("effect_when_running_dark_battle"), body);
        }
        return fill(effectStrings.effects("effect_when_running_dark"), body);
      case SUPPLIES_SHORT:
        return fill(effectStrings.effects("effect_when_supplies_short"), body);
      case CREW_SHORT:
        return fill(effectStrings.effects("effect_when_crew_short"), body);
      case RECENTLY_DOCKED:
        return fill(effectStrings.effects("effect_when_recently_docked"), body);
      case BATTLE_OPENING:
        return fill(effectStrings.effects("effect_when_battle_opening"), body);
      case AFTER_OVERLOAD:
        return fill(effectStrings.effects("effect_when_after_overload"), body);
      case OUTNUMBERED:
        return fill(effectStrings.effects("effect_when_outnumbered"), body);
      case PEAK_TIME_OUT:
        return fill(effectStrings.effects("effect_when_peak_time_out"), body);
      default:
        return body;
    }
  }

  private String markets(String arg) {
    switch (arg) {
      case "":
        return "";
      case "luddic_majority":
        return effectStrings.effects("effect_markets_luddic_majority");
      case "independent":
        return effectStrings.effects("effect_markets_independent");
      case "military_base":
        return effectStrings.effects("effect_markets_military_base");
      case "persean_league":
        return effectStrings.effects("effect_markets_persean_league");
      case "commission":
        return effectStrings.effects("effect_markets_commission");
      case "heavy_industry":
        return effectStrings.effects("effect_markets_heavy_industry");
      case "luddic_church":
        return effectStrings.effects("effect_markets_luddic_church");
      case "hegemony":
        return effectStrings.effects("effect_markets_hegemony");
      default:
        return fill(effectStrings.effects("effect_markets_other"), words(arg));
    }
  }

  private String event(CqWhen when, String arg) {
    String event;
    if (when == CqWhen.AFTER_BATTLE_WON) {
      event = effectStrings.effects("effect_after_battle_won");
    } else if (when == CqWhen.AFTER_BATTLE_LOST) {
      event = effectStrings.effects("effect_after_battle_lost");
    } else if (when == CqWhen.AFTER_RAID) {
      event = effectStrings.effects("effect_after_raid");
    } else {
      event = effectStrings.effects("effect_after_battle");
    }
    if ("hyperspace".equals(arg)) {
      return fill(effectStrings.effects("effect_in_hyperspace"), event);
    }
    if (CqCasualtyMath.ARG_NO_SHIP_DESTROYED.equals(arg)
        && (when == null || when == CqWhen.AFTER_BATTLE)) {
      return effectStrings.effects("effect_after_battle_no_ship_destroyed");
    }
    return event;
  }

  private String relationship(String arg, float mag) {
    if (!CqContactMath.namesSomeone(arg)) {
      return "";
    }
    String who = CqContactMath.who(arg);
    String whom;
    String where = effectStrings.effects("effect_rel_at_contact");
    if (CqContactMath.WHO_CONTACTS.equals(who)) {
      whom = effectStrings.effects("effect_rel_all_contacts");
    } else if (CqContactMath.WHO_FACTION.equals(who)) {
      whom = effectStrings.effects("effect_rel_faction");
      where = effectStrings.effects("effect_rel_at_faction");
    } else {
      whom = fill(effectStrings.effects("effect_rel_contacts"), words(who));
    }
    String gain = count("RELATIONSHIP", mag);
    int days = CqContactMath.periodDays(arg);
    if (days == 1) {
      return fill(relationshipTemplate(arg), gain, whom, where);
    }
    return fill(relationshipTemplate(arg), gain, whom, num(days), where);
  }

  private String relationshipTemplate(String arg) {
    if (CqContactMath.periodDays(arg) == 1) {
      return effectStrings.effects("effect_relationship_every_day");
    }
    return effectStrings.effects("effect_relationship_every_days");
  }

  private String experience(String arg, float mag, String firstKind) {
    if ("marines".equals(arg)) {
      return fill(effectStrings.effects("effect_experience_day_marines"), num(mag));
    }
    String who;
    switch (arg) {
      case "officers":
        who = effectStrings.effects("effect_xp_officers");
        break;
      case "officers_below_3":
        who = effectStrings.effects("effect_xp_officers_below_3");
        break;
      case "officer_lowest":
        who = effectStrings.effects("effect_xp_officer_lowest");
        break;
      case "officer_highest":
        who = effectStrings.effects("effect_xp_officer_highest");
        break;
      default:
        who = effectStrings.effects("effect_xp_player");
        break;
    }
    if ("DMOD_REPAIRED".equals(firstKind) && !who.isEmpty()) {
      who = who.substring(0, 1).toLowerCase(Locale.ROOT) + who.substring(1);
    }
    return fill(effectStrings.effects("effect_experience_day"), who, num(mag));
  }

  private String dmodRepairedTemplate(float mag) {
    if (repairChance(mag)) {
      return effectStrings.effects("effect_dmod_repaired_chance");
    }
    if (one("DMOD_REPAIRED", mag)) {
      return effectStrings.effects("effect_dmod_repaired_one");
    }
    return effectStrings.effects("effect_dmod_repaired_many");
  }

  private static String dmodRepairedNumber(float mag) {
    return repairChance(mag) ? num(mag * 100f) : count("DMOD_REPAIRED", mag);
  }

  private static boolean repairChance(float mag) {
    return mag < 1f && CqRoll.fractional(mag);
  }

  public String commodity(String arg, String argName) {
    switch (arg == null ? "" : arg.trim().toLowerCase(Locale.ROOT)) {
      case "domestic_goods":
        return effectStrings.effects("effect_commodity_domestic_goods");
      case "luxury_goods":
        return effectStrings.effects("effect_commodity_luxury_goods");
      case "fuel":
        return effectStrings.effects("effect_commodity_fuel");
      case "lobster":
        return effectStrings.effects("effect_commodity_lobster");
      case "supplies":
        return effectStrings.effects("effect_commodity_supplies");
      case "drugs":
        return effectStrings.effects("effect_commodity_drugs");
      default:
        return argName == null ? "" : argName.trim();
    }
  }

  private static boolean recoveryKind(String kind) {
    return "RECOVERY_HULL".equals(kind) || "RECOVERY_CR".equals(kind)
        || "RECOVERY_HULL_CR".equals(kind) || "RECOVERY_DMODS".equals(kind);
  }

  private String recovery(String kind, String firstKind, String arg, float mag) {
    String ships = ships(arg);
    if (ships.isEmpty()) {
      return "";
    }
    String gets = fill(recoveryTemplate(kind, mag), count(kind, mag));
    if (recoveryKind(firstKind)) {
      return gets;
    }
    return ships + " " + gets;
  }

  private String cargo(String arg, String argName, float mag) {
    String ship = ship(CqRecoveryMath.cargoGroup(arg));
    String commodity = commodity(CqRecoveryMath.cargoCommodity(arg), argName);
    if (ship.isEmpty() || commodity.isEmpty()) {
      return "";
    }
    String gets = fill(effectStrings.effects("effect_recovery_cargo"), num(mag), commodity);
    return fill(effectStrings.effects("effect_cooldown"),
        num(CqRecoveryMath.RECOVERY_CARGO_DAYS), ship + " " + gets);
  }

  private String ship(String arg) {
    switch (arg) {
      case "low_tech":
        return effectStrings.effects("effect_ship_low_tech");
      case "midline":
        return effectStrings.effects("effect_ship_midline");
      case "pirate":
        return effectStrings.effects("effect_ship_pirate");
      case "destroyer":
        return effectStrings.effects("effect_ship_destroyer");
      case "capital":
        return effectStrings.effects("effect_ship_capital");
      case "cruiser":
        return effectStrings.effects("effect_ship_cruiser");
      case "high_tech":
        return effectStrings.effects("effect_ship_high_tech");
      case "civilian":
        return effectStrings.effects("effect_ship_civilian");
      case "derelict":
        return effectStrings.effects("effect_ship_derelict");
      case "dmods_3":
        return effectStrings.effects("effect_ship_dmods_3");
      default:
        return "";
    }
  }

  private String recoveryTemplate(String kind, float mag) {
    if ("RECOVERY_HULL".equals(kind)) {
      return effectStrings.effects("effect_recovery_hull");
    }
    if ("RECOVERY_CR".equals(kind)) {
      return effectStrings.effects("effect_recovery_cr");
    }
    if ("RECOVERY_HULL_CR".equals(kind)) {
      return effectStrings.effects("effect_recovery_hull_cr");
    }
    if (one("RECOVERY_DMODS", mag)) {
      return effectStrings.effects("effect_recovery_dmods_one");
    }
    return effectStrings.effects("effect_recovery_dmods_many");
  }

  private String ships(String arg) {
    switch (arg) {
      case "low_tech":
        return effectStrings.effects("effect_ships_low_tech");
      case "midline":
        return effectStrings.effects("effect_ships_midline");
      case "pirate":
        return effectStrings.effects("effect_ships_pirate");
      case "destroyer":
        return effectStrings.effects("effect_ships_destroyer");
      case "capital":
        return effectStrings.effects("effect_ships_capital");
      case "cruiser":
        return effectStrings.effects("effect_ships_cruiser");
      case "high_tech":
        return effectStrings.effects("effect_ships_high_tech");
      case "civilian":
        return effectStrings.effects("effect_ships_civilian");
      case "derelict":
        return effectStrings.effects("effect_ships_derelict");
      case "dmods_3":
        return effectStrings.effects("effect_ships_dmods_3");
      default:
        return "";
    }
  }

  public static String credited(String message, String names, String source) {
    if (message == null || names == null || names.isEmpty() || !message.endsWith(".")) {
      return message;
    }
    return fill(source, message.substring(0, message.length() - 1), names);
  }

  public static String limitNoted(String message, boolean stopped, String template,
      String percent) {
    if (!stopped || message == null || template == null || template.isEmpty()) {
      return message;
    }
    return message + " " + fill(template, percent);
  }

  public static String listed(Collection<String> names, String comma, String join) {
    Set<String> once = new LinkedHashSet<String>();
    if (names != null) {
      for (String name : names) {
        if (name != null && !name.trim().isEmpty()) {
          once.add(name.trim());
        }
      }
    }
    StringBuilder out = new StringBuilder();
    int i = 0;
    for (String name : once) {
      if (i > 0) {
        out.append(i == once.size() - 1 ? join : comma);
      }
      out.append(name);
      i++;
    }
    return out.toString();
  }

  private static String words(String arg) {
    return arg.replace('_', ' ').trim();
  }

  public static String num(float v) {
    if (Float.isNaN(v) || Float.isInfinite(v)) {
      return "0";
    }
    String sign = v < 0f ? "-" : "";
    float a = Math.abs(v);
    if (a == Math.floor(a)) {
      return sign + grouped((long) a);
    }
    if (a < 0.1f) {
      int thousandths = Math.round(a * 1000f);
      if (thousandths == 0) {
        return "0";
      }
      if (thousandths % 10 != 0 && thousandths < 100) {
        return sign + "0.0" + (thousandths < 10 ? "0" : "") + thousandths;
      }
    }
    if (a < 1f) {
      int hundredths = Math.round(a * 100f);
      if (hundredths >= 100) {
        return sign + "1";
      }
      if (hundredths == 0) {
        return "0";
      }
      if (hundredths % 10 == 0) {
        return sign + "0." + (hundredths / 10);
      }
      return sign + "0." + (hundredths < 10 ? "0" : "") + hundredths;
    }
    long tenths = Math.round(a * 10.0);
    String whole = grouped(tenths / 10);
    return sign + (tenths % 10 == 0 ? whole : whole + "." + (tenths % 10));
  }

  static String count(String kind, float mag) {
    return CqEffectScale.rolled(kind) ? CqRoll.range(mag) : num(mag);
  }

  private static boolean one(String kind, float mag) {
    return "1".equals(count(kind, mag));
  }

  private static String grouped(long n) {
    String digits = String.valueOf(n);
    StringBuilder out = new StringBuilder(digits.length() + 4);
    for (int i = 0; i < digits.length(); i++) {
      if (i > 0 && (digits.length() - i) % 3 == 0) {
        out.append(',');
      }
      out.append(digits.charAt(i));
    }
    return out.toString();
  }

  static String token(String template, int slot, String number) {
    int at = template.indexOf("%s");
    for (int i = 0; i < slot && at >= 0; i++) {
      at = template.indexOf("%s", at + 2);
    }
    if (at < 0) {
      return number;
    }
    String sign = "";
    if (at > 0 && (template.charAt(at - 1) == '+' || template.charAt(at - 1) == '-')
        && (at == 1 || template.charAt(at - 2) == ' ')) {
      sign = template.substring(at - 1, at);
    }
    boolean percent = template.startsWith("%", at + 2) && !template.startsWith("%s", at + 2);
    return sign + number + (percent ? "%" : "");
  }

  static String fill(String template, String... args) {
    int slots = 0;
    for (int at = template.indexOf("%s"); at >= 0; at = template.indexOf("%s", at + 2)) {
      slots++;
    }
    if (slots != args.length) {
      throw new IllegalStateException("Captain's Quarters: the effect template \"" + template
          + "\" has " + slots + " %s slot(s) but " + args.length + " arg(s)");
    }
    StringBuilder out = new StringBuilder(template.length() + 48);
    int from = 0;
    for (String arg : args) {
      int at = template.indexOf("%s", from);
      out.append(template, from, at).append(arg);
      from = at + 2;
    }
    out.append(template, from, template.length());
    return out.toString();
  }
}
