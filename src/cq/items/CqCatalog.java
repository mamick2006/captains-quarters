package cq.items;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;

import cq.api.CqFurnishingEffect;
import cq.campaign.CqData;
import cq.campaign.CqSettings;
import cq.campaign.CqStrings;
import cq.effects.pure.CqArgs;
import cq.effects.pure.CqEffectLine;
import cq.effects.pure.CqEffectScale;
import cq.effects.pure.CqRoll;
import cq.effects.pure.CqWhen;

public class CqCatalog {

  public static final String CSV_PATH = "data/config/cq/cq_furnishings.csv";

  public static final String MASTER_MOD_ID = "captains_quarters";

  private static final Map<String, String> SLOT_NAMES;
  static {
    Map<String, String> names = new LinkedHashMap<String, String>();
    names.put(CqData.SLOT_BUNK, "Bunk");
    names.put(CqData.SLOT_WALL, "Wall art");
    names.put(CqData.SLOT_PET, "Pet");
    names.put(CqData.SLOT_PLANT, "Plant");
    names.put(CqData.SLOT_DESK, "Desk");
    names.put(CqData.SLOT_DISPLAY, "Display case");
    names.put(CqData.SLOT_RUG, "Rug");
    names.put(CqData.SLOT_LIGHT, "Lighting");
    names.put(CqData.SLOT_GALLEY, "Galley nook");
    names.put(CqData.SLOT_ENT, "Entertainment");
    SLOT_NAMES = Collections.unmodifiableMap(names);
  }

  public static boolean isSlot(String slotId) {
    return SLOT_NAMES.containsKey(slotId);
  }

  public static String slotName(String slotId) {
    String name = SLOT_NAMES.get(slotId);
    return name != null ? name : String.valueOf(slotId);
  }

  public enum Rarity {
    COMMON("Common", "cq_rarity_weight_common", 1.0f, 1500, new Color(210, 210, 210)),
    UNCOMMON("Uncommon", "cq_rarity_weight_uncommon", 0.55f, 4000, new Color(140, 210, 130)),
    RARE("Rare", "cq_rarity_weight_rare", 0.30f, 10000, new Color(130, 170, 255)),
    EXCEPTIONAL("Exceptional", "cq_rarity_weight_exceptional", 0.12f, 25000, new Color(255, 175, 90));

    private final String displayName;
    private final String weightKey;
    private final float defaultWeight;
    private final int basePrice;
    private final Color color;

    Rarity(String displayName, String weightKey, float defaultWeight, int basePrice, Color color) {
      this.displayName = displayName;
      this.weightKey = weightKey;
      this.defaultWeight = defaultWeight;
      this.basePrice = basePrice;
      this.color = color;
    }

    public String getDisplayName() {
      return displayName;
    }

    public float getWeight() {
      return CqSettings.getFloat(weightKey, defaultWeight);
    }

    public int getBasePrice() {
      return basePrice;
    }

    public Color getColor() {
      return color;
    }
  }

  public enum Channel {
    MARKET("cq_pool_market"),
    SCAVENGE("cq_pool_scavenge"),
    BATTLE("cq_pool_battle"),
    QUEST("cq_pool_quest"),
    GUARANTEED("cq_pool_guaranteed"),
    UNIQUE("cq_pool_unique"),
    THEMATIC("cq_pool_thematic");

    private final String poolTag;

    Channel(String poolTag) {
      this.poolTag = poolTag;
    }

    public String getPoolTag() {
      return poolTag;
    }
  }

