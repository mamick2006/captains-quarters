package cq.campaign.acquire;

public final class CqDropMath {

  private CqDropMath() {
  }

  public static final float DEFAULT_MARKET_CHANCE = 0.60f;
  public static final float DEFAULT_SALVAGE_CHANCE = 0.50f;
  public static final float DEFAULT_BATTLE_CHANCE_PER_FP = 0.00952381f;

  public static float marketCarryChance(int marketSize, float base, float step, float min,
      float max) {
    float sizeMult = 1f + step * (marketSize - 5);
    sizeMult = Math.max(min, Math.min(max, sizeMult));
    return base * sizeMult;
  }

  public static float battleChance(float destroyedFp, float perFp) {
    if (destroyedFp <= 0f || perFp <= 0f) {
      return 0f;
    }
    return Math.min(1f, destroyedFp * perFp);
  }

  public static final float VANILLA_SPECIAL_ITEM_FRACTION = 0.5f;

  public static float survivalFindMult(float specialItemFraction) {
    return Math.max(0f, Math.min(1f, specialItemFraction));
  }

  public static float survivalChance(float chance, float findMult) {
    float c = Math.max(0f, Math.min(1f, chance));
    return c * Math.max(0f, Math.min(1f, findMult));
  }

  public static float survivalBattleChance(float destroyedFp, float perFp, float findMult) {
    return battleChance(destroyedFp, perFp * Math.max(0f, Math.min(1f, findMult)));
  }

  public static final int QUEST_NOTHING = 0;
  public static final int QUEST_MARK_ONLY = 1;
  public static final int QUEST_MARK_AND_GRANT = 2;

  public static int questGrantAction(boolean completed, boolean alreadyMarked, boolean owned) {
    if (!completed || alreadyMarked) {
      return QUEST_NOTHING;
    }
    return owned ? QUEST_MARK_ONLY : QUEST_MARK_AND_GRANT;
  }

  public static float collectionAwareWeight(float rarityWeight, int tierCount, int tierOwned,
      boolean itemOwned) {
    if (itemOwned || rarityWeight <= 0f || tierCount <= 0) {
      return 0f;
    }
    int unowned = tierCount - tierOwned;
    if (unowned <= 0) {
      return 0f;
    }
    return rarityWeight * tierCount / unowned;
  }
}
