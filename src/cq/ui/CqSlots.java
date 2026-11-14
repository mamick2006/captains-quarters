package cq.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cq.campaign.CqData;

public final class CqSlots {

  public static final class Entry {
    public final String id;
    public final Float x;
    public final Float y;
    public final String occupant;
    public final float scale;
    public final boolean flipX;
    public final int rotSteps;
    public final boolean decorative;

    Entry(CqData.Slot s) {
      this.id = s.getId();
      this.x = s.getX();
      this.y = s.getY();
      this.occupant = s.getOccupant();
      this.scale = s.getScale();
      this.flipX = s.getFlipX();
      this.rotSteps = s.getRotSteps();
      this.decorative = s.isDecorative();
    }

    public boolean placed() {
      return x != null && y != null;
    }
  }

  private CqSlots() {
  }

  public static List<Entry> list() {
    if (!CqData.exists()) {
      return Collections.emptyList();
    }
    List<Entry> out = new ArrayList<Entry>();
    for (CqData.Slot s : CqData.getInstance().getSlotEntities()) {
      out.add(new Entry(s));
    }
    return out;
  }

  public static Entry byId(String slotId) {
    if (!CqData.exists() || slotId == null) {
      return null;
    }
    CqData.Slot s = CqData.getInstance().getSlotById(slotId);
    return s == null ? null : new Entry(s);
  }

  public static int slotCount() {
    return CqData.exists() ? CqData.getInstance().getSlotCount() : 0;
  }

  public static Long nextPrice() {
    if (!CqData.exists()) {
      return null;
    }
    long p = CqData.getInstance().getNextSlotPrice();
    return p < 0 ? null : Long.valueOf(p);
  }

  public static boolean canPurchaseSlot() {
    return CqData.exists() && CqData.getInstance().canPurchaseSlot();
  }

  public static boolean purchaseSlot() {
    return CqData.exists() && CqData.getInstance().purchaseSlot() != null;
  }

  public static boolean canAddDecorativeSlot() {
    return CqData.exists() && CqData.getInstance().canAddDecorativeSlot();
  }

  public static int decorativeSlotCount() {
    return CqData.exists() ? CqData.getInstance().getDecorativeSlotCount() : 0;
  }

  public static int maxDecorativeSlotCount() {
    return CqData.exists() ? CqData.getInstance().getMaxDecorativeSlotCount() : 0;
  }

  public static boolean addDecorativeSlot() {
    return CqData.exists() && CqData.getInstance().addDecorativeSlot() != null;
  }

  public static Long nextDecorativePrice() {
    if (!CqData.exists()) {
      return null;
    }
    long p = CqData.getInstance().getNextDecorativeSlotPrice();
    return p < 0 ? null : Long.valueOf(p);
  }

  public static boolean moveItemToSlot(String itemId, String targetSlotId) {
    return CqData.exists() && CqData.getInstance().moveItemToSlot(itemId, targetSlotId);
  }

  public static Integer slotNumberOf(String itemId) {
    if (!CqData.exists()) {
      return null;
    }
    int n = CqData.getInstance().activeSlotNumberOf(itemId);
    return n <= 0 ? null : Integer.valueOf(n);
  }

  public static long remainingLadderCost() {
    return CqData.exists() ? CqData.getInstance().getRemainingLadderCost() : 0L;
  }

  public static java.util.List<Long> remainingLadderPrices() {
    return CqData.exists() ? CqData.getInstance().getRemainingLadderPrices()
        : new java.util.ArrayList<Long>();
  }

  public static boolean setOccupant(String slotId, String itemId) {
    return CqData.exists() && CqData.getInstance().occupy(slotId, itemId);
  }

  public static void resetSlot(String slotId) {
    if (CqData.exists()) {
      CqData.getInstance().vacate(slotId);
    }
  }

  public static void setStickerPos(String slotId, float x, float y) {
    if (CqData.exists()) {
      CqData.getInstance().setPosition(slotId, Float.valueOf(x), Float.valueOf(y));
    }
  }

  public static void reorder(String slotId, int toIndex) {
    if (CqData.exists()) {
      CqData.getInstance().moveSlotToIndex(slotId, toIndex);
    }
  }

  public static void setScale(String slotId, float scale) {
    if (CqData.exists()) {
      CqData.getInstance().setScale(slotId, scale);
    }
  }

  public static void setFlipX(String slotId, boolean flipX) {
    if (CqData.exists()) {
      CqData.getInstance().setFlipX(slotId, flipX);
    }
  }

  public static void setRotSteps(String slotId, int rotSteps) {
    if (CqData.exists()) {
      CqData.getInstance().setRotSteps(slotId, rotSteps);
    }
  }
}