  public enum Kind {
    PEAK_TIME,
    GROUND_OPS,
    NONCOMBAT_CREW_LOSS,
    SALVAGE_NOT_RARE,
    SUPPLY_USE_CAPPED,
    SURVEY_COST,
    BATTLE_SALVAGE,
    DETECTED_AT,
    SENSOR_RANGE,
    REPAIR_RATE,
    OFFICER_XP,
    MARINE_CASUALTIES,
    FUEL_SALVAGE,
    EBURN_CR_COST,
    GO_DARK_DETECT,
    CUSTOM_PRODUCTION,
    EMP_TAKEN,
    FLUX_DISSIPATION,
    CREW_COMBAT_LOSS,
    CREW_HULL_DAMAGE,
    CR_DEGRADATION,
    TERRAIN_PENALTY,
    INSTA_REPAIR,
    MOVE_SLOW_BONUS,
    FUEL_USE_CAPPED,
    RECOVERED_HULL_MAX,
    RECOVERED_CR_MAX,
    COMMAND_POINT_RATE,
    CORONA_RESIST,
    MAX_CONTACTS,
    GROUND_SUPPORT,
    DMOD_REDUCTION,
    CR_RECOVERY,
    TOP_SPEED,
    VENT_RATE,
    FLUX_CAPACITY,
    SHIELD_DAMAGE,
    OVERLOAD_REDUCTION,
    RECOIL,
    MANEUVERABILITY,
    ENEMY_SALVAGE_RECOVERY,
    ARMOR_DAMAGE_TAKEN,
    WEAPON_RANGE,
    MAX_CR,
    COMMAND_POINTS,
    TIME_FLOW,
    TARIFF_CAPPED,
    CREW_RETURNED,
    MARINES_RETURNED,
    CR_REGAINED,
    RELATIONSHIP,
    NEW_CONTACT_REL,
    RECOVERY_HULL,
    RECOVERY_CR,
    RECOVERY_HULL_CR,
    RECOVERY_DMODS,
    MONTHLY_YIELD,
    EXPERIENCE,
    RARE_SALVAGE,
    RECOVERY_CARGO,
    DMOD_REPAIRED
  }

  public static CqWhen.Family homeOf(Kind kind) {
    if (kind == null) {
      return null;
    }
    switch (kind) {
      case TIME_FLOW:
        return CqWhen.Family.COMBAT;
      case TARIFF_CAPPED:
        return CqWhen.Family.TARIFF;
      case CREW_RETURNED:
      case MARINES_RETURNED:
      case CR_REGAINED:
        return CqWhen.Family.CASUALTY;
      case RELATIONSHIP:
      case NEW_CONTACT_REL:
        return CqWhen.Family.CONTACT;
      case RECOVERY_HULL:
      case RECOVERY_CR:
      case RECOVERY_HULL_CR:
      case RECOVERY_DMODS:
      case RECOVERY_CARGO:
        return CqWhen.Family.RECOVERY;
      case MONTHLY_YIELD:
      case EXPERIENCE:
      case DMOD_REPAIRED:
        return CqWhen.Family.MONTH;
      default:
        return null;
    }
  }

  public static class Furnishing {
    private final String id;
    private final String name;
    private final String slotId;
    private final Rarity rarity;
    private final Channel channel;
    private final Kind kind;
    private final float defaultMag;
    private final float defaultPct;
    private final Kind kind2;
    private final float defaultMag2;
    private final float defaultPct2;
    private final String sourceMod;
    private final boolean ownRow;
    private final CqFurnishingEffect effect;
    private final CqWhen when;
    private final String arg;

    Furnishing(String id, String name, String slotId, Rarity rarity, Channel channel,
        Kind kind, float defaultMag, float defaultPct, Kind kind2, float defaultMag2,
        float defaultPct2, String sourceMod, boolean ownRow, CqFurnishingEffect effect,
        CqWhen when, String arg) {
      this.when = when;
      this.arg = arg == null ? "" : arg;
      this.id = id;
      this.name = name;
      this.slotId = slotId;
      this.rarity = rarity;
      this.channel = channel;
      this.kind = kind;
      this.defaultMag = defaultMag;
      this.defaultPct = defaultPct;
      this.kind2 = kind2;
      this.defaultMag2 = defaultMag2;
      this.defaultPct2 = defaultPct2;
      this.sourceMod = sourceMod == null ? "" : sourceMod;
      this.ownRow = ownRow;
      this.effect = effect;
    }

