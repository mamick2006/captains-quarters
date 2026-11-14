package cq.effects;

public class CqFleetWindowData {

  private Long dockedAt;

  public Long dockedAt() {
    return dockedAt;
  }

  public void leftMarket(long timestamp) {
    dockedAt = timestamp;
  }
}
