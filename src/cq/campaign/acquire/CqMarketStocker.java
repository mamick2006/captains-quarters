package cq.campaign.acquire;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.SubmarketUpdateListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import cq.campaign.CqSettings;
import cq.campaign.CqSurvival;
import cq.items.CqCatalog;

public class CqMarketStocker implements SubmarketUpdateListener {

  public static final String STOCKED_BUCKET_KEY = "$cq_stocked_bucket";

  private boolean warnedOnce;

  @Override
  public void reportSubmarketCargoAndShipsUpdated(SubmarketAPI submarket) {
    try {
      if (submarket == null || submarket.getMarket() == null || submarket.getCargo() == null) {
        return;
      }
      if (!Submarkets.SUBMARKET_OPEN.equals(submarket.getSpecId())) {
        return;
      }
      MarketAPI market = submarket.getMarket();
      if (market.isPlanetConditionMarketOnly()) {
        return;
      }
      restock(market, submarket.getCargo());
    } catch (Throwable t) {
      if (!warnedOnce) {
        warnedOnce = true;
        Global.getLogger(CqMarketStocker.class).warn(
            "Captain's Quarters: market stocking failed; furnishings may not appear in market"
                + " stock this session.", t);
      }
    }
  }

  private void restock(MarketAPI market, CargoAPI cargo) {
    CampaignClockAPI clock = Global.getSector().getClock();
    long monthBucket = clock.getCycle() * 12L + clock.getMonth();
    MemoryAPI mem = market.getMemoryWithoutUpdate();
    if (mem.getLong(STOCKED_BUCKET_KEY) == monthBucket) {
      return;
    }
    mem.set(STOCKED_BUCKET_KEY, Long.valueOf(monthBucket));

    for (CargoStackAPI stack : cargo.getStacksCopy()) {
      if (!stack.isSpecialStack()) {
        continue;
      }
      SpecialItemData data = stack.getSpecialDataIfSpecial();
      CqCatalog.Furnishing f = data == null ? null : CqCatalog.get(data.getId());
      if (f == null) {
        continue;
      }
      if (f.getChannel() == CqCatalog.Channel.QUEST
          || f.getChannel() == CqCatalog.Channel.GUARANTEED) {
        continue;
      }
      cargo.removeItems(CargoItemType.SPECIAL, data, stack.getSize());
    }

    Random random = new Random(market.getId().hashCode() * 31L + monthBucket);

    if (random.nextFloat() >= carryChance(market.getSize())) {
      return;
    }
    CqCatalog.Furnishing unique = uniqueHomedAt(market.getId());
    if (unique != null) {
      if (!cq.campaign.CqData.getInstance().isOwned(unique.getId())) {
        cargo.addSpecial(new SpecialItemData(unique.getId(), null), 1);
      }
      return;
    }
    CqCatalog.Furnishing pick = pickMarketItem(random);
    if (pick != null) {
      cargo.addSpecial(new SpecialItemData(pick.getId(), null), 1);
    }
  }

  public static CqCatalog.Furnishing uniqueHomedAt(String marketId) {
    if (marketId == null) {
      return null;
    }
    for (CqCatalog.Furnishing f : CqCatalog.pool(CqCatalog.Channel.UNIQUE)) {
      if (marketId.equals(f.getHomeMarketId())) {
        return f;
      }
    }
    return null;
  }

  public static float carryChance(int marketSize) {
    float chance = CqSettings.tunable("cq_market_chance", CqDropMath.DEFAULT_MARKET_CHANCE);
    float step = CqSettings.getFloat("cq_market_size_step", 0.10f);
    float min = CqSettings.getFloat("cq_market_size_min", 0.70f);
    float max = CqSettings.getFloat("cq_market_size_max", 1.30f);
    return CqDropMath.survivalChance(CqDropMath.marketCarryChance(marketSize, chance, step, min, max),
        CqSurvival.findMult());
  }

  public static CqCatalog.Furnishing pickMarketItem(Random random) {
    return CqDrops.pickCollectionAware(CqCatalog.pool(CqCatalog.Channel.MARKET), random);
  }
}
