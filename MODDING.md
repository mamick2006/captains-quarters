# Adding furnishings from your own mod

Captain's Quarters reads its furnishings from `data/config/cq/cq_furnishings.csv`. The game merges that
path across every enabled mod. This document describes how you can add your own custom furnishings.

For a more complete example of how to do this, see `https://github.com/mamick2006/crossmod-demo`.

## Adding a furnishing

Add to `cq_furnishings.csv` something like:

```
id,name,category,rarity,channel,kind,mag,pct,kind2,mag2,pct2,plugin
mymod_ledger,Purser's Ledger,cq_slot_desk,uncommon,market,survey_cost,10,0,,,,
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
| `pct` | Used only by `supply_use_capped` and `fuel_use_capped`. |
| `kind2`, `mag2`, `pct2` | A second effect on the same furnishing. |
| `plugin` | Plugin for custom effects. |

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
`own_salvage_recovery`, `recovered_hull_max`, `recovered_cr_max`, `command_point_rate`,
`max_contacts`, `ground_support`, `dmod_reduction`.

Per ship in the fleet: `supply_use_capped`, `fuel_use_capped`, `insta_repair`, `corona_resist`,
`cr_recovery`.

The ship you are flying: `peak_time`, `emp_taken`, `flux_dissipation`, `crew_combat_loss`,
`crew_hull_damage`, `cr_degradation`, `top_speed`, `vent_rate`, `flux_capacity`, `shield_damage`,
`overload_reduction`, `recoil`, `maneuverability`. 

Officer experience: `officer_xp`.

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
