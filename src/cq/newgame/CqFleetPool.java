package cq.newgame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.ShipVariantAPI;

import cq.newgame.pure.CqStartRules;

public final class CqFleetPool {

  private CqFleetPool() {
  }

  public static final class Entry {
    public final String hullId;
    public final String name;
    public final String designation;
    public final String size;
    public final int value;
    public final String source;
    public final String variantId;
    public final CqStartRules.Hull facts;

    Entry(String hullId, String name, String designation, String size, int value, String source,
        String variantId, CqStartRules.Hull facts) {
      this.hullId = hullId;
      this.name = name;
      this.designation = designation == null ? "" : designation;
      this.size = size;
      this.value = value;
      this.source = source;
      this.variantId = variantId;
      this.facts = facts;
    }
  }

  public static final String VANILLA_SOURCE = "Starsector";

  public static List<Entry> build() {
    return build(CqStartRules.Budgets.DEFAULTS);
  }

  public static List<Entry> build(CqStartRules.Budgets budgets) {
    List<Entry> out = new ArrayList<Entry>();
    for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
      try {
        CqStartRules.Hull facts = facts(spec);
        if (!CqStartRules.fits(facts, CqStartRules.Flavour.LONE, false, budgets)
            && !CqStartRules.fits(facts, CqStartRules.Flavour.SMALL, false, budgets)) {
          continue;
        }
        String variant = variantFor(spec);
        if (variant == null) {
          continue;
        }
        out.add(new Entry(spec.getHullId(), spec.getHullName(), spec.getDesignation(), facts.size,
            facts.baseValue,
            source(spec), variant, facts));
      } catch (Throwable t) {
      }
    }
    Collections.sort(out, new Comparator<Entry>() {
      @Override
      public int compare(Entry a, Entry b) {
        if (a.value != b.value) {
          return a.value < b.value ? -1 : 1;
        }
        return a.name.compareToIgnoreCase(b.name);
      }
    });
    return out;
  }

  public static CqStartRules.Hull facts(ShipHullSpecAPI spec) {
    Set<String> hints = new HashSet<String>();
    for (ShipTypeHints h : spec.getHints()) {
      hints.add(h.name());
    }
    Set<String> baseTags = null;
    String baseId = spec.getBaseHullId();
    if (baseId != null && !baseId.equals(spec.getHullId())) {
      try {
        ShipHullSpecAPI base = Global.getSettings().getHullSpec(baseId);
        baseTags = base == null ? null : new HashSet<String>(base.getTags());
      } catch (Throwable missing) {
        baseTags = null;
      }
    }
    List<String> builtIns = spec.getBuiltInMods();
    return new CqStartRules.Hull(spec.getHullId(), spec.getHullSize().name(),
        Math.round(spec.getBaseValue()), new HashSet<String>(spec.getTags()), hints,
        builtIns == null ? null : new HashSet<String>(builtIns), spec.isDefaultDHull(), spec.isDHull(),
        baseTags);
  }

  public static String source(ShipHullSpecAPI spec) {
    ModSpecAPI mod = spec.getSourceMod();
    return mod == null ? VANILLA_SOURCE : mod.getName();
  }

  public static String variantFor(ShipHullSpecAPI spec) {
    String hullId = spec.getHullId();
    String vanilla = CqStartRules.VANILLA_VARIANTS.get(hullId);
    if (vanilla != null && spec.getSourceMod() == null) {
      return vanilla;
    }
    String goal = null;
    String stock = null;
    List<String> ids = Global.getSettings().getHullIdToVariantListMap().getList(hullId);
    if (ids != null) {
      List<String> sorted = new ArrayList<String>(ids);
      Collections.sort(sorted);
      for (String id : sorted) {
        ShipVariantAPI v;
        try {
          v = Global.getSettings().getVariant(id);
        } catch (Throwable missing) {
          continue;
        }
        if (v == null || v.isEmptyHullVariant() || !hullId.equals(v.getHullSpec().getHullId())) {
          continue;
        }
        if (v.isGoalVariant() && goal == null) {
          goal = id;
        }
        if (stock == null) {
          stock = id;
        }
      }
    }
    if (goal != null) {
      return goal;
    }
    return stock;
  }
}
