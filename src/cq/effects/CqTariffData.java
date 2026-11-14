package cq.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CqTariffData {

  private int month;
  private HashMap<String, Long> returned;
  private long unannounced;
  private ArrayList<String> unannouncedBy;
  private boolean unannouncedStopped;

  public void markStopped() {
    unannouncedStopped = true;
  }

  public boolean takeStopped() {
    boolean stopped = unannouncedStopped;
    unannouncedStopped = false;
    return stopped;
  }

  public void rollTo(int monthNow) {
    if (month != monthNow) {
      month = monthNow;
      if (returned != null) {
        returned.clear();
      }
    }
  }

  public long returnedBy(String itemId) {
    Long value = returned == null ? null : returned.get(itemId);
    return value == null ? 0L : value;
  }

  public void addReturned(String itemId, long credits) {
    if (credits <= 0L) {
      return;
    }
    if (returned == null) {
      returned = new HashMap<String, Long>();
    }
    returned.put(itemId, returnedBy(itemId) + credits);
    unannounced += credits;
    if (unannouncedBy == null) {
      unannouncedBy = new ArrayList<String>();
    }
    if (!unannouncedBy.contains(itemId)) {
      unannouncedBy.add(itemId);
    }
  }

  public long unannounced() {
    return unannounced;
  }

  public long takeUnannounced() {
    long credits = unannounced;
    unannounced = 0L;
    return credits;
  }

  public List<String> takeUnannouncedBy() {
    List<String> ids = new ArrayList<String>();
    if (unannouncedBy != null) {
      ids.addAll(unannouncedBy);
      unannouncedBy.clear();
    }
    return ids;
  }
}
