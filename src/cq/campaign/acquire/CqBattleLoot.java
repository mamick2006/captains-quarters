package cq.campaign.acquire;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.DataForEncounterSide;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.DataForEncounterSide.OfficerEngagementData;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.FleetMemberData;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.Status;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;

import cq.campaign.CqData;
import cq.campaign.CqEffectSlots;
import cq.campaign.CqSettings;
import cq.campaign.CqSurvival;
import cq.effects.CqOccasions;
import cq.items.CqCatalog;

public class CqBattleLoot extends BaseCampaignEventListener {

  private boolean warnedOnce;

  public CqBattleLoot() {
    super(false);
  }

  @Override
  public void reportEncounterLootGenerated(FleetEncounterContextPlugin plugin, CargoAPI loot) {
    try {
      if (!(plugin instanceof FleetEncounterContext) || loot == null) {
        return;
      }
      FleetEncounterContext context = (FleetEncounterContext) plugin;
      BattleAPI battle = context.getBattle();
      CampaignFleetAPI winner = context.getWinner();
      if (battle == null || winner == null || !battle.isPlayerSide(battle.getSideFor(winner))) {
        return;
      }
      DataForEncounterSide winnerData = context.getWinnerData();
      if (winnerData == null) {
        return;
      }

      List<FleetMemberAPI> destroyed = destroyedEnemies(winnerData);
      String dropId = rollDrop(fpOf(destroyed), new Random());
      if (dropId != null) {
        loot.addSpecial(new SpecialItemData(dropId, null), 1);
      }
      grantGuaranteedDrops(destroyed, loot);

      grantOfficerXpBonus(context, winnerData);
    } catch (Throwable t) {
      if (!warnedOnce) {
        warnedOnce = true;
        Global.getLogger(CqBattleLoot.class).warn(
            "Captain's Quarters: post-battle furnishing roll failed; battle salvage finds may"
                + " not appear this session.", t);
      }
    }
  }

  static List<FleetMemberAPI> destroyedEnemies(DataForEncounterSide winnerData) {
    List<FleetMemberAPI> destroyed = new ArrayList<FleetMemberAPI>();
    for (FleetMemberData casualty : winnerData.getEnemyCasualties()) {
      if (casualty.getMember() == null) {
        continue;
      }
      Status status = casualty.getStatus();
      if (status == Status.DISABLED || status == Status.DESTROYED) {
        destroyed.add(casualty.getMember());
      }
    }
    return destroyed;
  }

  public static float fpOf(List<FleetMemberAPI> destroyed) {
    float fp = 0f;
    for (FleetMemberAPI member : destroyed) {
      fp += member.getFleetPointCost();
    }
    return fp;
  }

  public static float chanceFor(float destroyedFp) {
    float perFp = CqSettings.tunable("cq_battle_chance_per_fp",
        CqDropMath.DEFAULT_BATTLE_CHANCE_PER_FP);
    return CqDropMath.survivalBattleChance(destroyedFp, perFp, CqSurvival.findMult());
  }

  static void grantGuaranteedDrops(List<FleetMemberAPI> destroyed, CargoAPI loot) {
    Map<String, String> sources = CqSettings.getStringMap("cq_guaranteed_sources");
    if (sources.isEmpty()) {
      return;
    }
    CqData data = CqData.getInstance();
    java.util.Set<String> granted = new java.util.HashSet<String>();
    for (FleetMemberAPI member : destroyed) {
      String hullId = member.getHullSpec() != null
          ? member.getHullSpec().getBaseHullId() : member.getHullId();
      String configured = sources.get(hullId);
      if (configured == null || !granted.add(configured)) {
        continue;
      }
      CqCatalog.Furnishing f = CqCatalog.resolveConfigured(configured);
      if (f == null || data.isOwned(f.getId())) {
        continue;
      }
      loot.addSpecial(new SpecialItemData(f.getId(), null), 1);
    }
  }

  public static String rollDrop(float destroyedFp, Random random) {
    if (random.nextFloat() >= chanceFor(destroyedFp)) {
      return null;
    }
    CqCatalog.Furnishing pick =
        CqDrops.pickCollectionAware(CqCatalog.pool(CqCatalog.Channel.BATTLE), random);
    return pick != null ? pick.getId() : null;
  }

  public static float slottedOfficerXpPercent() {
    float pct = 0f;
    java.util.Set<String> seen = new java.util.HashSet<String>();
    for (String itemId : CqOccasions.payingItemIds(CqData.getInstance())) {
      if (!seen.add(itemId)) {
        continue;
      }
      CqCatalog.Furnishing f = CqCatalog.get(itemId);
      if (f != null) {
        pct += CqEffectSlots.pick(f.getKind(), f.getKind2(), CqCatalog.Kind.OFFICER_XP,
            f.getEffectiveMag(), f.getEffectiveMag2());
      }
    }
    return pct;
  }

  private static void grantOfficerXpBonus(FleetEncounterContext context,
      DataForEncounterSide winnerData) {
    float pct = slottedOfficerXpPercent();
    if (pct <= 0f) {
      return;
    }
    DataForEncounterSide loserData = context.getLoserData();
    if (loserData == null) {
      return;
    }
    float fpTotal = 0f;
    for (FleetMemberData casualty : loserData.getOwnCasualties()) {
      if (casualty.getMember() == null || casualty.getMember().getCaptain() == null) {
        continue;
      }
      float fp = casualty.getMember().getFleetPointCost();
      fp *= 1f + casualty.getMember().getCaptain().getStats().getLevel() / 5f;
      fpTotal += fp;
    }
    float xp = fpTotal * 250f * 2f;
    xp *= Math.max(1f, context.getDifficulty());
    xp *= context.computePlayerContribFraction();
    xp *= Global.getSettings().getFloat("xpGainMult");
    if (xp <= 0f) {
      return;
    }

    Map<PersonAPI, OfficerEngagementData> officers = winnerData.getOfficerData();
    if (officers == null || officers.isEmpty()) {
      return;
    }
    float maxDeployed = winnerData.getMaxTimeDeployed();
    if (maxDeployed < 1f) {
      maxDeployed = 1f;
    }
    float num = officers.size();
    for (Map.Entry<PersonAPI, OfficerEngagementData> e : officers.entrySet()) {
      OfficerEngagementData engagement = e.getValue();
      if (engagement == null || engagement.sourceFleet == null
          || !engagement.sourceFleet.isPlayerFleet()) {
        continue;
      }
      OfficerDataAPI officer = engagement.sourceFleet.getFleetData().getOfficerData(e.getKey());
      if (officer == null) {
        continue;
      }
      float f = engagement.timeDeployed / maxDeployed;
      f = Math.max(0f, Math.min(1f, f));
      long bonus = (long) (f * xp * (pct / 100f) / num);
      if (bonus > 0) {
        officer.addXP(bonus);
      }
    }
  }
}