    public String getId() {
      return id;
    }

    public String getSourceMod() {
      return sourceMod;
    }

    public boolean isOwnRow() {
      return ownRow;
    }

    public CqFurnishingEffect getEffectPlugin() {
      return effect;
    }

    public String getName() {
      return name;
    }

    public String getSlotId() {
      return slotId;
    }

    public Rarity getRarity() {
      return rarity;
    }

    public Channel getChannel() {
      return channel;
    }

    public Kind getKind() {
      return kind;
    }

    public Kind getKind2() {
      return kind2;
    }

    public CqWhen getWhen() {
      return when;
    }

    public String getArg() {
      return arg;
    }

    public String getSlug() {
      return id.startsWith("cq_") ? id.substring(3) : id;
    }

    public float getMag() {
      return CqSettings.getFloat("cq_item_" + getSlug() + "_mag", defaultMag);
    }

    private static boolean kindCapped(Kind k) {
      return k == Kind.SUPPLY_USE_CAPPED || k == Kind.FUEL_USE_CAPPED
          || k == Kind.TARIFF_CAPPED || k == Kind.CREW_RETURNED || k == Kind.MARINES_RETURNED;
    }

    private static float scaledMag(Kind k, float raw) {
      return CqEffectScale.mag(k == null ? null : k.name(), raw,
          CqSettings.difficultyEffectMult());
    }

    private static float scaledPct(Kind k, float raw) {
      return CqEffectScale.pct(k == null ? null : k.name(), raw,
          CqSettings.difficultyEffectMult());
    }

    public float getPct() {
      if (!kindCapped(kind)) {
        return 0f;
      }
      return CqSettings.getFloat("cq_item_" + getSlug() + "_pct", defaultPct);
    }

    public float getMag2() {
      if (kind2 == null) {
        return 0f;
      }
      return CqSettings.getFloat("cq_item_" + getSlug() + "_mag2", defaultMag2);
    }

    public float getPct2() {
      if (kind2 == null || !kindCapped(kind2)) {
        return 0f;
      }
      return CqSettings.getFloat("cq_item_" + getSlug() + "_pct2", defaultPct2);
    }

    public float getEffectiveMag() {
      return scaledMag(kind, getMag());
    }

    public float getEffectivePct() {
      return scaledPct(kind, getPct());
    }

    public float getEffectiveMag2() {
      if (kind2 == null) {
        return 0f;
      }
      return scaledMag(kind2, getMag2());
    }

    public float getEffectivePct2() {
      return scaledPct(kind2, getPct2());
    }

    public String getHomeMarketId() {
      if (channel != Channel.UNIQUE) {
        return null;
      }
      String home = CqSettings.getString("cq_item_" + getSlug() + "_home", null);
      return home == null || home.isEmpty() ? null : home;
    }

    private static boolean kindPiloted(Kind k) {
      return k == Kind.PEAK_TIME || k == Kind.EMP_TAKEN || k == Kind.FLUX_DISSIPATION
          || k == Kind.CREW_COMBAT_LOSS || k == Kind.CREW_HULL_DAMAGE
          || k == Kind.CR_DEGRADATION || k == Kind.TOP_SPEED || k == Kind.VENT_RATE
          || k == Kind.FLUX_CAPACITY || k == Kind.SHIELD_DAMAGE
          || k == Kind.OVERLOAD_REDUCTION || k == Kind.RECOIL || k == Kind.MANEUVERABILITY
          || k == Kind.ARMOR_DAMAGE_TAKEN || k == Kind.WEAPON_RANGE || k == Kind.MAX_CR
          || k == Kind.TIME_FLOW;
    }

    public boolean isPiloted() {
      return kindPiloted(kind) || (kind2 != null && kindPiloted(kind2));
    }

