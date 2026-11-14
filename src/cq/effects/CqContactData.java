package cq.effects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CqContactData {

  private LinkedHashMap<String, LinkedHashMap<String, Long>> stamps;
  private ArrayList<String> known;

  public Long stamp(String itemId, String targetId) {
    if (stamps == null) {
      return null;
    }
    Map<String, Long> row = stamps.get(itemId);
    return row == null ? null : row.get(targetId);
  }

  public void setStamp(String itemId, String targetId, long timestamp) {
    if (stamps == null) {
      stamps = new LinkedHashMap<String, LinkedHashMap<String, Long>>();
    }
    LinkedHashMap<String, Long> row = stamps.get(itemId);
    if (row == null) {
      row = new LinkedHashMap<String, Long>();
      stamps.put(itemId, row);
    }
    row.put(targetId, Long.valueOf(timestamp));
  }

  public Map<String, LinkedHashMap<String, Long>> stamps() {
    return stamps;
  }

  public int stampCount() {
    int count = 0;
    if (stamps != null) {
      for (Map<String, Long> row : stamps.values()) {
        count += row.size();
      }
    }
    return count;
  }

  public boolean knowsContacts() {
    return known != null;
  }

  public List<String> known() {
    return known == null ? new ArrayList<String>() : known;
  }

  public void setKnown(Collection<String> personIds) {
    known = new ArrayList<String>(personIds);
  }
}
