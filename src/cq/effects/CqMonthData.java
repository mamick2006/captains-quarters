package cq.effects;

import java.util.HashMap;
import java.util.Map;

public class CqMonthData {

  private long lastStamp;
  private double monthDays;
  private Map<String, Double> accrued;
  private Map<String, Double> repairAccrued;
  private Map<String, Double> carry;
  private Map<String, Long> activeSince;

  public long getLastStamp() {
    return lastStamp;
  }

  public void setLastStamp(long stamp) {
    lastStamp = stamp;
  }

  public double getMonthDays() {
    return monthDays;
  }

  public void addMonthDays(double days) {
    monthDays += days;
  }

  public Map<String, Double> accrued() {
    if (accrued == null) {
      accrued = new HashMap<String, Double>();
    }
    return accrued;
  }

  public void addAccrued(String itemId, double amount) {
    if (amount <= 0) {
      return;
    }
    Double was = accrued().get(itemId);
    accrued().put(itemId, (was == null ? 0 : was) + amount);
  }

  public Map<String, Double> repairAccrued() {
    if (repairAccrued == null) {
      repairAccrued = new HashMap<String, Double>();
    }
    return repairAccrued;
  }

  public void addRepairAccrued(String itemId, double amount) {
    if (amount <= 0) {
      return;
    }
    Double was = repairAccrued().get(itemId);
    repairAccrued().put(itemId, (was == null ? 0 : was) + amount);
  }

  public double getCarry(String itemId) {
    if (carry == null) {
      return 0;
    }
    Double v = carry.get(itemId);
    return v == null ? 0 : v;
  }

  public void setCarry(String itemId, double value) {
    if (carry == null) {
      carry = new HashMap<String, Double>();
    }
    if (value > 0) {
      carry.put(itemId, value);
    } else {
      carry.remove(itemId);
    }
  }

  public Map<String, Long> activeSince() {
    if (activeSince == null) {
      activeSince = new HashMap<String, Long>();
    }
    return activeSince;
  }

  public void closeMonth() {
    monthDays = 0;
    if (accrued != null) {
      accrued.clear();
    }
    if (repairAccrued != null) {
      repairAccrued.clear();
    }
  }
}