    private String lineOf(Kind k, float effectiveMag, float effectivePct) {
      if (k == null) {
        return "";
      }
      String stock = stockLine(k, effectiveMag, effectivePct);
      if (when == null && !kindAppended(k)) {
        return stock;
      }
      return CqEffectText.line(this, k, effectiveMag, effectivePct, stock);
    }

    private static boolean kindAppended(Kind k) {
      return k.ordinal() > Kind.ENEMY_SALVAGE_RECOVERY.ordinal();
    }

    private static String stockMag(Kind k, float effectiveMag) {
      if (CqEffectScale.rolled(k.name()) && CqRoll.fractional(effectiveMag)) {
        return CqRoll.range(effectiveMag);
      }
      return fmt(effectiveMag);
    }

    private String stockLine(Kind k, float effectiveMag, float effectivePct) {
      String mag = stockMag(k, effectiveMag);
      switch (k) {
        case PEAK_TIME:
          return CqStrings.effects("effect_peak_time", mag);
        case GROUND_OPS:
          return CqStrings.effects("effect_ground_ops", mag);
        case NONCOMBAT_CREW_LOSS:
          return CqStrings.effects("effect_noncombat_crew_loss", mag);
        case SALVAGE_NOT_RARE:
          return CqStrings.effects("effect_salvage_not_rare", mag);
        case SUPPLY_USE_CAPPED:
          return CqStrings.effects("effect_supply_use_capped",
              fmt(effectivePct), mag);
        case FUEL_USE_CAPPED:
          return CqStrings.effects("effect_fuel_use_capped",
              fmt(effectivePct), mag);
        case MOVE_SLOW_BONUS:
          return CqStrings.effects("effect_move_slow_bonus", mag);
        case SURVEY_COST:
          return CqStrings.effects("effect_survey_cost", mag);
        case BATTLE_SALVAGE:
          return CqStrings.effects("effect_battle_salvage", mag);
        case DETECTED_AT:
          return CqStrings.effects("effect_detected_at", mag);
        case SENSOR_RANGE:
          return CqStrings.effects("effect_sensor_range", mag);
        case REPAIR_RATE:
          return CqStrings.effects("effect_repair_rate", mag);
        case OFFICER_XP:
          return CqStrings.effects("effect_officer_xp", mag);
        case MARINE_CASUALTIES:
          return CqStrings.effects("effect_marine_casualties", mag);
        case FUEL_SALVAGE:
          return CqStrings.effects("effect_fuel_salvage", mag);
        case EBURN_CR_COST:
          return CqStrings.effects("effect_eburn_cr_cost", mag);
        case GO_DARK_DETECT:
          return CqStrings.effects("effect_go_dark_detect", mag);
        case CUSTOM_PRODUCTION:
          return CqStrings.effects("effect_custom_production", mag);
        case EMP_TAKEN:
          return CqStrings.effects("effect_emp_taken", mag);
        case FLUX_DISSIPATION:
          return CqStrings.effects("effect_flux_dissipation", mag);
        case CREW_COMBAT_LOSS:
          return CqStrings.effects("effect_crew_combat_loss", mag);
        case CREW_HULL_DAMAGE:
          return CqStrings.effects("effect_crew_hull_damage", mag);
        case CR_DEGRADATION:
          return CqStrings.effects("effect_cr_degradation", mag);
        case TERRAIN_PENALTY:
          return CqStrings.effects("effect_terrain_penalty", mag);
        case INSTA_REPAIR:
          return CqStrings.effects("effect_insta_repair", mag);
        case ENEMY_SALVAGE_RECOVERY:
          return CqStrings.effects("effect_enemy_salvage_recovery", mag);
        case RECOVERED_HULL_MAX:
          return CqStrings.effects("effect_recovered_hull_max", mag);
        case RECOVERED_CR_MAX:
          return CqStrings.effects("effect_recovered_cr_max", mag);
        case COMMAND_POINT_RATE:
          return CqStrings.effects("effect_command_point_rate", mag);
        case CORONA_RESIST:
          return CqStrings.effects("effect_corona_resist", mag);
        case MAX_CONTACTS:
          return CqStrings.effects("effect_max_contacts", mag);
        case GROUND_SUPPORT:
          return CqStrings.effects("effect_ground_support", mag);
        case DMOD_REDUCTION:
          return CqStrings.effects(effectiveMag == 1f
              ? "effect_dmod_reduction_one" : "effect_dmod_reduction_many", mag);
        case CR_RECOVERY:
          return CqStrings.effects("effect_cr_recovery", mag);
        case TOP_SPEED:
          return CqStrings.effects("effect_top_speed", mag);
        case VENT_RATE:
          return CqStrings.effects("effect_vent_rate", mag);
        case FLUX_CAPACITY:
          return CqStrings.effects("effect_flux_capacity", mag);
        case SHIELD_DAMAGE:
          return CqStrings.effects("effect_shield_damage", mag);
        case OVERLOAD_REDUCTION:
          return CqStrings.effects("effect_overload_reduction", mag);
        case RECOIL:
          return CqStrings.effects("effect_recoil", mag);
        case MANEUVERABILITY:
          return CqStrings.effects("effect_maneuverability", mag);
        default:
          return "";
      }
    }

