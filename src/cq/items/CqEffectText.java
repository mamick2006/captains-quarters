package cq.items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;

import cq.campaign.CqStrings;
import cq.effects.pure.CqEffectLine;
import cq.effects.pure.CqRecoveryMath;

public final class CqEffectText {

  private CqEffectText() {
  }

  private static final CqEffectLine LINES = new CqEffectLine(new CqEffectLine.Source() {
    @Override
    public String effects(String key) {
      return CqStrings.effects(key);
    }
  });

  public static String line(CqCatalog.Furnishing f, CqCatalog.Kind kind, float effectiveMag,
      float effectivePct, String stock) {
    CqCatalog.Kind first = f.getKind();
    boolean second = secondSlot(f, kind, effectiveMag, effectivePct);
    return LINES.line(kind.name(), second && first != null ? first.name() : null, f.getWhen(),
        f.getArg(), argName(f, kind), effectiveMag, effectivePct, stock);
  }

  private static String argName(CqCatalog.Furnishing f, CqCatalog.Kind kind) {
    if (kind == CqCatalog.Kind.RECOVERY_CARGO) {
      return commodityName(CqRecoveryMath.cargoCommodity(f.getArg()));
    }
    return kind == CqCatalog.Kind.MONTHLY_YIELD ? commodityName(f.getArg()) : null;
  }

  public static String[] tokens(CqCatalog.Furnishing f, CqCatalog.Kind kind, float effectiveMag,
      float effectivePct, String[] stock) {
    return LINES.tokens(kind.name(), f.getWhen(), f.getArg(), argName(f, kind), effectiveMag,
        effectivePct, stock);
  }

  public static boolean statesOccasion(CqCatalog.Furnishing f) {
    CqCatalog.Kind first = f.getKind();
    CqCatalog.Kind second = f.getKind2();
    return (first != null && CqEffectLine.statesOccasion(first.name(), f.getWhen()))
        || (second != null && CqEffectLine.statesOccasion(second.name(), f.getWhen()));
  }

  public static String credit(String message, Collection<String> itemIds) {
    String names = names(itemIds);
    if (names.isEmpty()) {
      return message;
    }
    return CqEffectLine.credited(message, names, CqStrings.effects("payout_source"));
  }

  public static String names(Collection<String> itemIds) {
    List<String> names = new ArrayList<String>();
    if (itemIds != null) {
      for (String id : itemIds) {
        CqCatalog.Furnishing f = id == null ? null : CqCatalog.get(id);
        if (f != null) {
          names.add(f.getName());
        }
      }
    }
    return CqEffectLine.listed(names, CqStrings.effects("window_comma"),
        CqStrings.effects("window_join"));
  }

  private static boolean secondSlot(CqCatalog.Furnishing f, CqCatalog.Kind kind, float mag,
      float pct) {
    if (kind != f.getKind2()) {
      return false;
    }
    if (kind != f.getKind()) {
      return true;
    }
    return mag == f.getEffectiveMag2() && pct == f.getEffectivePct2()
        && !(mag == f.getEffectiveMag() && pct == f.getEffectivePct());
  }

  public static String commodityWords(String id) {
    String trimmed = id == null ? "" : id.trim();
    return LINES.commodity(trimmed, commodityName(trimmed));
  }

  private static String commodityName(String id) {
    if (id == null || id.isEmpty()) {
      return null;
    }
    try {
      CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(id);
      return spec == null ? null : spec.getLowerCaseName();
    } catch (RuntimeException e) {
      return null;
    }
  }
}
