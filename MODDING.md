# Adding furnishings from your own mod

Captain's Quarters reads its furnishings from `data/config/cq/cq_furnishings.csv`. The game merges that
path across every enabled mod. This document describes how you can add your own custom furnishings.

For a more complete example of how to do this, see `https://github.com/mamick2006/crossmod-demo`.

## Adding a furnishing

Add to `cq_furnishings.csv` something like:

```
id,name,category,rarity,channel,kind,mag,pct,kind2,mag2,pct2,plugin,when,arg
mymod_ledger,Purser's Ledger,cq_slot_desk,uncommon,market,survey_cost,10,0,,,,,,
```

A furnishing is also a special item. You have two options.

**Add a row** to your own `data/campaign/special_items.csv`:

- `plugin` is `cq.items.CqItemPlugin`. This mod becomes a hard dependency of your mod.
- `tags` carries `cq_furnishing` and the `cq_pool_*` tag matching your channel.
- `stack size` and `cargo space` are both 1.
- `icon` is your own 80x80 cargo icon.

**Or add no row**, and grant the furnishing yourself with `grant` below. This mod is not a hard dependency, and you can dynamically figure out if it's loaded to grant the furnishing. Set `channel` to `quest`, since the other channels drop the furnishing.

Either way, register the sticker sprite under `cq_f_<your id>` in your own `settings.json`:

```
"graphics": { "cq": { "cq_f_mymod_ledger": "graphics/mymod/furnishings/ledger.png" } },
```

## The columns

| Column | What it does |
|---|---|
| `id` | The furnishing's id, matching your `special_items.csv` row. |
| `name` | The display name. |
| `category` | One of `cq_slot_bunk`, `cq_slot_wall`, `cq_slot_pet`, `cq_slot_plant`, `cq_slot_desk`, `cq_slot_display`, `cq_slot_rug`, `cq_slot_light`, `cq_slot_galley`, `cq_slot_ent`. Groups the picker and sets the sticker's size. |
| `rarity` | `common`, `uncommon`, `rare` or `exceptional`. |
| `channel` | Where it comes from. See the channels below. |
| `kind` | A stock effect kind, or blank if you ship a `plugin`. |
| `mag` | The size of the effect, if you use a stock effect. |
| `pct` | Used only by `supply_use_capped`, `fuel_use_capped`, `tariff_capped`, `crew_returned` and `marines_returned`. |
| `kind2`, `mag2`, `pct2` | A second effect on the same furnishing. |
| `plugin` | Plugin for custom effects. |
| `when` | The occasion the effect applies on. Leave it blank and the effect always applies. See the occasions below. |
| `arg` | A detail some kinds need, such as which markets or which ships. See the event kinds below. |

## The channels

| Channel | Where it comes from |
|---|---|
| `market` | General market pool, which is weighted by rarity. Exceptional rows never appear here. |
| `scavenge` | Salvage pool. |
| `battle` | Post-battle random loot. |
| `thematic` | One location type's salvage pool. |
| `guaranteed` | Drops with certainty at one named source until you have it. |
| `unique` | Stocked only by the market named in `cq_item_<id>_home`. |
| `quest` | Never rolled. Grant it yourself, see below. Probably most cross-mod stuff will use this. |

## Stock effect kinds

No extra code, each is like the matching vanilla skill or hullmod.

Fleet and commander: `ground_ops`, `noncombat_crew_loss`, `salvage_not_rare`, `battle_salvage`,
`survey_cost`, `detected_at`, `sensor_range`, `repair_rate`, `marine_casualties`, `fuel_salvage`,
`eburn_cr_cost`, `go_dark_detect`, `custom_production`, `terrain_penalty`, `move_slow_bonus`,
`enemy_salvage_recovery`, `recovered_hull_max`, `recovered_cr_max`, `command_point_rate`,
`max_contacts`, `ground_support`, `dmod_reduction`, `command_points`, `rare_salvage`.

Per ship in the fleet: `supply_use_capped`, `fuel_use_capped`, `insta_repair`, `corona_resist`,
`cr_recovery`.

The ship you are flying: `peak_time`, `emp_taken`, `flux_dissipation`, `crew_combat_loss`,
`crew_hull_damage`, `cr_degradation`, `top_speed`, `vent_rate`, `flux_capacity`, `shield_damage`,
`overload_reduction`, `recoil`, `maneuverability`, `armor_damage_taken`, `weapon_range`, `max_cr`.

Officer experience: `officer_xp`.

## Occasions

A row in `cq_furnishings.csv` with something in `when` applies only on that occasion:

```
id,name,category,rarity,channel,kind,mag,pct,kind2,mag2,pct2,plugin,when,arg
mymod_lantern,Storm Lantern,cq_slot_light,common,market,sensor_range,10,0,,,,,transponder_off,
```