    public String getEffectText() {
      List<String> lines = getEffectLines();
      StringBuilder text = new StringBuilder();
      for (int i = 0; i < lines.size(); i++) {
        if (i > 0) {
          text.append(CqStrings.effects("effect_join"));
        }
        text.append(lines.get(i));
      }
      return text.toString();
    }

    public List<String> getEffectLines() {
      List<String> lines = CqEffectLine.shown(
          kind == null ? null : lineOf(kind, getEffectiveMag(), getEffectivePct()),
          kind2 == null ? null : lineOf(kind2, getEffectiveMag2(), getEffectivePct2()));
      addPluginLines(lines);
      return lines;
    }

    public boolean hasEffect() {
      return !getEffectLines().isEmpty();
    }

    private void addPluginLines(List<String> lines) {
      if (effect == null || pluginDisabled) {
        return;
      }
      try {
        List<String> extra = effect.effectLines(this);
        if (extra == null) {
          return;
        }
        for (String line : extra) {
          if (line != null && !line.isEmpty()) {
            lines.add(line);
          }
        }
      } catch (Throwable t) {
        disablePlugin("effectLines", t);
      }
    }

    private String[] tokensOf(Kind k, float effectiveMag, float effectivePct) {
      if (k == null) {
        return new String[0];
      }
      String[] stock = stockTokens(k, effectiveMag, effectivePct);
      if (when == null && !kindAppended(k)) {
        return stock;
      }
      return CqEffectText.tokens(this, k, effectiveMag, effectivePct, stock);
    }

    private String[] stockTokens(Kind k, float effectiveMag, float effectivePct) {
      String mag = stockMag(k, effectiveMag);
      switch (k) {
        case PEAK_TIME:
        case MOVE_SLOW_BONUS:
        case MAX_CONTACTS:
        case GROUND_SUPPORT:
          return new String[] { "+" + mag };
        case DMOD_REDUCTION:
          return new String[] { mag };
        case SUPPLY_USE_CAPPED:
        case FUEL_USE_CAPPED:
          return new String[] { "-" + fmt(effectivePct) + "%", mag };
        case NONCOMBAT_CREW_LOSS:
        case SURVEY_COST:
        case DETECTED_AT:
        case MARINE_CASUALTIES:
        case EBURN_CR_COST:
        case GO_DARK_DETECT:
        case EMP_TAKEN:
        case CREW_COMBAT_LOSS:
        case CREW_HULL_DAMAGE:
        case CR_DEGRADATION:
        case TERRAIN_PENALTY:
        case CORONA_RESIST:
        case SHIELD_DAMAGE:
        case OVERLOAD_REDUCTION:
        case RECOIL:
          return new String[] { "-" + mag + "%" };
        default:
          return new String[] { "+" + mag + "%" };
      }
    }

