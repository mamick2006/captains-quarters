package cq.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.RelationshipAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel.ContactState;

import cq.campaign.CqData;
import cq.campaign.CqEffectSlots;
import cq.campaign.CqStrings;
import cq.effects.pure.CqContactMath;
import cq.effects.pure.CqEffectLine;
import cq.items.CqCatalog;

public final class CqContacts {

  static final RepLevel CONTACT_LIMIT = RepLevel.COOPERATIVE;

  static final RepLevel FACTION_LIMIT = RepLevel.WELCOMING;

  static final RepLevel AT_WORST = RepLevel.INHOSPITABLE;

  private static volatile boolean warnedOnce;

  private CqContacts() {
  }

  public static void register(SectorAPI sector) {
    warnedOnce = false;
    ListenerManagerAPI listeners = sector.getListenerManager();
    if (!listeners.hasListenerOfClass(Listener.class)) {
      listeners.addListener(new Listener(), true);
    }
    CqContactData state = CqData.getInstance().contacts();
    if (!state.knowsContacts()) {
      state.setKnown(ids(held(sector), false));
    }
  }

  static final class Listener implements ColonyInteractionListener {

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
      try {
        onDock(market);
      } catch (Throwable t) {
        warnOnce(t);
      }
    }

    @Override
    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {
    }

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
    }
  }

  static void onDock(MarketAPI market) {
    if (!isPort(market)) {
      return;
    }
    SectorAPI sector = Global.getSector();
    CqData data = CqData.getInstance();
    CqContactData state = data.contacts();
    CampaignClockAPI clock = sector.getClock();
    long now = clock.getTimestamp();
    String day = CqRolls.day();

    List<ContactIntel> held = held(sector);
    List<CqCatalog.Furnishing> rows =
        CqOccasions.activeRows(data, CqCatalog.Kind.RELATIONSHIP);
    List<CqCatalog.Furnishing> welcomers =
        CqOccasions.activeRows(data, CqCatalog.Kind.NEW_CONTACT_REL);
    List<String> fresh = fresh(state, held);

    List<String> labels = labels(rows);
    labels.addAll(labels(welcomers));
    for (Target target : targets(held, market)) {
      int[] points = new int[rows.size() + welcomers.size()];
      for (int i = 0; i < rows.size(); i++) {
        points[i] = pay(state, clock, now, day, rows.get(i), target, market);
      }
      if (target.intel != null && fresh.contains(target.id)) {
        int[] welcome = welcomePoints(welcomers, target.id);
        System.arraycopy(welcome, 0, points, rows.size(), welcome.length);
      }
      gain(sector, target, CqContactMath.stacked(points), CqContactMath.payers(labels, points));
    }

    List<String> heldIds = ids(held, false);
    state.setKnown(heldIds);
    sweep(state, heldIds, sector, clock);
  }

  private static int pay(CqContactData state, CampaignClockAPI clock, long now, String day,
      CqCatalog.Furnishing row, Target target, MarketAPI market) {
    float mag = CqOccasions.magOf(row, CqCatalog.Kind.RELATIONSHIP);
    if (!(mag > 0f) || !names(row, target, market)) {
      return 0;
    }
    Long stamp = state.stamp(row.getId(), target.id);
    float since = stamp == null ? 0f : clock.getElapsedDaysSince(stamp.longValue());
    if (!CqContactMath.ready(stamp != null, since, CqContactMath.periodDays(row.getArg()))) {
      return 0;
    }
    state.setStamp(row.getId(), target.id, now);
    return CqRolls.pay(mag, row.getId(), target.id, day);
  }

  private static final class Target {
    final String id;
    final ContactIntel intel;
    final FactionAPI faction;

    Target(ContactIntel intel) {
      this.id = intel.getPerson().getId();
      this.intel = intel;
      this.faction = null;
    }

    Target(FactionAPI faction) {
      this.id = faction.getId();
      this.intel = null;
      this.faction = faction;
    }
  }

  private static List<Target> targets(List<ContactIntel> held, MarketAPI market) {
    List<Target> result = new ArrayList<Target>(held.size() + 1);
    for (ContactIntel intel : held) {
      if (isActive(intel) && canGain(intel.getPerson().getRelToPlayer(), CONTACT_LIMIT)) {
        result.add(new Target(intel));
      }
    }
    FactionAPI faction = market.getFaction();
    if (faction != null && !faction.isNeutralFaction() && !faction.isPlayerFaction()
        && canGain(faction.getRelToPlayer(), FACTION_LIMIT)) {
      result.add(new Target(faction));
    }
    return result;
  }

  private static int[] welcomePoints(List<CqCatalog.Furnishing> welcomers, String personId) {
    int[] points = new int[welcomers.size()];
    for (int i = 0; i < points.length; i++) {
      CqCatalog.Furnishing row = welcomers.get(i);
      points[i] = CqRolls.pay(CqOccasions.magOf(row, CqCatalog.Kind.NEW_CONTACT_REL),
          row.getId(), personId);
    }
    return points;
  }

  private static List<String> labels(List<CqCatalog.Furnishing> rows) {
    List<String> result = new ArrayList<String>(rows.size());
    for (CqCatalog.Furnishing row : rows) {
      result.add(row.getName());
    }
    return result;
  }

  private static void gain(SectorAPI sector, Target target, int points, List<String> payers) {
    if (target.intel != null) {
      gain(sector, target.intel.getPerson(), points, payers);
    } else {
      gain(sector, target.faction, points, payers);
    }
  }

  private static void gain(SectorAPI sector, PersonAPI person, int points,
      List<String> payers) {
    CustomRepImpact impact = impact(person.getRelToPlayer(), points, CONTACT_LIMIT);
    if (impact != null) {
      sector.adjustPlayerReputation(envelope(impact, reason(payers)), person);
    }
  }

  private static void gain(SectorAPI sector, FactionAPI faction, int points,
      List<String> payers) {
    CustomRepImpact impact = impact(faction.getRelToPlayer(), points, FACTION_LIMIT);
    if (impact != null) {
      sector.adjustPlayerReputation(envelope(impact, reason(payers)), faction.getId());
    }
  }

  private static String reason(List<String> payers) {
    if (payers.isEmpty()) {
      return null;
    }
    return CqStrings.effects("contact_gain_reason", CqEffectLine.listed(payers,
        CqStrings.effects("window_comma"), CqStrings.effects("window_join")));
  }

  private static int taken(RelationshipAPI rel, int points, RepLevel limit) {
    if (rel.isHostile()) {
      return 0;
    }
    return CqContactMath.taken(RepLevel.getRepInt(rel.getRel()), points,
        RepLevel.getRepInt(limit.getMax()));
  }

  private static boolean canGain(RelationshipAPI rel, RepLevel limit) {
    return taken(rel, 1, limit) > 0;
  }

  private static CustomRepImpact impact(RelationshipAPI rel, int points, RepLevel limit) {
    int fits = taken(rel, points, limit);
    if (fits <= 0) {
      return null;
    }
    CustomRepImpact impact = new CustomRepImpact();
    impact.delta = fits * CqContactMath.POINT;
    impact.limit = limit;
    impact.requireAtWorst = AT_WORST;
    return impact;
  }

  private static RepActionEnvelope envelope(CustomRepImpact impact, String reason) {
    return new RepActionEnvelope(RepActions.CUSTOM, impact, null, null, false, true, reason);
  }

  private static boolean isPort(MarketAPI market) {
    return CqOccasions.docked(market);
  }

  private static boolean names(CqCatalog.Furnishing row, Target target, MarketAPI market) {
    String who = CqContactMath.who(row.getArg());
    boolean ofFaction = CqContactMath.WHO_FACTION.equals(who);
    if (target.intel == null || ofFaction) {
      return target.intel == null && ofFaction;
    }
    ContactIntel intel = target.intel;
    SectorEntityToken home = intel.getMapLocation(null);
    boolean here = (home != null && home == market.getPrimaryEntity())
        || intel.getPerson().getMarket() == market;
    if (!here) {
      return false;
    }
    if (CqContactMath.WHO_CONTACTS.equals(who)) {
      return true;
    }
    String tag = tagOf(who);
    return tag != null && intel.getPerson().hasTag(tag);
  }

  private static String tagOf(String who) {
    if (CqContactMath.GROUP_TRADE.equals(who)) {
      return Tags.CONTACT_TRADE;
    }
    if (CqContactMath.GROUP_UNDERWORLD.equals(who)) {
      return Tags.CONTACT_UNDERWORLD;
    }
    if (CqContactMath.GROUP_MILITARY.equals(who)) {
      return Tags.CONTACT_MILITARY;
    }
    return null;
  }

  private static List<ContactIntel> held(SectorAPI sector) {
    List<ContactIntel> result = new ArrayList<ContactIntel>();
    for (IntelInfoPlugin intel : sector.getIntelManager().getIntel(ContactIntel.class)) {
      if (!(intel instanceof ContactIntel) || intel.isEnding() || intel.isEnded()) {
        continue;
      }
      ContactIntel contact = (ContactIntel) intel;
      ContactState state = contact.getState();
      boolean has = state == ContactState.NON_PRIORITY || state == ContactState.PRIORITY
          || state == ContactState.SUSPENDED;
      if (has && contact.getPerson() != null && contact.getPerson().getId() != null) {
        result.add(contact);
      }
    }
    return result;
  }

  private static boolean isActive(ContactIntel intel) {
    return intel.getState() != ContactState.SUSPENDED;
  }

  private static List<String> fresh(CqContactData state, List<ContactIntel> held) {
    if (!state.knowsContacts()) {
      state.setKnown(ids(held, false));
      return new ArrayList<String>();
    }
    return CqContactMath.unseen(state.known(), ids(held, true));
  }

  private static List<String> ids(List<ContactIntel> held, boolean activeOnly) {
    List<String> result = new ArrayList<String>(held.size());
    for (ContactIntel intel : held) {
      if (!activeOnly || isActive(intel)) {
        result.add(intel.getPerson().getId());
      }
    }
    return result;
  }

  private static void sweep(CqContactData state, List<String> heldIds, SectorAPI sector,
      CampaignClockAPI clock) {
    Map<String, LinkedHashMap<String, Long>> stamps = state.stamps();
    if (stamps == null) {
      return;
    }
    Iterator<Map.Entry<String, LinkedHashMap<String, Long>>> rows = stamps.entrySet().iterator();
    while (rows.hasNext()) {
      Map.Entry<String, LinkedHashMap<String, Long>> entry = rows.next();
      CqCatalog.Furnishing row = CqCatalog.get(entry.getKey());
      boolean pays = row != null && CqEffectSlots.carries(row.getKind(), row.getKind2(),
          CqCatalog.Kind.RELATIONSHIP);
      int period = pays ? CqContactMath.periodDays(row.getArg()) : 0;
      boolean ofFaction = pays
          && CqContactMath.WHO_FACTION.equals(CqContactMath.who(row.getArg()));
      Iterator<Map.Entry<String, Long>> targets = entry.getValue().entrySet().iterator();
      while (targets.hasNext()) {
        Map.Entry<String, Long> target = targets.next();
        boolean there = ofFaction ? sector.getFaction(target.getKey()) != null
            : heldIds.contains(target.getKey());
        float since = clock.getElapsedDaysSince(target.getValue().longValue());
        if (!CqContactMath.keepStamp(period, since, there)) {
          targets.remove();
        }
      }
      if (entry.getValue().isEmpty()) {
        rows.remove();
      }
    }
  }

  private static void warnOnce(Throwable t) {
    if (!warnedOnce) {
      warnedOnce = true;
      Global.getLogger(CqContacts.class).warn("Captain's Quarters: a contact effect failed on"
          + " one event. Every other furnishing effect is unaffected.", t);
    }
  }
}
