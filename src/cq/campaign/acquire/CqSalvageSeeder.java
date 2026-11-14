package cq.campaign.acquire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.DiscoverEntityListener;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.DropGroupRow;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;

import cq.campaign.CqData;
import cq.campaign.CqSettings;
import cq.campaign.CqSurvival;
import cq.items.CqCatalog;

public class CqSalvageSeeder implements DiscoverEntityListener {

  static final String SEEDED_FLAG = "$cq_furnishing_drop_seeded";

  private static final List<String> DEFAULT_TYPES = Arrays.asList(
      "station_research", "station_research_remnant",
      "station_mining", "station_mining_remnant",
      "orbital_habitat");

  private static volatile boolean warnedOnce;

  @Override
  public void reportEntityDiscovered(SectorEntityToken entity) {
    try {
      seedEntity(entity);
    } catch (Throwable t) {
      warnOnce(t);
    }
  }

  public static void syncTables() {
    try {
      for (LocationAPI location : Global.getSector().getAllLocations()) {
        for (SectorEntityToken entity : location.getEntitiesWithTag(Tags.SALVAGEABLE)) {
          if (isSeeded(entity)) {
            rebuild(entity);
          } else {
            seedEntity(entity);
          }
        }
      }
    } catch (Throwable t) {
      warnOnce(t);
    }
  }

  public static void refreshSeeded() {
    try {
      for (LocationAPI location : Global.getSector().getAllLocations()) {
        for (SectorEntityToken entity : location.getEntitiesWithTag(Tags.SALVAGEABLE)) {
          if (isSeeded(entity)) {
            rebuild(entity);
          }
        }
      }
    } catch (Throwable t) {
      warnOnce(t);
    }
  }

  private static boolean isSeeded(SectorEntityToken entity) {
    return entity.getMemoryWithoutUpdate() != null
        && entity.getMemoryWithoutUpdate().getBoolean(SEEDED_FLAG);
  }

  private static void rebuild(SectorEntityToken entity) {
    removeOurDropData(entity);
    String type = entity.getCustomEntityType();
    DropData guaranteed = buildGuaranteedDropData(type);
    if (guaranteed != null) {
      entity.addDropRandom(guaranteed);
    }
    DropData drop = buildDropData(type);
    if (drop != null) {
      entity.addDropRandom(drop);
    }
  }

  public static void seedEntity(SectorEntityToken entity) {
    if (entity == null || entity.getMemoryWithoutUpdate() == null) {
      return;
    }
    String type = entity.getCustomEntityType();
    if (type == null || !eligibleTypes().contains(type)) {
      return;
    }
    if (entity.getMemoryWithoutUpdate().getBoolean(SEEDED_FLAG)) {
      return;
    }
    boolean attached = false;
    DropData guaranteed = buildGuaranteedDropData(type);
    if (guaranteed != null) {
      entity.addDropRandom(guaranteed);
      attached = true;
    }
    DropData drop = buildDropData(type);
    if (drop != null) {
      entity.addDropRandom(drop);
      attached = true;
    }
    if (attached) {
      entity.getMemoryWithoutUpdate().set(SEEDED_FLAG, true);
    }
  }

  private static Set<String> eligibleTypes() {
    Set<String> types =
        new HashSet<String>(CqSettings.getStringList("cq_salvage_entity_types", DEFAULT_TYPES));
    types.addAll(CqSettings.getStringMap("cq_thematic_sources").keySet());
    types.addAll(CqSettings.getStringMap("cq_guaranteed_sources").keySet());
    return types;
  }

  static DropData buildDropData(String entityType) {
    List<CqCatalog.Furnishing> pool = poolFor(entityType);
    if (pool.isEmpty()) {
      return null;
    }
    float chance = CqSettings.tunable("cq_salvage_chance", CqDropMath.DEFAULT_SALVAGE_CHANCE);
    chance = Math.max(0f, Math.min(1f, chance));
    if (chance <= 0f) {
      return null;
    }
    CqData data = CqData.getInstance();
    int[] tierCount = new int[CqCatalog.Rarity.values().length];
    int[] tierOwned = new int[tierCount.length];
    float total = 0f;
    for (CqCatalog.Furnishing f : pool) {
      int r = f.getRarity().ordinal();
      tierCount[r]++;
      if (data.isOwned(f.getId())) {
        tierOwned[r]++;
      }
      float w = f.getRarity().getWeight();
      if (w > 0f) {
        total += w;
      }
    }
    if (total <= 0f) {
      return null;
    }

    DropData drop = new DropData();
    drop.chances = 1;
    drop.unaffectedByExtMode = !CqSurvival.findsCutOn();
    float live = 0f;
    for (CqCatalog.Furnishing f : pool) {
      float w = f.getRarity().getWeight();
      if (w <= 0f) {
        continue;
      }
      int r = f.getRarity().ordinal();
      float adjusted = CqDropMath.collectionAwareWeight(
          w, tierCount[r], tierOwned[r], data.isOwned(f.getId()));
      if (adjusted > 0f) {
        drop.addSpecialItem(f.getId(), adjusted * chance / total);
        live += adjusted;
      }
    }
    if (live <= 0f) {
      return null;
    }
    float nothing = 1f - chance * (live / total);
    if (nothing > 0f) {
      drop.addNothing(nothing);
    }
    return drop;
  }

  static List<CqCatalog.Furnishing> poolFor(String entityType) {
    List<CqCatalog.Furnishing> pool = new ArrayList<CqCatalog.Furnishing>();
    if (entityType != null
        && CqSettings.getStringList("cq_salvage_entity_types", DEFAULT_TYPES)
            .contains(entityType)) {
      pool.addAll(CqCatalog.pool(CqCatalog.Channel.SCAVENGE));
    }
    CqCatalog.Furnishing f = CqCatalog.resolveConfigured(
        CqSettings.getStringMap("cq_thematic_sources").get(entityType));
    if (f != null) {
      pool.add(f);
    }
    return pool;
  }

  static DropData buildGuaranteedDropData(String entityType) {
    if (entityType == null) {
      return null;
    }
    CqCatalog.Furnishing f = CqCatalog.resolveConfigured(
        CqSettings.getStringMap("cq_guaranteed_sources").get(entityType));
    if (f == null || CqData.getInstance().isOwned(f.getId())) {
      return null;
    }
    DropData drop = new DropData();
    drop.chances = 1;
    drop.unaffectedByExtMode = true;
    drop.addSpecialItem(f.getId(), 1f);
    return drop;
  }

  private static void removeOurDropData(SectorEntityToken entity) {
    List<DropData> dropRandom = entity.getDropRandom();
    if (dropRandom == null) {
      return;
    }
    for (Iterator<DropData> it = dropRandom.iterator(); it.hasNext();) {
      DropData data = it.next();
      if (data.getCustom() == null) {
        continue;
      }
      for (DropGroupRow row : data.getCustom().getItems()) {
        if (row.isSpecialItem() && CqCatalog.get(row.getSpecialItemId()) != null) {
          it.remove();
          break;
        }
      }
    }
  }

  private static void warnOnce(Throwable t) {
    if (warnedOnce) {
      return;
    }
    warnedOnce = true;
    Global.getLogger(CqSalvageSeeder.class).warn(
        "Captain's Quarters: salvage drop seeding failed; furnishings may not appear at special"
            + " locations this session.", t);
  }
}