That is +10% sensor range while the transponder is off, and nothing while it is on. The tooltip adds the occasion to the effect line for you.

Leave `when` blank and the effect always applies. A row written before the column existed needs no change, and every kind above still loads and applies as it did.

Fleet occasions depend on what the fleet is doing. They take any kind above, and a `plugin` on the row follows the occasion too.

| `when` | The effect applies |
|---|---|
| `moving_slowly` | While the fleet is moving slowly. |
| `stopped` | While the fleet is stopped. |
| `sustained_burn` | While Sustained Burn is on. |
| `in_slipstream` | While inside a slipstream. |
| `in_hyperspace` | While in hyperspace. |
| `no_market_system` | In a star system with no market. Hidden markets are not counted. |
| `transponder_off` | While the transponder is off. |
| `running_dark` | While Go Dark is on. |
| `dark_day` | Once Go Dark has been on for a full day. |
| `supplies_short` | With less than 10 days of supplies. |
| `crew_short` | With less than 75% of the crew the ships need. |
| `recently_docked` | For 2 days after leaving a market you docked at. A planet nobody lives on, an abandoned station and a port that refused you docking clearance are not dockings. |

Battle occasions are read in battle, on the ship you are flying and only while you fly it. Every kind on the row has to be from "The ship you are flying" or be `time_flow`.

| `when` | The effect applies |
|---|---|
| `battle_opening` | For the ship's first 60 seconds in a battle. |
| `after_overload` | For 30 seconds after an overload or an active vent ends. |
| `outnumbered` | All battle, when the enemy side has more fleet points than yours. Never in the simulator. |
| `peak_time_out` | Once peak operating time has run out. |

A `plugin` on a row with a battle occasion is never applied.

The rest say which event pays one of the event kinds below.

| `when` | Pays |
|---|---|
| `after_battle` | After any battle. |
| `after_battle_won` | After a battle you win. |
| `after_battle_lost` | After a retreat or a defeat. |
| `after_raid` | After a ground raid. |

A `when` that is none of these, or one that does not fit the row's kinds, turns the row's stock effects off and writes a warning to the log.

## Event kinds

These need no code either, but no vanilla skill does them. Most pay when something happens, and `arg` says where or for whom. In `cq_furnishings.csv`:

```
id,name,category,rarity,channel,kind,mag,pct,kind2,mag2,pct2,plugin,when,arg
mymod_flag,Trade Flag,cq_slot_wall,uncommon,market,tariff_capped,3000,25,,,,,,mymod_faction
mymod_kit,Field Kit,cq_slot_plant,rare,scavenge,crew_returned,40,50,,,,,after_battle_won,
```

The flag refunds 25% of the tariffs you pay at markets of the faction `mymod_faction`, up to 3,000 credits a month. The kit returns 50% of the crew lost in a battle you win, up to 40.

| Kind | What it does | `mag` | `pct` | `when` | `arg` |
|---|---|---|---|---|---|
| `time_flow` | Time passes faster for the ship you are flying. | Percent faster. | | Blank for the whole battle, or a battle occasion. | |
| `tariff_capped` | Refunds part of each tariff you pay at the markets `arg` names. | The most credits refunded in a month. | Percent of the tariff. | Blank. | `luddic_majority`, `independent`, `military_base`, `persean_league`, `commission`, `heavy_industry`, `luddic_church`, `hegemony`, or a faction id. |
| `crew_returned` | Crew lost in a battle return to duty. | The most crew returned per battle. | Percent of the crew lost. | `after_battle`, `after_battle_won` or `after_battle_lost`. | Blank, `hyperspace` for battles fought there only, or `no_ship_destroyed` for battles in which none of your ships is destroyed. A ship disabled and not destroyed does not count as lost. |
| `marines_returned` | Marines lost return to duty. A ship battle almost never loses marines, so use `after_raid`. | The most marines returned. | Percent of the marines lost. | `after_raid`, or one of the three above. | As above. |
| `cr_regained` | The ship you flew regains part of the combat readiness the battle cost it. | Percent regained. | | `after_battle`, `after_battle_won` or `after_battle_lost`. | As above. |
| `relationship` | Contacts or a faction gain relationship with you when you dock at their market. Each one gains at most once in the days `arg` names. One that is hostile to you gains nothing. | Points. | | Blank. | Who gains: `contacts` for every contact at the market you dock at, `faction` for the faction of that market, or a contact type, `trade`, `underworld` or `military`, for each contact of that type at that market. Add `:` and a number for the days between gains, as in `underworld:15`. Without a number it is 30. |
| `new_contact_rel` | A new contact starts with extra relationship. | Points. | | Blank. | |
| `recovery_hull`, `recovery_cr`, `recovery_hull_cr` | A ship you recover has more hull, more combat readiness, or both. | Percentage points. | | Blank. | Which ships: `low_tech`, `midline`, `high_tech`, `pirate`, `destroyer`, `cruiser`, `capital`, `civilian`, `derelict`, or `dmods_3` for 3 or more d-mods. |
| `recovery_dmods` | A ship you recover has fewer d-mods. One d-mod always stays, and a ship the game marks unrestorable keeps them all. | D-mods removed. | | Blank. | The same groups. |
| `recovery_cargo` | A ship you recover comes with cargo, at most once every 30 days. | Units of the commodity. | | Blank. | A group and a commodity id, `group:commodity`, as in `derelict:fuel`. |
| `monthly_yield` | Adds a commodity to your cargo at month end. | Units a month. | | Blank. | The commodity id, such as `supplies`. |
| `experience` | Gives experience at month end, for each day the furnishing was slotted. | Points a day. For `marines` it is a percentage. | | Blank. | Who gains: `player`, `officers`, `officers_below_3`, `officer_lowest` (the two lowest-level officers), `officer_highest` (the two highest-level officers below maximum level) or `marines`. Each officer gains the full amount. |
| `dmod_repaired` | At every month end, repairs d-mods on your most expensive ship that has one. It can repair a ship's last d-mod, and a ship the game marks unrestorable keeps them all. | D-mods repaired a month. Under 1 it is a chance of one d-mod: 0.3 is 30% a month. | | Blank. | |