    public String[] getEffectHighlights() {
      List<String> tokens = new ArrayList<String>(2);
      if (kind != null) {
        Collections.addAll(tokens, tokensOf(kind, getEffectiveMag(), getEffectivePct()));
      }
      if (kind2 != null) {
        Collections.addAll(tokens, tokensOf(kind2, getEffectiveMag2(), getEffectivePct2()));
      }
      addPluginHighlights(tokens);
      return tokens.toArray(new String[tokens.size()]);
    }

    private void addPluginHighlights(List<String> tokens) {
      if (effect == null || pluginDisabled) {
        return;
      }
      try {
        String[] extra = effect.effectHighlights(this);
        if (extra == null) {
          return;
        }
        for (String token : extra) {
          if (token != null && !token.isEmpty()) {
            tokens.add(token);
          }
        }
      } catch (Throwable t) {
        disablePlugin("effectHighlights", t);
      }
    }

    private volatile boolean pluginDisabled;

    public boolean isPluginActive() {
      return effect != null && !pluginDisabled;
    }

    public void notePluginFailure(String seam, Throwable t) {
      disablePlugin(seam, t);
    }

    private void disablePlugin(String seam, Throwable t) {
      pluginDisabled = true;
      Global.getLogger(CqCatalog.class).warn("Captain's Quarters: the furnishing " + id
          + " (from " + (sourceMod.isEmpty() ? "an unnamed mod" : sourceMod) + ") threw from "
          + seam + "; its effect is off for the rest of this session. The furnishing itself"
          + " stays usable and every other furnishing is unaffected.", t);
    }

    static String fmt(float v) {
      if (v == Math.floor(v)) {
        return String.valueOf((int) v);
      }
      return String.valueOf(Math.round(v * 10f) / 10f);
    }
  }

  private static volatile List<Furnishing> all;
  private static volatile Map<String, Furnishing> byId;
  private static volatile List<String> lastProblems = Collections.emptyList();

  public static synchronized void reload() {
    Map<String, Furnishing> index = new LinkedHashMap<String, Furnishing>();
    List<String> problems = new ArrayList<String>();
    try {
      String ownDir = ownModDirName();
      Set<String> factionIds = loadedFactionIds();
      Set<String> commodityIds = loadedCommodityIds();
      JSONArray table = Global.getSettings()
          .getMergedSpreadsheetDataForMod("id", CSV_PATH, MASTER_MOD_ID);
      for (int i = 0; i < table.length(); i++) {
        JSONObject row = table.getJSONObject(i);
        String id = str(row, "id");
        if (id.isEmpty() || id.startsWith("#")) {
          continue;
        }
        String source = modDirOf(str(row, "fs_rowSource"));
        Furnishing f = parse(row, id, source, ownDir, factionIds, commodityIds, problems);
        if (f != null) {
          index.put(id, f);
        }
      }
    } catch (Throwable t) {
      emptyWarnOnce("could not read " + CSV_PATH, t);
    }

    if (index.isEmpty()) {
      emptyWarnOnce("read no furnishing rows from " + CSV_PATH
          + "; the collection is left exactly as it is", null);
      return;
    }
    all = Collections.unmodifiableList(new ArrayList<Furnishing>(index.values()));
    byId = Collections.unmodifiableMap(index);
    lastProblems = Collections.unmodifiableList(new ArrayList<String>(problems));
    if (!problems.isEmpty()) {
      loadWarn(problems.size() + " furnishing row(s) had problems: " + join(problems, 5), null);
    }
  }

  private static void ensureLoaded() {
    if (byId == null) {
      reload();
    }
  }

