package cq.effects.pure;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class CqArgs {

  private static final Set<String> CONTACT_GROUPS = Collections.unmodifiableSet(
      new LinkedHashSet<String>(Arrays.asList(CqContactMath.GROUP_TRADE,
          CqContactMath.GROUP_UNDERWORLD, CqContactMath.GROUP_MILITARY)));

  private CqArgs() {
  }

  public static Set<String> contactGroups() {
    return CONTACT_GROUPS;
  }

  public static String problem(String kind, String arg, Collection<String> factionIds,
      Collection<String> commodityIds) {
    String expected = kind == null ? null : what(kind);
    if (expected == null) {
      return null;
    }
    String value = arg == null ? "" : arg.trim();
    if (accepts(kind, value, factionIds, commodityIds)) {
      return null;
    }
    return "arg '" + value + "' is not " + expected + " for "
        + kind.toLowerCase(Locale.ROOT);
  }

  public static boolean readable(String kind, String arg) {
    return kind == null || accepts(kind, arg == null ? "" : arg.trim(), null, null);
  }

  private static String what(String kind) {
    switch (kind) {
      case "TARIFF_CAPPED":
        return "a market group or a faction id";
      case "CREW_RETURNED":
      case "MARINES_RETURNED":
      case "CR_REGAINED":
        return "blank, " + CqCasualtyMath.ARG_HYPERSPACE + " or "
            + CqCasualtyMath.ARG_NO_SHIP_DESTROYED;
      case "RELATIONSHIP":
        return CqContactMath.WHO_CONTACTS + ", " + CqContactMath.WHO_FACTION
            + " or a contact type";
      case "RECOVERY_HULL":
      case "RECOVERY_CR":
      case "RECOVERY_HULL_CR":
      case "RECOVERY_DMODS":
        return "a ship group";
      case "RECOVERY_CARGO":
        return "a ship group, a colon and a commodity id";
      case "MONTHLY_YIELD":
        return "a commodity id";
      case "EXPERIENCE":
        return "someone who can gain experience";
      default:
        return null;
    }
  }

  private static boolean accepts(String kind, String value, Collection<String> factionIds,
      Collection<String> commodityIds) {
    switch (kind) {
      case "TARIFF_CAPPED":
        if (value.isEmpty()) {
          return false;
        }
        return CqTariffMath.isNamedGroup(value) || unknownOrHolds(factionIds, value);
      case "CREW_RETURNED":
      case "MARINES_RETURNED":
      case "CR_REGAINED":
        return value.isEmpty() || CqCasualtyMath.ARG_HYPERSPACE.equals(value)
            || CqCasualtyMath.ARG_NO_SHIP_DESTROYED.equals(value);
      case "RELATIONSHIP":
        return CqContactMath.namesSomeone(value);
      case "RECOVERY_HULL":
      case "RECOVERY_CR":
      case "RECOVERY_HULL_CR":
      case "RECOVERY_DMODS":
        return CqRecoveryMath.knownGroup(value);
      case "RECOVERY_CARGO":
        return CqRecoveryMath.cargoArg(value)
            && unknownOrHolds(commodityIds, CqRecoveryMath.cargoCommodity(value));
      case "MONTHLY_YIELD":
        return !value.isEmpty() && unknownOrHolds(commodityIds, value);
      case "EXPERIENCE":
        return CqMonthMath.Recipient.parse(value) != null;
      default:
        return true;
    }
  }

  private static boolean unknownOrHolds(Collection<String> ids, String value) {
    return ids == null || ids.isEmpty() || ids.contains(value);
  }
}
