package cq.newgame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FleetInflater;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.util.Misc;

import cq.newgame.pure.CqStartRules;

public final class CqFleetStart {

  private CqFleetStart() {
  }

  static final String PLACEHOLDER_VARIANT = "hammerhead_Balanced";

  public static void confirm(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap,
      CqStartRules.Flavour flavour, boolean officer, List<String> variantIds, boolean chooseLocation) {
    MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
    CharacterCreationData data = (CharacterCreationData) local.get("$characterData");
    for (String v : variantIds) {
      data.addStartingFleetMember(v, FleetMemberType.SHIP);
    }
    boolean withOfficer = officer && flavour.officerAllowed;
    if (withOfficer) {
      local.set("$ngcAddOfficer", true);
    }
    data.addScript(new Outfit(flavour, false));
    FireBest.fire(null, dialog, memoryMap, "NGCSelectDifficulty");
    data.addScript(new Settle(variantIds.size(), chooseLocation));
  }

  public static final class Outfit implements Script {
    private final CqStartRules.Flavour flavour;
    private final boolean nex;

    public Outfit(CqStartRules.Flavour flavour, boolean nex) {
      this.flavour = flavour;
      this.nex = nex;
    }

    @Override
    public void run() {
      CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
      if (fleet == null) {
        return;
      }
      outfit(fleet, flavour, new Random());
      if (nex) {
        fleet.getMemoryWithoutUpdate().set("$nex_addedStartingDMods", true, 5f);
      }
    }
  }

  public static void outfit(CampaignFleetAPI fleet, CqStartRules.Flavour flavour, Random random) {
    DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
    p.quality = flavour.quality;
    p.mode = ShipPickMode.PRIORITY_THEN_ALL;
    p.persistent = false;
    p.seed = random.nextLong();
    p.factionId = Factions.INDEPENDENT;
    if (flavour.maxTier >= 0) {
      p.maxTier = flavour.maxTier;
    }
    p.blockHullmodsWithItemReqs = true;
    p.timestamp = null;

    List<FleetMemberAPI> skipped = skippedByInflater(fleet);

    FleetInflater inflater = Misc.getInflater(fleet, p);
    fleet.setFaction(Factions.INDEPENDENT);
    inflater.inflate(fleet);
    fleet.setInflater(null);
    fleet.setFaction(Factions.PLAYER);

    float average = DefaultFleetInflater.getAverageDmodsForQuality(flavour.quality);
    for (FleetMemberAPI m : skipped) {
      addDModsAsInflater(m, average, random);
    }

    fleet.getFleetData().syncIfNeeded();
    fleet.getCargo().sort();
    if (fleet.getFlagship() != null) {
      fleet.getFlagship().setCaptain(Global.getSector().getCharacterData().getPerson());
      fleet.getFlagship().updateStats();
    }
  }

