package cq.campaign.acquire;

import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.util.WeightedRandomPicker;

import cq.campaign.CqData;
import cq.items.CqCatalog;

public final class CqDrops {

  private CqDrops() {
  }

  public static CqCatalog.Furnishing pickCollectionAware(List<CqCatalog.Furnishing> pool,
      Random random) {
    if (pool == null || pool.isEmpty()) {
      return null;
    }
    CqData data = CqData.getInstance();
    int[] tierCount = new int[CqCatalog.Rarity.values().length];
    int[] tierOwned = new int[tierCount.length];
    for (CqCatalog.Furnishing f : pool) {
      int r = f.getRarity().ordinal();
      tierCount[r]++;
      if (data.isOwned(f.getId())) {
        tierOwned[r]++;
      }
    }
    float totalMass = 0f;
    float liveMass = 0f;
    WeightedRandomPicker<CqCatalog.Furnishing> picker =
        new WeightedRandomPicker<CqCatalog.Furnishing>(random);
    for (CqCatalog.Furnishing f : pool) {
      float w = f.getRarity().getWeight();
      if (w <= 0f) {
        continue;
      }
      totalMass += w;
      int r = f.getRarity().ordinal();
      float adjusted = CqDropMath.collectionAwareWeight(
          w, tierCount[r], tierOwned[r], data.isOwned(f.getId()));
      if (adjusted > 0f) {
        picker.add(f, adjusted);
        liveMass += adjusted;
      }
    }
    if (totalMass <= 0f || liveMass <= 0f) {
      return null;
    }
    if (random.nextFloat() * totalMass >= liveMass) {
      return null;
    }
    return picker.pick();
  }
}