  private static Furnishing parse(JSONObject row, String id, String source, String ownDir,
      Set<String> factionIds, Set<String> commodityIds, List<String> problems) {
    String name = str(row, "name");
    String category = str(row, "category");
    Rarity rarity = enumOf(Rarity.class, str(row, "rarity"));
    Channel channel = enumOf(Channel.class, str(row, "channel"));
    String where = id + (source.isEmpty() ? "" : " from " + source);
    if (name.isEmpty()) {
      problems.add(where + ": no name");
      return null;
    }
    if (!isSlot(category)) {
      problems.add(where + ": category '" + category + "' is not one of the ten");
      return null;
    }
    if (rarity == null) {
      problems.add(where + ": unknown rarity '" + str(row, "rarity") + "'");
      return null;
    }
    if (channel == null) {
      problems.add(where + ": unknown channel '" + str(row, "channel") + "'");
      return null;
    }
    Kind kind = kindOf(str(row, "kind"), where, "kind", problems);
    Kind kind2 = kindOf(str(row, "kind2"), where, "kind2", problems);
    String whenCell = str(row, "when");
    CqWhen when = CqWhen.parse(whenCell);
    String whenProblem = !whenCell.isEmpty() && when == null
        ? "unknown when '" + whenCell + "'"
        : CqWhen.mismatch(when, kind != null, homeOf(kind), Furnishing.kindPiloted(kind),
            kind2 != null, homeOf(kind2), Furnishing.kindPiloted(kind2));
    if (whenProblem != null) {
      problems.add(where + ": " + whenProblem + ", so its effect is off");
      kind = null;
      kind2 = null;
      when = null;
    }
    String arg = str(row, "arg").toLowerCase(Locale.ROOT);
    noteArg(kind, arg, factionIds, commodityIds, where, problems);
    noteArg(kind2, arg, factionIds, commodityIds, where, problems);
    CqFurnishingEffect effect = effectOf(str(row, "plugin"), where, problems);
    boolean own = ownDir.isEmpty() || source.isEmpty()
        ? id.startsWith("cq_") : ownDir.equals(source);
    return new Furnishing(id, name, category, rarity, channel, kind,
        num(row, "mag", where, problems), num(row, "pct", where, problems), kind2,
        num(row, "mag2", where, problems), num(row, "pct2", where, problems),
        source, own, effect, when, arg);
  }

  private static void noteArg(Kind kind, String arg, Set<String> factionIds,
      Set<String> commodityIds, String where, List<String> problems) {
    String problem = kind == null ? null
        : CqArgs.problem(kind.name(), arg, factionIds, commodityIds);
    if (problem != null) {
      problems.add(where + ": " + problem + ", so that effect pays nothing");
    }
  }

  private static Set<String> loadedFactionIds() {
    Set<String> ids = new LinkedHashSet<String>();
    try {
      for (FactionSpecAPI spec : Global.getSettings().getAllFactionSpecs()) {
        if (spec != null && spec.getId() != null) {
          ids.add(spec.getId());
        }
      }
    } catch (Throwable ignore) {
    }
    return ids;
  }

  private static Set<String> loadedCommodityIds() {
    Set<String> ids = new LinkedHashSet<String>();
    try {
      for (CommoditySpecAPI spec : Global.getSettings().getAllCommoditySpecs()) {
        if (spec != null && spec.getId() != null) {
          ids.add(spec.getId());
        }
      }
    } catch (Throwable ignore) {
    }
    return ids;
  }

  static final String OWN_SALVAGE_ALIAS = "own_salvage_recovery";

  private static Kind kindOf(String value, String where, String column, List<String> problems) {
    if (value.isEmpty()) {
      return null;
    }
    Kind kind = OWN_SALVAGE_ALIAS.equalsIgnoreCase(value) ? Kind.ENEMY_SALVAGE_RECOVERY
        : enumOf(Kind.class, value);
    if (kind == null) {
      problems.add(where + ": unknown " + column + " '" + value + "'");
    }
    return kind;
  }

