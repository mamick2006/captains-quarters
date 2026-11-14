package cq.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import cq.effects.pure.CqRoll;

public final class CqRolls {

  private CqRolls() {
  }

  public static int pay(float value, String... key) {
    if (!CqRoll.fractional(value)) {
      return CqRoll.pay(value, 0f);
    }
    return CqRoll.pay(value, CqRoll.unit(seed(key)));
  }

  public static String day() {
    CampaignClockAPI clock = Global.getSector().getClock();
    return clock.getCycle() + "." + clock.getMonth() + "." + clock.getDay();
  }

  public static String month() {
    CampaignClockAPI clock = Global.getSector().getClock();
    return clock.getCycle() + "." + clock.getMonth();
  }

  private static long seed(String... key) {
    SectorAPI sector = Global.getSector();
    String sectorSeed = sector == null ? "" : sector.getSeedString();
    String[] parts = new String[key.length + 1];
    parts[0] = sectorSeed;
    System.arraycopy(key, 0, parts, 1, key.length);
    return CqRoll.seed(parts);
  }
}
