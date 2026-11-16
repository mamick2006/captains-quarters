package cq.campaign;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import com.fs.starfarer.api.Global;

import cq.newgame.pure.CqStartRules;

import lunalib.lunaSettings.LunaSettings;

public class CqSettings {

  public static final String MOD_ID = "captains_quarters";

  public static float getFloat(String key, float fallback) {
    if (Global.getSettings() == null) {
      return fallback;
    }
    return Global.getSettings().optFloat(key, fallback);
  }

  public static List<String> getStringList(String key, List<String> fallback) {
    if (Global.getSettings() == null) {
      return fallback;
    }
    try {
      JSONArray arr = Global.getSettings().getJSONArray(key);
      List<String> result = new ArrayList<String>();
      for (int i = 0; i < arr.length(); i++) {
        result.add(arr.getString(i));
      }
      return result;
    } catch (Exception e) {
      return fallback;
    }
  }

  public static java.util.Map<String, String> getStringMap(String key) {
    java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<String, String>();
    if (Global.getSettings() == null) {
      return result;
    }
    try {
      org.json.JSONObject obj = Global.getSettings().getJSONObject(key);
      java.util.Iterator<?> it = obj.keys();
      while (it.hasNext()) {
        String k = String.valueOf(it.next());
        result.put(k, obj.getString(k));
      }
    } catch (Exception e) {
    }
    return result;
  }

  public static int getInt(String key, int fallback) {
    if (Global.getSettings() == null) {
      return fallback;
    }
    return (int) Global.getSettings().optFloat(key, fallback);
  }

  public static String getString(String key, String fallback) {
    if (Global.getSettings() == null) {
      return fallback;
    }
    try {
      String v = Global.getSettings().getString(key);
      return v != null ? v : fallback;
    } catch (Exception e) {
      return fallback;
    }
  }

  public static String difficultyMode() {
    String v = null;
    if (lunaEnabled()) {
      v = LunaSettings.getString(MOD_ID, "cq_difficulty");
    }
    if (v == null || v.isEmpty()) {
      v = getString("cq_difficulty_default", CqDifficulty.AUTO);
    }
    return CqDifficulty.resolve(CqDifficulty.normalize(v), CqSurvival.isSurvivalCampaign());
  }

  public static float difficultyEffectMult() {
    String mode = difficultyMode();
    if (CqDifficulty.SURVIVAL.equals(mode)) {
      return getFloat("cq_difficulty_effect_mult_survival", 0.5f);
    }
    if (CqDifficulty.STORY.equals(mode)) {
      return getFloat("cq_difficulty_effect_mult_story", 2.0f);
    }
    return getFloat("cq_difficulty_effect_mult_normal", 1.0f);
  }

  public static long[] getLongArray(String key, long[] fallback) {
    if (Global.getSettings() == null) {
      return fallback;
    }
    try {
      JSONArray arr = Global.getSettings().getJSONArray(key);
      long[] result = new long[arr.length()];
      for (int i = 0; i < arr.length(); i++) {
        result[i] = arr.getLong(i);
      }
      return result;
    } catch (Exception e) {
      return fallback;
    }
  }

  public static float[] getFloatArray(String key) {
    if (Global.getSettings() == null) {
      return null;
    }
    try {
      JSONArray arr = Global.getSettings().getJSONArray(key);
      float[] result = new float[arr.length()];
      for (int i = 0; i < arr.length(); i++) {
        result[i] = (float) arr.getDouble(i);
      }
      return result;
    } catch (Exception e) {
      return null;
    }
  }

  private static boolean lunaEnabled() {
    return Global.getSettings() != null
        && Global.getSettings().getModManager() != null
        && Global.getSettings().getModManager().isModEnabled("lunalib");
  }

  public static long slotPrice(int purchaseIndex) {
    return CqSlotMath.priceOf(purchaseIndex, slotLadder(), slotPriceOverride(purchaseIndex));
  }

  public static long[] slotLadder() {
    return getLongArray("cq_slot_prices", CqSlotMath.DEFAULT_PRICES);
  }

  public static Long[] slotPriceOverrides(int steps) {
    Long[] out = new Long[Math.max(0, steps)];
    for (int i = 0; i < out.length; i++) {
      out[i] = slotPriceOverride(i);
    }
    return out;
  }

  private static Long slotPriceOverride(int purchaseIndex) {
    if (!lunaEnabled()) {
      return null;
    }
    Double v = LunaSettings.getDouble(MOD_ID, "cq_slot_price_" + (purchaseIndex + 1));
    return v == null ? null : Long.valueOf(v.longValue());
  }

  public static float tunable(String key, float fallback) {
    if (lunaEnabled()) {
      Double v = LunaSettings.getDouble(MOD_ID, key);
      if (v != null) {
        return v.floatValue();
      }
    }
    return getFloat(key, fallback);
  }

  public static CqStartRules.Budgets fleetBudgets() {
    return CqStartRules.Budgets.of(tunableNumber(CqStartRules.LONE_BUDGET_KEY),
        tunableNumber(CqStartRules.SMALL_BUDGET_KEY));
  }

  private static Number tunableNumber(String key) {
    if (lunaEnabled()) {
      try {
        Integer v = LunaSettings.getInt(MOD_ID, key);
        if (v != null) {
          return v;
        }
      } catch (Throwable t) {
      }
    }
    if (Global.getSettings() == null) {
      return null;
    }
    float v = Global.getSettings().optFloat(key, Float.NaN);
    return Float.isNaN(v) ? null : Float.valueOf(v);
  }

  public static boolean tunableBool(String key, boolean fallback) {
    if (lunaEnabled()) {
      Boolean v = LunaSettings.getBoolean(MOD_ID, key);
      if (v != null) {
        return v;
      }
    }
    if (Global.getSettings() == null) {
      return fallback;
    }
    return Global.getSettings().optBoolean(key, fallback);
  }
}