  public static List<FleetMemberAPI> skippedByInflater(CampaignFleetAPI fleet) {
    List<FleetMemberAPI> out = new ArrayList<FleetMemberAPI>();
    if (fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_FORCE_AUTOFIT_ON_NO_AUTOFIT_SHIPS)) {
      return out;
    }
    for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
      if (noAutofit(m.getHullSpec().getTags())
          || (m.getVariant() != null && noAutofit(m.getVariant().getTags()))) {
        out.add(m);
      }
    }
    return out;
  }

  private static boolean noAutofit(Collection<String> tags) {
    return tags != null && (tags.contains(Tags.TAG_NO_AUTOFIT)
        || tags.contains(Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER));
  }

  public static void addDModsAsInflater(FleetMemberAPI member, float averageDMods, Random random) {
    if (member.isStation()) {
      return;
    }
    ShipVariantAPI v = member.getVariant().clone();
    v.setSource(VariantSource.REFIT);
    v.setHullVariantId(Misc.genUID());
    member.setVariant(v, false, false);
    int add = DefaultFleetInflater.getNumDModsToAdd(v, averageDMods, random);
    if (add > 0) {
      DModManager.setDHull(v);
      DModManager.addDMods(member, true, add, random);
    }
  }

  public static final class Provision implements Script {
    private final int chosen;
    private final boolean officer;

    public Provision(int chosen, boolean officer) {
      this.chosen = chosen;
      this.officer = officer;
    }

    @Override
    public void run() {
      CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
      if (fleet == null) {
        return;
      }
      provision(fleet, chosen, officer);
    }
  }

  public static void provision(CampaignFleetAPI fleet, int chosen, boolean officer) {
    CargoAPI cargo = fleet.getCargo();
    cargo.initPartialsIfNeeded();
    cargo.removeSupplies(cargo.getSupplies());
    cargo.removeCommodity(Commodities.HEAVY_MACHINERY,
        cargo.getCommodityQuantity(Commodities.HEAVY_MACHINERY));
    cargo.addSupplies(10);
    cargo.addCommodity(Commodities.HEAVY_MACHINERY, 10);
    float[] extra = CqStartRules.startMachineryAndSupplies(cargo.getSpaceLeft());
    if (extra[0] > 0) {
      cargo.addCommodity(Commodities.HEAVY_MACHINERY, extra[0]);
    }
    if (extra[1] > 0) {
      cargo.addSupplies(extra[1]);
    }
    addMercuryIfShort(fleet);
    settle(fleet, chosen);
    fleet.getFleetData().ensureHasFlagship();
    if (officer) {
      addStartOfficer(fleet);
    }
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
    }
    fleet.getFleetData().setSyncNeeded();
  }

  static void addMercuryIfShort(CampaignFleetAPI fleet) {
    if (fleet.getCargo().getMaxPersonnel() >= fleet.getFleetData().getMinCrew()) {
      return;
    }
    fleet.getFleetData().addFleetMember("mercury_Standard");
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      if (!"mercury".equals(member.getHullId())) {
        continue;
      }
      ShipVariantAPI v = member.getVariant().clone();
      v.setSource(VariantSource.REFIT);
      v.setHullVariantId(Misc.genUID());
      v.clearWeapons();
      v.addPermaMod(HullMods.DEGRADED_DRIVE_FIELD);
      v.addPermaMod(HullMods.COMP_STORAGE);
      member.setVariant(v, false, false);
      break;
    }
    fleet.getFleetData().setSyncNeeded();
    fleet.forceSync();
  }

  static void addStartOfficer(CampaignFleetAPI fleet) {
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      if (member.isFlagship()) {
        continue;
      }
      PersonAPI person = Global.getSector().getPlayerFaction().createRandomPerson(new Random());
      person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
      person.setRankId(Ranks.SPACE_LIEUTENANT);
      person.setPostId(Ranks.POST_OFFICER);
      person.setPersonality(Personalities.STEADY);
      person.getStats().refreshCharacterStatsEffects();
      member.setCaptain(person);
      fleet.getFleetData().addOfficer(person);
      return;
    }
  }

  static final class Settle implements Script {
    private final int chosen;
    private final boolean chooseLocation;

    Settle(int chosen, boolean chooseLocation) {
      this.chosen = chosen;
      this.chooseLocation = chooseLocation;
    }

    @Override
    public void run() {
      CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
      if (fleet == null) {
        return;
      }
      settle(fleet, chosen);
      if (chooseLocation) {
        Global.getSector().addTransientScript(new CqStartLocationPrompt());
      }
    }
  }

  public static Script locationPrompt() {
    return new Script() {
      @Override
      public void run() {
        Global.getSector().addTransientScript(new CqStartLocationPrompt());
      }
    };
  }

  public static void settle(CampaignFleetAPI fleet, int chosen) {
    CargoAPI cargo = fleet.getCargo();
    List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
    boolean mercuryAdded = members.size() > chosen;
    int maxPersonnel = (int) cargo.getMaxPersonnel();
    int crew;
    if (mercuryAdded) {
      float min = 0f;
      for (int i = 0; i < chosen && i < members.size(); i++) {
        min += members.get(i).getMinCrew();
      }
      crew = Math.min((int) min, maxPersonnel);
    } else {
      crew = CqStartRules.startCrew((int) fleet.getFleetData().getMinCrew(), maxPersonnel);
    }
    cargo.removeCrew(cargo.getCrew());
    cargo.addCrew(crew);

    float placeholder = 0f;
    try {
      placeholder = Global.getFactory().createFleetMember(FleetMemberType.SHIP, PLACEHOLDER_VARIANT)
          .getFuelCapacity();
    } catch (Throwable missing) {
      placeholder = 0f;
    }
    float fuel = CqStartRules.startFuel(placeholder, cargo.getMaxFuel());
    cargo.removeFuel(cargo.getFuel());
    cargo.addFuel(fuel);
  }

  public static List<MarketAPI> startMarkets() {
    List<MarketAPI> out = new ArrayList<MarketAPI>();
    SectorAPI sector = Global.getSector();
    if (sector == null || sector.getEconomy() == null) {
      return out;
    }
    for (MarketAPI market : sector.getEconomy().getMarketsCopy()) {
      if (market.isHidden()) continue;
      if (market.getFaction().isHostileTo(Factions.PLAYER)) continue;
      if (market.getStarSystem() == null) continue;
      if (!market.getStarSystem().hasTag(Tags.THEME_CORE_POPULATED)) continue;
      out.add(market);
    }
    return out;
  }

  public static void moveTo(CampaignFleetAPI fleet, MarketAPI market) {
    SectorAPI sector = Global.getSector();
    sector.setCurrentLocation(market.getContainingLocation());
    if (fleet.getContainingLocation() != null) {
      fleet.getContainingLocation().removeEntity(fleet);
    }
    market.getContainingLocation().addEntity(fleet);
    Vector2f loc = Misc.getPointAtRadius(market.getPrimaryEntity().getLocation(),
        250f + StarSystemGenerator.random.nextFloat() * 500f);
    fleet.setLocation(loc.x, loc.y);
    pushHostiles(fleet, 2000f);
  }

  static void pushHostiles(CampaignFleetAPI player, float range) {
    for (CampaignFleetAPI other : player.getContainingLocation().getFleets()) {
      if (other == player || other.getBattle() != null || other.getOrbit() != null) continue;
      if (!other.isHostileTo(player)) continue;
      float d = Misc.getDistance(player.getLocation(), other.getLocation());
      if (d >= range) continue;
      float angle = Misc.getAngleInDegrees(player.getLocation(), other.getLocation());
      Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
      dir.scale(range);
      Vector2f.add(dir, other.getLocation(), dir);
      other.setLocation(dir.x, dir.y);
    }
  }
}
