package cq.effects;

import java.util.Arrays;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction.LineItemType;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction.ShipSaleInfo;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction.TransactionLineItem;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.util.Misc;

import cq.campaign.CqData;
import cq.campaign.CqStrings;
import cq.effects.pure.CqEffectLine;
import cq.effects.pure.CqTariffMath;
import cq.items.CqCatalog;
import cq.items.CqEffectText;

public final class CqTariffs {

  private static boolean warnedOnce;

  private CqTariffs() {
  }

  public static void register(SectorAPI sector) {
    warnedOnce = false;
    ListenerManagerAPI lm = sector.getListenerManager();
    if (!lm.hasListenerOfClass(Listener.class)) {
      lm.addListener(new Listener(), true);
    }
  }

  private static void pay(PlayerMarketTransaction transaction) {
    MarketAPI market = transaction.getMarket();
    if (market == null) {
      return;
    }
    CqData data = CqData.getInstance();
    List<CqCatalog.Furnishing> rows = CqOccasions.activeRows(data, CqCatalog.Kind.TARIFF_CAPPED);
    if (rows.isEmpty()) {
      return;
    }
    int n = 0;
    String[] ids = new String[rows.size()];
    float[] pcts = new float[rows.size()];
    float[] caps = new float[rows.size()];
    for (CqCatalog.Furnishing f : rows) {
      if (!inGroup(market, f.getArg())) {
        continue;
      }
      ids[n] = f.getId();
      pcts[n] = CqOccasions.pctOf(f, CqCatalog.Kind.TARIFF_CAPPED);
      caps[n] = CqOccasions.magOf(f, CqCatalog.Kind.TARIFF_CAPPED);
      n++;
    }
    if (n == 0) {
      return;
    }
    float paid = tariffPaid(transaction);
    if (paid <= 0f) {
      return;
    }
    CqTariffData month = thisMonth(data);
    long[] returned = new long[n];
    for (int i = 0; i < n; i++) {
      returned[i] = month.returnedBy(ids[i]);
    }
    long[] parts = CqTariffMath.rebate(paid, Arrays.copyOf(pcts, n), Arrays.copyOf(caps, n),
        returned);
    long total = CqTariffMath.total(parts);
    if (total <= 0L) {
      return;
    }
    for (int i = 0; i < n; i++) {
      month.addReturned(ids[i], parts[i]);
    }
    if (n > 1 && CqTariffMath.stopped(paid, Arrays.copyOf(pcts, n), Arrays.copyOf(caps, n),
        returned)) {
      month.markStopped();
    }
    Global.getSector().getPlayerFleet().getCargo().getCredits().add(total);
  }

  private static CqTariffData thisMonth(CqData data) {
    CqTariffData month = data.tariffs();
    CampaignClockAPI clock = Global.getSector().getClock();
    month.rollTo(CqTariffMath.monthIndex(clock.getCycle(), clock.getMonth()));
    return month;
  }

  private static void announce() {
    CqTariffData state = CqData.getInstance().tariffs();
    if (state.unannounced() <= 0L) {
      return;
    }
    long credits = state.takeUnannounced();
    List<String> paidBy = state.takeUnannouncedBy();
    boolean stopped = state.takeStopped();
    CampaignUIAPI ui = Global.getSector().getCampaignUI();
    if (ui != null) {
      String message = CqEffectText.credit(
          CqStrings.effects("tariff_msg_returned", Misc.getWithDGS(credits)), paidBy);
      ui.addMessage(CqEffectLine.limitNoted(message, stopped,
          CqStrings.effects("tariff_msg_limit"), CqTariffMath.limitPercent()),
          Misc.getPositiveHighlightColor());
    }
  }

  private static boolean dialogUp() {
    CampaignUIAPI ui = Global.getSector().getCampaignUI();
    return ui != null && ui.getCurrentInteractionDialog() != null;
  }