  private static CqFurnishingEffect effectOf(String className, String where,
      List<String> problems) {
    if (className.isEmpty()) {
      return null;
    }
    try {
      Class<?> type = Class.forName(className, true, CqCatalog.class.getClassLoader());
      Object plugin = MethodHandles.lookup()
          .findConstructor(type, MethodType.methodType(void.class)).invoke();
      if (plugin instanceof CqFurnishingEffect) {
        return (CqFurnishingEffect) plugin;
      }
      problems.add(where + ": plugin " + className + " does not implement "
          + CqFurnishingEffect.class.getName());
    } catch (Throwable t) {
      problems.add(where + ": plugin " + className + " would not load (" + t + ")");
    }
    return null;
  }

  private static <E extends Enum<E>> E enumOf(Class<E> type, String value) {
    if (value.isEmpty()) {
      return null;
    }
    try {
      return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static String str(JSONObject row, String column) {
    String value = row.optString(column, "");
    return value == null ? "" : value.trim();
  }

  private static float num(JSONObject row, String column, String where, List<String> problems) {
    String value = str(row, column);
    if (value.isEmpty()) {
      return 0f;
    }
    try {
      return Float.parseFloat(value);
    } catch (NumberFormatException e) {
      problems.add(where + ": " + column + " '" + value + "' is not a number");
      return 0f;
    }
  }

  private static String ownModDirName() {
    try {
      ModSpecAPI spec = Global.getSettings().getModManager().getModSpec(MASTER_MOD_ID);
      if (spec != null && spec.getDirName() != null) {
        return spec.getDirName();
      }
    } catch (Throwable ignore) {
    }
    return "";
  }

  static String modDirOf(String rowSource) {
    if (rowSource == null || rowSource.isEmpty()) {
      return "";
    }
    String path = rowSource.replace('\\', '/');
    int at = path.lastIndexOf("/mods/");
    if (at < 0) {
      return "";
    }
    String rest = path.substring(at + "/mods/".length());
    int end = rest.indexOf('/');
    return end < 0 ? rest : rest.substring(0, end);
  }

  private static String join(List<String> items, int limit) {
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < items.size() && i < limit; i++) {
      if (i > 0) {
        text.append("; ");
      }
      text.append(items.get(i));
    }
    if (items.size() > limit) {
      text.append("; and ").append(items.size() - limit).append(" more");
    }
    return text.toString();
  }

  private static void loadWarn(String what, Throwable t) {
    String line = "Captain's Quarters: " + what + ".";
    if (t == null) {
      Global.getLogger(CqCatalog.class).warn(line);
    } else {
      Global.getLogger(CqCatalog.class).warn(line, t);
    }
  }

  private static volatile boolean emptyWarned;

  private static void emptyWarnOnce(String what, Throwable t) {
    if (emptyWarned) {
      return;
    }
    emptyWarned = true;
    loadWarn(what, t);
  }

  public static Furnishing get(String itemId) {
    if (itemId == null) {
      return null;
    }
    ensureLoaded();
    Map<String, Furnishing> index = byId;
    return index == null ? null : index.get(itemId);
  }

  public static List<Furnishing> all() {
    ensureLoaded();
    List<Furnishing> rows = all;
    return rows == null ? Collections.<Furnishing>emptyList() : rows;
  }

  public static Set<String> itemIds() {
    ensureLoaded();
    Map<String, Furnishing> index = byId;
    return index == null ? new LinkedHashSet<String>()
        : new LinkedHashSet<String>(index.keySet());
  }

  public static Furnishing resolveConfigured(String idOrSlug) {
    if (idOrSlug == null || idOrSlug.isEmpty()) {
      return null;
    }
    Furnishing f = get(idOrSlug);
    return f != null ? f : get("cq_" + idOrSlug);
  }

  public static List<Furnishing> forSlot(String slotId) {
    List<Furnishing> result = new ArrayList<Furnishing>();
    for (Furnishing f : all()) {
      if (f.getSlotId().equals(slotId)) {
        result.add(f);
      }
    }
    return result;
  }

  public static List<Furnishing> pool(Channel channel) {
    List<Furnishing> result = new ArrayList<Furnishing>();
    for (Furnishing f : all()) {
      if (f.getChannel() == channel) {
        result.add(f);
      }
    }
    return result;
  }

}