An `arg` the kind does not know pays nothing, and so does a `tariff_capped` row with no `arg`. Either one writes a warning to the log when the game loads, naming the row, and the furnishing shows no line for that effect. The one exception is a `tariff_capped` row that names a faction the game does not have, which still shows its line.

Recovered ships are ships you did not have before the battle or the salvage. Getting your own ship back does not count.

`recovery_dmods`, `dmod_repaired`, `relationship`, `new_contact_rel` and `command_points` can take a fraction. The whole part is paid and the fraction is rolled each time the effect pays: 0.5 fewer d-mods is 0 or 1 for each ship, and the tooltip reads "0-1 fewer d-mods". `move_slow_bonus` and `dmod_reduction` take a fraction the same way and are rolled once a calendar month. `max_contacts` is rounded up to a whole number and never rolled. The player's effect setting can halve or double a number, and the same roll applies to the result.

At one market, `tariff_capped` furnishings refund at most 60% of a tariff between them.

## Effects nothing above covers

Extend `cq.api.BaseCqFurnishingEffect`, add your class to the row's `plugin` column, and override only the methods you need. Add 
every stat write with the `modId` and unmodify that key in the matching clear.

```java
public class LedgerEffect extends BaseCqFurnishingEffect {

  @Override
  public void applyFleetAndCharacter(Furnishing f, String modId,
      MutableFleetStatsAPI fleet, MutableCharacterStatsAPI commander) {
    fleet.getFleetwideMaxBurnMod().modifyFlat(modId, 1f, f.getName());
  }

  @Override
  public void clearFleetAndCharacter(Furnishing f, String modId,
      MutableFleetStatsAPI fleet, MutableCharacterStatsAPI commander) {
    fleet.getFleetwideMaxBurnMod().unmodifyFlat(modId);
  }

  @Override
  public List<String> effectLines(Furnishing f) {
    return Collections.singletonList("+1 fleet burn level");
  }
}
```

## Granting one directly

If you want to manually grant a furnishing, call `cq.api.CaptainsQuartersAPI` through `Class.forName` plus
`MethodHandles`. Some methods that are relevant:

| Method | What it does |
|---|---|
| `boolean isAvailable()` | True when there is a campaign to grant into. |
| `boolean isKnownFurnishing(String id)` | True when some installed mod has that furnishing. |
| `boolean isOwned(String id)` | True when it is already collected. |
| `boolean grant(String id)` | Adds it to the collection. |
| `boolean giveAsCargo(String id, CargoAPI into)` | Puts it in a cargo hold instead, for the player to right-click. |

## Knowing whether the Manage screen is open

Nomad Flotilla, Liminal, Captain's Quarters and Project Arondight share one Manage button on the bottom bar and one Manage screen, so this covers all four mods.

Check `Global.getSector().getMemoryWithoutUpdate().getBoolean("$mgmt_open")`.

If the button conflicts with your mod, each mod has a setting that moves its own screen from the button to an intel entry: `nomad_mgmt_intel_only`, `liminal_mgmt_intel_only`, `cq_mgmt_intel_only` and `arondight_mgmt_intel_only`. The button is gone only when every installed one of the four has it on. A change takes effect on the next load.