  private static boolean inGroup(MarketAPI market, String group) {
    String faction = market.getFactionId();
    switch (group) {
      case CqTariffMath.LUDDIC_MAJORITY:
        return market.hasCondition(Conditions.LUDDIC_MAJORITY);
      case CqTariffMath.INDEPENDENT:
        return Factions.INDEPENDENT.equals(faction);
      case CqTariffMath.MILITARY_BASE:
        return Misc.isMilitary(market);
      case CqTariffMath.PERSEAN_LEAGUE:
        return Factions.PERSEAN.equals(faction);
      case CqTariffMath.COMMISSION: {
        String commission = Misc.getCommissionFactionId();
        return commission != null && commission.equals(faction);
      }
      case CqTariffMath.HEAVY_INDUSTRY:
        return Misc.hasHeavyIndustry(market);
      case CqTariffMath.LUDDIC_CHURCH:
        return Factions.LUDDIC_CHURCH.equals(faction);
      case CqTariffMath.HEGEMONY:
        return Factions.HEGEMONY.equals(faction);
      default:
        return !group.isEmpty() && group.equals(faction);
    }
  }

  private static float tariffPaid(PlayerMarketTransaction transaction) {
    SubmarketAPI submarket = transaction.getSubmarket();
    if (!chargesTariff(submarket)) {
      return 0f;
    }
    float rate = submarket.getTariff();
    float paid = 0f;
    List<TransactionLineItem> items = transaction.getLineItems();
    for (TransactionLineItem item : items) {
      if (item.getItemType() != LineItemType.UPDATE) {
        paid += Math.abs(item.getTariff());
      }
    }
    for (ShipSaleInfo ship : transaction.getShipsBought()) {
      paid += CqTariffMath.tariffInBuyPrice(ship.getPrice(), rate);
    }
    for (ShipSaleInfo ship : transaction.getShipsSold()) {
      paid += CqTariffMath.tariffInSellPrice(ship.getPrice(), rate);
    }
    SettingsAPI settings = Global.getSettings();
    boolean plainBought = false;
    if (transaction.getBought() != null) {
      float buyMult = settings.getFloat("nonEconItemBuyPriceMult");
      for (CargoStackAPI stack : transaction.getBought().getStacksCopy()) {
        if (stack.isNull()) {
          continue;
        }
        if (stack.isSpecialStack()) {
          paid += rate * stack.getBaseValuePerUnit() * stack.getSize() * buyMult;
        } else {
          plainBought = true;
        }
      }
    }
    if (transaction.getSold() != null) {
      for (CargoStackAPI stack : transaction.getSold().getStacksCopy()) {
        if (!stack.isNull() && stack.isSpecialStack()) {
          paid += rate * (float) Math.floor(stack.getBaseValuePerUnit() * stack.getSize()
              * specialSellMult(stack, settings));
        }
      }
    }
    if (plainBought && items.isEmpty() && transaction.getCreditValue() < 0f) {
      paid += CqTariffMath.tariffInBuyPrice(-transaction.getCreditValue(), rate);
    }
    return paid;
  }

  private static boolean chargesTariff(SubmarketAPI submarket) {
    return submarket != null && submarket.getPlugin() != null && submarket.getTariff() > 0f
        && submarket.getPlugin().getTariffTextOverride() == null;
  }

  private static float specialSellMult(CargoStackAPI stack, SettingsAPI settings) {
    float mult = settings.getFloat("nonEconItemSellPriceMult");
    if (Misc.isExtended()) {
      SpecialItemData special = stack.getSpecialDataIfSpecial();
      if (special != null && Items.MODSPEC.equals(special.getId())) {
        mult *= settings.getFloat("ext_hullmodSellPriceMult");
      }
      if (stack.isBlueprintStack()) {
        mult *= settings.getFloat("ext_blueprintSellPriceMult");
      }
    }
    return mult;
  }

  private static void failed(Throwable t) {
    if (!warnedOnce) {
      warnedOnce = true;
      Global.getLogger(CqTariffs.class).warn(
          "Captain's Quarters: a tariff effect failed; wall art may not have returned a tariff"
              + " or said so.", t);
    }
  }

  private static final class Listener implements ColonyInteractionListener, EconomyTickListener {

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
      try {
        announce();
      } catch (Throwable t) {
        failed(t);
      }
    }

    @Override
    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {
    }

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
      try {
        pay(transaction);
      } catch (Throwable t) {
        failed(t);
      }
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
      try {
        if (CqData.getInstance().tariffs().unannounced() > 0L && !dialogUp()) {
          announce();
        }
      } catch (Throwable t) {
        failed(t);
      }
    }

    @Override
    public void reportEconomyMonthEnd() {
      try {
        thisMonth(CqData.getInstance());
      } catch (Throwable t) {
        failed(t);
      }
    }
  }
}
