package cq.effects;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

import cq.campaign.CqData;
import cq.campaign.CqStrings;
import cq.effects.pure.CqCombatWindows;
import cq.effects.pure.CqWhen;
import cq.hullmods.CqQuartersHullmod;
import cq.items.CqCatalog;

public final class CqCombatOccasions {

  private CqCombatOccasions() {
  }

  private static boolean failed;

  public static void onShipCreated(ShipAPI ship, String hullmodId) {
    if (failed || ship.hasListenerOfClass(Tracker.class)) {
      return;
    }
    CqData data = CqData.getInstance();
    List<CqCatalog.Furnishing> onOccasion =
        CqOccasions.activeRows(data, CqWhen.Family.COMBAT);
    List<CqCatalog.Furnishing> timeFlow =
        CqOccasions.activeRows(data, CqCatalog.Kind.TIME_FLOW);
    if (onOccasion.isEmpty() && timeFlow.isEmpty()) {
      return;
    }
    List<Row> rows = new ArrayList<Row>(onOccasion.size() + timeFlow.size());
    int watched = 0;
    for (CqCatalog.Furnishing f : onOccasion) {
      if (f.getKind() == null && f.getKind2() == null) {
        continue;
      }
      CqWhen when = f.getWhen();
      rows.add(new Row(CqCombatWindows.bit(when), hullmodId + "_" + f.getId(),
          f.getKind(), kindMag(f, f.getKind()), f.getKind2(), kindMag(f, f.getKind2()),
          f.getName(), statusLine(f)));
      watched |= CqCombatWindows.bit(when);
    }
    for (CqCatalog.Furnishing f : timeFlow) {
      if (f.getWhen() != null) {
        continue;
      }
      rows.add(new Row(CqCombatWindows.ALWAYS, hullmodId + "_" + f.getId(),
          CqCatalog.Kind.TIME_FLOW, CqOccasions.magOf(f, CqCatalog.Kind.TIME_FLOW), null, 0f,
          f.getName(), null));
      watched |= CqCombatWindows.ALWAYS;
    }
    if (rows.isEmpty()) {
      return;
    }
    boolean outnumbered = (watched & CqCombatWindows.OUTNUMBERED) != 0 && outnumbered();
    String icon = Global.getSettings().getHullModSpec(hullmodId).getSpriteName();
    ship.addListener(new Tracker(ship, rows.toArray(new Row[rows.size()]), watched, outnumbered,
        icon));
  }

  public static void advance(ShipAPI ship, float amount) {
  }

  private static float kindMag(CqCatalog.Furnishing f, CqCatalog.Kind kind) {
    return kind == null ? 0f : CqOccasions.magOf(f, kind);
  }

  private static String statusLine(CqCatalog.Furnishing f) {
    String first = shortLine(f.getKind(), kindMag(f, f.getKind()));
    String second = shortLine(f.getKind2(), kindMag(f, f.getKind2()));
    if (first != null && second != null) {
      return first + CqStrings.effects("effect_join") + second;
    }
    if (first != null && f.getKind2() == null) {
      return first;
    }
    if (second != null && f.getKind() == null) {
      return second;
    }
    String whole = f.getEffectText();
    return whole.isEmpty() ? null : whole;
  }

  private static String shortLine(CqCatalog.Kind kind, float mag) {
    if (kind == null) {
      return null;
    }
    switch (kind) {
      case TOP_SPEED:
        return CqStrings.effects("hud_top_speed", number(mag));
      case ARMOR_DAMAGE_TAKEN:
        return CqStrings.effects("hud_armor_damage_taken", number(mag));
      case SHIELD_DAMAGE:
        return CqStrings.effects("hud_shield_damage", number(mag));
      case WEAPON_RANGE:
        return CqStrings.effects("hud_weapon_range", number(mag));
      case TIME_FLOW:
        return CqStrings.effects("hud_time_flow", number(mag));
      default:
        return null;
    }
  }

  private static String number(float v) {
    if (v == Math.floor(v)) {
      return String.valueOf((int) v);
    }
    return String.valueOf(Math.round(v * 10f) / 10f);
  }

  private static boolean outnumbered() {
    CampaignFleetAPI player = Global.getSector().getPlayerFleet();
    BattleAPI battle = player == null ? null : player.getBattle();
    if (battle == null) {
      return false;
    }
    return CqCombatWindows.outnumbered(points(battle.getPlayerSide()),
        points(battle.getNonPlayerSide()));
  }

  private static int[] points(List<CampaignFleetAPI> side) {
    if (side == null) {
      return null;
    }
    int[] points = new int[side.size()];
    for (int i = 0; i < points.length; i++) {
      points[i] = side.get(i).getFleetPoints();
    }
    return points;
  }

