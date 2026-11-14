package cq.campaign;

import cq.items.CqCatalog;

public final class CqDev {

  private CqDev() {
  }

  public static int giveAllItems() {
    CqData data = CqData.getInstance();
    int added = 0;
    for (CqCatalog.Furnishing f : CqCatalog.all()) {
      if (data.addOwned(f.getId())) {
        added++;
      }
    }
    if (added > 0) {
      cq.campaign.acquire.CqSalvageSeeder.refreshSeeded();
    }
    return added;
  }
}
