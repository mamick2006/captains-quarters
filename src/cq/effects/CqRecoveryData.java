package cq.effects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class CqRecoveryData {

  private HashMap<String, Long> cargoPaidAt;

  public Long cargoPaidAt(String itemId) {
    return cargoPaidAt == null ? null : cargoPaidAt.get(itemId);
  }

  public void setCargoPaidAt(String itemId, long timestamp) {
    if (cargoPaidAt == null) {
      cargoPaidAt = new HashMap<String, Long>();
    }
    cargoPaidAt.put(itemId, Long.valueOf(timestamp));
  }

  public void retain(Set<String> validItemIds) {
    if (cargoPaidAt == null || validItemIds == null || validItemIds.isEmpty()) {
      return;
    }
    for (Iterator<String> it = cargoPaidAt.keySet().iterator(); it.hasNext();) {
      if (!validItemIds.contains(it.next())) {
        it.remove();
      }
    }
  }
}