  private static void fail(Throwable t) {
    if (failed) {
      return;
    }
    failed = true;
    Global.getLogger(CqCombatOccasions.class).warn("Captain's Quarters: the in-battle furnishing"
        + " effects failed and are off for the rest of this session. Every other furnishing"
        + " effect is unaffected.", t);
  }

  private static final class Row {
    final int bit;
    final String modId;
    final CqCatalog.Kind kind;
    final float mag;
    final CqCatalog.Kind kind2;
    final float mag2;
    final String title;
    final String line;

    Row(int bit, String modId, CqCatalog.Kind kind, float mag, CqCatalog.Kind kind2, float mag2,
        String title, String line) {
      this.bit = bit;
      this.modId = modId;
      this.kind = kind;
      this.mag = mag;
      this.kind2 = kind2;
      this.mag2 = mag2;
      this.title = title;
      this.line = line;
    }

    void write(MutableShipStatsAPI stats, boolean on) {
      one(stats, kind, on ? mag : 0f);
      one(stats, kind2, on ? mag2 : 0f);
    }

    private void one(MutableShipStatsAPI stats, CqCatalog.Kind k, float m) {
      if (k == null) {
        return;
      }
      if (k != CqCatalog.Kind.TIME_FLOW) {
        CqQuartersHullmod.applyOne(stats, modId, k, m);
      } else if (m != 0f) {
        stats.getTimeMult().modifyMult(modId, CqCombatWindows.timeFlowMult(m));
      } else {
        stats.getTimeMult().unmodify(modId);
      }
    }
  }

  private static final class Tracker implements AdvanceableListener {
    private final ShipAPI ship;
    private final Row[] rows;
    private final int watched;
    private final int shown;
    private final boolean outnumbered;
    private final String icon;

    private float enteredAt = CqCombatWindows.NEVER;
    private float overloadEndedAt = CqCombatWindows.NEVER;
    private boolean wasOverloaded;
    private boolean wasPiloted;
    private boolean peakTimeOut;
    private float nextAt;
    private int held;
    private boolean off;

    Tracker(ShipAPI ship, Row[] rows, int watched, boolean outnumbered, String icon) {
      this.ship = ship;
      this.rows = rows;
      this.watched = watched;
      this.outnumbered = outnumbered;
      this.icon = icon;
      int shown = 0;
      for (Row r : rows) {
        if (r.line != null) {
          shown |= r.bit;
        }
      }
      this.shown = shown;
    }

    @Override
    public void advance(float amount) {
      if (off) {
        return;
      }
      try {
        step();
      } catch (Throwable t) {
        off = true;
        fail(t);
        try {
          write(0);
        } catch (Throwable ignored) {
        }
      }
    }

    private void step() {
      CombatEngineAPI engine = Global.getCombatEngine();
      if (engine == null) {
        return;
      }
      float now = engine.getTotalElapsedTime(false);
      boolean piloted = engine.getPlayerShip() == ship && !ship.isHulk();
      boolean overloaded = false;
      if ((watched & CqCombatWindows.AFTER_OVERLOAD) != 0) {
        FluxTrackerAPI flux = ship.getFluxTracker();
        overloaded = CqCombatWindows.stalled(flux.isOverloaded(), flux.isVenting());
      }
      if (piloted != wasPiloted || overloaded != wasOverloaded || now >= nextAt) {
        look(piloted, overloaded, now);
      }
      if ((held & shown) != 0) {
        for (Row r : rows) {
          if ((held & r.bit) != 0 && r.line != null) {
            engine.maintainStatusForPlayerShip(r.modId, icon, r.title, r.line, false);
          }
        }
      }
    }

    private void look(boolean piloted, boolean overloaded, float now) {
      if (enteredAt < 0f) {
        enteredAt = now;
      }
      overloadEndedAt =
          CqCombatWindows.overloadEndedAt(wasOverloaded, overloaded, overloadEndedAt, now);
      wasOverloaded = overloaded;
      wasPiloted = piloted;
      boolean readsPeakTime = piloted && (watched & CqCombatWindows.PEAK_TIME_OUT) != 0;
      if (readsPeakTime) {
        peakTimeOut = CqCombatWindows.peakTimeOut(ship.losesCRDuringCombat(),
            ship.getPeakTimeRemaining());
      }
      nextAt = CqCombatWindows.nextChange(enteredAt, overloadEndedAt, readsPeakTime, now);
      write(CqCombatWindows.holding(piloted, enteredAt, overloadEndedAt, outnumbered,
          peakTimeOut, now) & watched);
    }

    private void write(int holding) {
      if (holding == held) {
        return;
      }
      MutableShipStatsAPI stats = ship.getMutableStats();
      for (Row r : rows) {
        boolean on = (holding & r.bit) != 0;
        if (on != ((held & r.bit) != 0)) {
          r.write(stats, on);
        }
      }
      held = holding;
    }
  }
}
