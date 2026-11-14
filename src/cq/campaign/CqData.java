package cq.campaign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;

import cq.effects.CqCasualtyData;
import cq.effects.CqContactData;
import cq.effects.CqFleetStateData;
import cq.effects.CqFleetWindowData;
import cq.effects.CqMonthData;
import cq.effects.CqRecoveryData;
import cq.effects.CqTariffData;
import cq.effects.pure.CqWhen;
import cq.items.CqCatalog;

public class CqData {

  public static final String KEY = "cq_data";

  public static final String SLOT_BUNK = "cq_slot_bunk";
  public static final String SLOT_WALL = "cq_slot_wall";
  public static final String SLOT_PET = "cq_slot_pet";
  public static final String SLOT_PLANT = "cq_slot_plant";
  public static final String SLOT_DESK = "cq_slot_desk";
  public static final String SLOT_DISPLAY = "cq_slot_display";
  public static final String SLOT_RUG = "cq_slot_rug";
  public static final String SLOT_LIGHT = "cq_slot_light";
  public static final String SLOT_GALLEY = "cq_slot_galley";
  public static final String SLOT_ENT = "cq_slot_ent";

  public static final List<String> SLOT_IDS = Collections.unmodifiableList(Arrays.asList(
      SLOT_BUNK, SLOT_WALL, SLOT_PET, SLOT_PLANT, SLOT_DESK,
      SLOT_DISPLAY, SLOT_RUG, SLOT_LIGHT, SLOT_GALLEY, SLOT_ENT));

  public static final String STYLE_SPACER = "cq_style_spacer";
  public static final String STYLE_TRITACH = "cq_style_tritach";
  public static final String STYLE_HEGEMONY = "cq_style_hegemony";
  public static final String STYLE_LUDDIC = "cq_style_luddic";
  public static final String STYLE_DOMAIN = "cq_style_domain";

  public static final List<String> STYLE_IDS = Collections.unmodifiableList(Arrays.asList(
      STYLE_SPACER, STYLE_TRITACH, STYLE_HEGEMONY, STYLE_LUDDIC, STYLE_DOMAIN));

  public static final String DEFAULT_STYLE = STYLE_SPACER;

  public static String styleDisplayName(String styleId) {
    if (STYLE_SPACER.equals(styleId)) {
      return "Spacer Standard";
    }
    if (STYLE_TRITACH.equals(styleId)) {
      return "Tri-Tachyon Executive";
    }
    if (STYLE_HEGEMONY.equals(styleId)) {
      return "Hegemony Surplus";
    }
    if (STYLE_LUDDIC.equals(styleId)) {
      return "Luddic Pastoral";
    }
    if (STYLE_DOMAIN.equals(styleId)) {
      return "Domain Antique";
    }
    return String.valueOf(styleId);
  }

  public static class Slot {
    private String id;
    private Float x;
    private Float y;
    private Float scale;
    private Boolean flipX;
    private Integer rotSteps;
    private Boolean decorative;
    private String occupant;

    Slot(String id, Float x, Float y, String occupant) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.occupant = occupant;
    }

    Slot(Slot other) {
      this.id = other.id;
      this.x = other.x;
      this.y = other.y;
      this.scale = other.scale;
      this.flipX = other.flipX;
      this.rotSteps = other.rotSteps;
      this.decorative = other.decorative;
      this.occupant = other.occupant;
    }

    public String getId() {
      return id;
    }

    public Float getX() {
      return x;
    }

    public Float getY() {
      return y;
    }

    public float getScale() {
      if (scale == null || scale.isNaN() || scale.isInfinite() || scale.floatValue() <= 0f) {
        return 1f;
      }
      return scale.floatValue();
    }

    public boolean getFlipX() {
      return Boolean.TRUE.equals(flipX);
    }

    public int getRotSteps() {
      return rotSteps == null ? 0 : rotSteps.intValue();
    }

    public boolean isDecorative() {
      return Boolean.TRUE.equals(decorative);
    }

    public String getOccupant() {
      return occupant;
    }

    public boolean isPlaced() {
      return x != null && y != null;
    }
  }

  public static final int LAYOUT_SLOTS = 3;

  public static String layoutName(int index) {
    return String.valueOf((char) ('A' + index));
  }

  public static class Layout {
    private ArrayList<Slot> slots;
    private String style;

    public List<Slot> getSlots() {
      if (slots == null) {
        slots = new ArrayList<Slot>();
      }
      return Collections.unmodifiableList(slots);
    }

    public String getStyle() {
      return style;
    }

    public boolean isEmpty() {
      return slots == null || slots.isEmpty();
    }
  }

  public static CqData getInstance() {
    Map<String, Object> pd = Global.getSector().getPersistentData();
    Object o = pd.get(KEY);
    if (o instanceof CqData) {
      return (CqData) o;
    }
    CqData data = new CqData();
    pd.put(KEY, data);
    cq.intel.CqIntel.ensureExists();
    return data;
  }

  public static boolean exists() {
    if (Global.getSector() == null) {
      return false;
    }
    return Global.getSector().getPersistentData().get(KEY) instanceof CqData;
  }

  private LinkedHashSet<String> owned;
  private ArrayList<Slot> slotEntities;
  private int slotPurchases;
  private int nextSlotSeq;
  private String style;
  private ArrayList<Layout> layouts;
  private boolean quartersOpened;

  private transient boolean effectsDirty = true;

  private CqFleetStateData fleetState;
  private CqFleetWindowData fleetWindows;
  private CqTariffData tariffs;
  private CqCasualtyData casualties;
  private CqContactData contacts;
  private CqMonthData month;
  private CqRecoveryData recovery;

  private transient EnumSet<CqWhen> occasionsHolding;

  public CqFleetStateData fleetState() {
    if (fleetState == null) {
      fleetState = new CqFleetStateData();
    }
    return fleetState;
  }

  public CqFleetWindowData fleetWindows() {
    if (fleetWindows == null) {
      fleetWindows = new CqFleetWindowData();
    }
    return fleetWindows;
  }

  public CqTariffData tariffs() {
    if (tariffs == null) {
      tariffs = new CqTariffData();
    }
    return tariffs;
  }

  public CqCasualtyData casualties() {
    if (casualties == null) {
      casualties = new CqCasualtyData();
    }
    return casualties;
  }

  public CqContactData contacts() {
    if (contacts == null) {
      contacts = new CqContactData();
    }
    return contacts;
  }

  public CqMonthData month() {
    if (month == null) {
      month = new CqMonthData();
    }
    return month;
  }

  public CqRecoveryData recovery() {
    if (recovery == null) {
      recovery = new CqRecoveryData();
    }
    return recovery;
  }

  public boolean occasionHolds(CqWhen when) {
    return occasionsHolding != null && occasionsHolding.contains(when);
  }

  public boolean setOccasionsHolding(EnumSet<CqWhen> holding) {
    boolean changed = occasionsHolding == null ? !holding.isEmpty()
        : !occasionsHolding.equals(holding);
    occasionsHolding = holding;
    return changed;
  }

  private String newSlotId() {
    nextSlotSeq++;
    return "cq_slotinst_" + nextSlotSeq;
  }

  private static int initialCount() {
    return CqSettings.getInt("cq_slot_initial_count", CqSlotMath.DEFAULT_INITIAL_COUNT);
  }

  private static int maxCount() {
    return CqSettings.getInt("cq_slot_max_count", CqSlotMath.DEFAULT_MAX_COUNT);
  }

  public boolean addOwned(String itemId) {
    boolean added = owned().add(itemId);
    if (added) {
      effectsDirty = true;
    }
    return added;
  }

  public boolean isOwned(String itemId) {
    return owned().contains(itemId);
  }

  public Set<String> getOwned() {
    return Collections.unmodifiableSet(owned());
  }

  public List<Slot> getSlotEntities() {
    return Collections.unmodifiableList(entities());
  }

  public Slot getSlotById(String slotId) {
    if (slotId == null) {
      return null;
    }
    for (Slot slot : entities()) {
      if (slotId.equals(slot.id)) {
        return slot;
      }
    }
    return null;
  }

  public int getSlotCount() {
    return CqSlotMath.slotCount(getSlotPurchases(), initialCount(), maxCount());
  }

  public int getSlotPurchases() {
    return Math.max(0, Math.min(slotPurchases, CqSlotMath.maxPurchases(initialCount(), maxCount())));
  }

  public long getNextSlotPrice() {
    if (!canPurchaseSlot()) {
      return -1L;
    }
    return CqSettings.slotPrice(getSlotPurchases());
  }

  public long getRemainingLadderCost() {
    long sum = 0L;
    for (Long price : getRemainingLadderPrices()) {
      sum += price.longValue();
    }
    return sum;
  }

  public List<Long> getRemainingLadderPrices() {
    long[] ladder = CqSettings.slotLadder();
    return CqSlotMath.remainingPrices(getSlotPurchases(), initialCount(), maxCount(), ladder,
        CqSettings.slotPriceOverrides(ladder.length));
  }

  public boolean canPurchaseSlot() {
    return CqSlotMath.canPurchase(getSlotPurchases(), initialCount(), maxCount());
  }

  public Slot purchaseSlot() {
    if (!canPurchaseSlot()) {
      return null;
    }
    long price = getNextSlotPrice();
    if (price < 0 || Global.getSector() == null || Global.getSector().getPlayerFleet() == null) {
      return null;
    }
    com.fs.starfarer.api.campaign.CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
    if (cargo == null || cargo.getCredits() == null || cargo.getCredits().get() < price) {
      return null;
    }
    cargo.getCredits().subtract(price);
    slotPurchases = getSlotPurchases() + 1;
    Slot slot = new Slot(newSlotId(), null, null, null);
    entities().add(slot);
    return slot;
  }

  public int getMaxDecorativeSlotCount() {
    return CqSettings.getInt("cq_decorative_slot_max", 20);
  }

  public int getDecorativeSlotCount() {
    return decorativeCountIn(entities());
  }

  public boolean canAddDecorativeSlot() {
    return getDecorativeSlotCount() < getMaxDecorativeSlotCount();
  }

  public long getNextDecorativeSlotPrice() {
    if (!canAddDecorativeSlot()) {
      return -1L;
    }
    return (long) CqSettings.tunable("cq_decorative_slot_price", 10000f);
  }

  public Slot addDecorativeSlot() {
    if (!canAddDecorativeSlot()) {
      return null;
    }
    long price = getNextDecorativeSlotPrice();
    if (price < 0 || Global.getSector() == null || Global.getSector().getPlayerFleet() == null) {
      return null;
    }
    com.fs.starfarer.api.campaign.CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
    if (cargo == null || cargo.getCredits() == null || cargo.getCredits().get() < price) {
      return null;
    }
    cargo.getCredits().subtract(price);
    Slot slot = new Slot(newSlotId(), null, null, null);
    slot.decorative = Boolean.TRUE;
    entities().add(0, slot);
    return slot;
  }

  public boolean occupy(String slotId, String itemId) {
    Slot slot = getSlotById(slotId);
    if (slot == null || itemId == null || CqCatalog.get(itemId) == null || !isOwned(itemId)) {
      return false;
    }
    if (slot.occupant != null && !slot.occupant.equals(itemId)) {
      return false;
    }
    if (!slot.isDecorative()) {
      for (Slot other : entities()) {
        if (other != slot && !other.isDecorative() && itemId.equals(other.occupant)) {
          return false;
        }
      }
    }
    boolean changed = !itemId.equals(slot.occupant);
    slot.occupant = itemId;
    if (changed && !slot.isDecorative()) {
      effectsDirty = true;
    }
    return true;
  }

  public boolean vacate(String slotId) {
    Slot slot = getSlotById(slotId);
    if (slot == null || slot.occupant == null) {
      return false;
    }
    boolean wasRegular = !slot.isDecorative();
    slot.occupant = null;
    if (wasRegular) {
      effectsDirty = true;
    }
    return true;
  }

  public boolean moveItemToSlot(String itemId, String targetSlotId) {
    if (itemId == null || CqCatalog.get(itemId) == null || !isOwned(itemId)) {
      return false;
    }
    Slot target = getSlotById(targetSlotId);
    if (target == null || target.isDecorative()) {
      return false;
    }
    if (itemId.equals(target.occupant)) {
      return true;
    }
    Slot source = null;
    for (Slot slot : entities()) {
      if (!slot.isDecorative() && itemId.equals(slot.occupant)) {
        source = slot;
        break;
      }
    }
    if (source == null) {
      return false;
    }
    source.occupant = null;
    target.occupant = itemId;
    effectsDirty = true;
    return true;
  }

  public void setScale(String slotId, float scale) {
    Slot slot = getSlotById(slotId);
    if (slot == null) {
      return;
    }
    float min = CqSettings.getFloat("cq_sticker_scale_min", 0.5f);
    float max = CqSettings.getFloat("cq_sticker_scale_max", 2.0f);
    slot.scale = Float.valueOf(CqSlotMath.clampScale(scale, min, max));
  }

  public void setFlipX(String slotId, boolean flipX) {
    Slot slot = getSlotById(slotId);
    if (slot == null) {
      return;
    }
    slot.flipX = Boolean.valueOf(flipX);
  }

  public void setRotSteps(String slotId, int rotSteps) {
    Slot slot = getSlotById(slotId);
    if (slot == null) {
      return;
    }
    slot.rotSteps = Integer.valueOf(CqSlotMath.normalizeRotSteps(rotSteps, stepsPerCircle()));
  }

  private static int stepsPerCircle() {
    float step = CqSettings.getFloat("cq_rotate_step_degrees", 30f);
    return step <= 0f ? 12 : Math.max(1, Math.round(360f / step));
  }

  public void setPosition(String slotId, Float x, Float y) {
    Slot slot = getSlotById(slotId);
    if (slot == null) {
      return;
    }
    if (x == null || y == null) {
      slot.x = null;
      slot.y = null;
    } else {
      slot.x = x;
      slot.y = y;
    }
  }

  public void moveSlotToIndex(String slotId, int index) {
    Slot slot = getSlotById(slotId);
    if (slot == null) {
      return;
    }
    List<Slot> list = entities();
    list.remove(slot);
    int bandStart = 0;
    int bandEnd;
    if (slot.isDecorative()) {
      bandEnd = decorativeCountIn(list);
    } else {
      bandStart = decorativeCountIn(list);
      bandEnd = list.size();
    }
    int clamped = Math.max(bandStart, Math.min(index, bandEnd));
    list.add(clamped, slot);
  }

  private static int decorativeCountIn(List<Slot> list) {
    int count = 0;
    for (Slot slot : list) {
      if (slot.isDecorative()) {
        count++;
      }
    }
    return count;
  }

  public String activeSlotOf(String itemId) {
    if (itemId == null) {
      return null;
    }
    for (Slot slot : entities()) {
      if (!slot.isDecorative() && itemId.equals(slot.occupant)) {
        return slot.id;
      }
    }
    return null;
  }

  public int activeSlotNumberOf(String itemId) {
    if (itemId == null) {
      return 0;
    }
    int regularIndex = 0;
    for (Slot slot : entities()) {
      if (slot.isDecorative()) {
        continue;
      }
      regularIndex++;
      if (itemId.equals(slot.occupant)) {
        return regularIndex;
      }
    }
    return 0;
  }

  public boolean isItemActive(String itemId) {
    return activeSlotOf(itemId) != null;
  }

  public List<String> getActiveItemIds() {
    LinkedHashSet<String> result = new LinkedHashSet<String>();
    for (Slot slot : entities()) {
      if (!slot.isDecorative() && slot.occupant != null) {
        result.add(slot.occupant);
      }
    }
    return new ArrayList<String>(result);
  }

  public String getStyle() {
    if (style == null) {
      style = DEFAULT_STYLE;
    } else if (!STYLE_IDS.contains(style)) {
      healWarn("an unknown style id (" + style + ")");
      style = DEFAULT_STYLE;
    }
    return style;
  }

  public void setStyle(String styleId) {
    this.style = styleId;
  }

  public List<Layout> getLayouts() {
    return Collections.unmodifiableList(layouts());
  }

  public Layout getLayout(int index) {
    if (index < 0 || index >= LAYOUT_SLOTS) {
      return null;
    }
    return layouts().get(index);
  }

  public boolean isLayoutEmpty(int index) {
    Layout layout = getLayout(index);
    return layout == null || layout.isEmpty();
  }

  public boolean isArrangementEmpty() {
    for (Slot slot : entities()) {
      if (slot.occupant != null) {
        return false;
      }
    }
    return true;
  }

  public boolean saveLayout(int index) {
    Layout layout = getLayout(index);
    if (layout == null) {
      return false;
    }
    ArrayList<Slot> snapshot = new ArrayList<Slot>();
    for (Slot slot : entities()) {
      if (slot.occupant != null) {
        snapshot.add(new Slot(slot));
      }
    }
    if (snapshot.isEmpty()) {
      return false;
    }
    layout.slots = snapshot;
    layout.style = getStyle();
    return true;
  }

  public boolean canApplyLayoutEntry(Slot entry) {
    return entry != null && entry.occupant != null
        && getSlotById(entry.id) != null
        && CqCatalog.get(entry.occupant) != null
        && isOwned(entry.occupant);
  }

  public int loadLayout(int index) {
    Layout layout = getLayout(index);
    if (layout == null || layout.isEmpty()) {
      return -1;
    }
    for (Slot slot : entities()) {
      slot.occupant = null;
    }
    int skipped = 0;
    for (Slot entry : layout.slots) {
      if (!canApplyLayoutEntry(entry) || !occupy(entry.id, entry.occupant)) {
        skipped++;
        continue;
      }
      Slot live = getSlotById(entry.id);
      live.x = entry.x;
      live.y = entry.y;
      live.scale = entry.scale;
      live.flipX = entry.flipX;
      live.rotSteps = entry.rotSteps;
    }
    if (layout.style != null && STYLE_IDS.contains(layout.style)) {
      style = layout.style;
    }
    effectsDirty = true;
    return skipped;
  }

  public boolean isQuartersOpened() {
    return quartersOpened;
  }

  public void noteQuartersOpened() {
    quartersOpened = true;
  }

  public void markEffectsDirty() {
    effectsDirty = true;
  }

  public boolean consumeEffectsDirty() {
    boolean d = effectsDirty;
    effectsDirty = false;
    return d;
  }

  public void selfHeal(Set<String> validItemIds) {
    if (validItemIds == null || validItemIds.isEmpty()) {
      return;
    }
    if (recovery != null) {
      recovery.retain(validItemIds);
    }
    int dropped = 0;
    for (Iterator<String> it = owned().iterator(); it.hasNext();) {
      if (!validItemIds.contains(it.next())) {
        it.remove();
        dropped++;
      }
    }
    int before = getSlotPurchases();
    if (slotPurchases != before) {
      slotPurchases = before;
      dropped++;
    }
    Set<String> seenIds = new LinkedHashSet<String>();
    for (Slot slot : entities()) {
      if (slot.id == null || !seenIds.add(slot.id)) {
        slot.id = newSlotId();
        dropped++;
      }
      if (slot.occupant != null
          && (!validItemIds.contains(slot.occupant) || !owned().contains(slot.occupant))) {
        slot.occupant = null;
        dropped++;
      }
      boolean badX = slot.x != null && (slot.x.isNaN() || slot.x.isInfinite());
      boolean badY = slot.y != null && (slot.y.isNaN() || slot.y.isInfinite());
      if (badX || badY || (slot.x == null) != (slot.y == null)) {
        slot.x = null;
        slot.y = null;
        dropped++;
      }
      if (slot.scale != null) {
        float min = CqSettings.getFloat("cq_sticker_scale_min", 0.5f);
        float max = CqSettings.getFloat("cq_sticker_scale_max", 2.0f);
        float healed = CqSlotMath.clampScale(slot.scale.floatValue(), min, max);
        if (healed != slot.scale.floatValue()) {
          slot.scale = Float.valueOf(healed);
          dropped++;
        }
      }
      if (slot.rotSteps != null) {
        int normalized = CqSlotMath.normalizeRotSteps(slot.rotSteps.intValue(), stepsPerCircle());
        if (normalized != slot.rotSteps.intValue()) {
          slot.rotSteps = Integer.valueOf(normalized);
          dropped++;
        }
      }
    }
    List<Slot> regularSlots = new ArrayList<Slot>();
    for (Slot slot : entities()) {
      if (!slot.isDecorative()) {
        regularSlots.add(slot);
      }
    }
    String[] occupants = new String[regularSlots.size()];
    boolean[] placedFlags = new boolean[regularSlots.size()];
    for (int i = 0; i < regularSlots.size(); i++) {
      occupants[i] = regularSlots.get(i).occupant;
      placedFlags[i] = regularSlots.get(i).isPlaced();
    }
    boolean[] clearMask = CqSlotMath.dedupeClearMask(occupants, placedFlags);
    for (int i = 0; i < clearMask.length; i++) {
      if (clearMask[i]) {
        regularSlots.get(i).occupant = null;
        dropped++;
      }
    }
    ArrayList<Slot> partitioned = new ArrayList<Slot>();
    for (Slot slot : entities()) {
      if (slot.isDecorative()) {
        partitioned.add(slot);
      }
    }
    int decoCount = partitioned.size();
    for (Slot slot : entities()) {
      if (!slot.isDecorative()) {
        partitioned.add(slot);
      }
    }
    if (!partitioned.equals(entities())) {
      dropped++;
    }
    slotEntities = partitioned;
    int decoMax = getMaxDecorativeSlotCount();
    for (int i = decoCount - 1; i >= 0 && decoCount > decoMax; i--) {
      if (slotEntities.get(i).occupant == null) {
        slotEntities.remove(i);
        decoCount--;
        dropped++;
      }
    }
    int count = getSlotCount();
    int regulars = slotEntities.size() - decoCount;
    while (regulars < count) {
      slotEntities.add(new Slot(newSlotId(), null, null, null));
      regulars++;
      dropped++;
    }
    for (int i = slotEntities.size() - 1; i >= decoCount && regulars > count; i--) {
      if (slotEntities.get(i).occupant == null) {
        slotEntities.remove(i);
        regulars--;
        dropped++;
      }
    }
    int layoutCeiling = maxCount() + getMaxDecorativeSlotCount();
    for (Layout layout : layouts()) {
      while (layout.slots != null && layout.slots.size() > layoutCeiling) {
        layout.slots.remove(layout.slots.size() - 1);
        dropped++;
      }
    }
    if (dropped > 0) {
      healWarn(dropped + " invalid persisted entr(y/ies)");
      effectsDirty = true;
    }
  }

  private static volatile boolean healWarned;

  private static void healWarn(String what) {
    if (healWarned) {
      return;
    }
    healWarned = true;
    Global.getLogger(CqData.class).warn("Captain's Quarters: healed " + what
        + " in the saved quarters data (most likely a mod that adds furnishings was removed, or"
        + " settings.json changed). The rest of the collection is intact.");
  }

  private LinkedHashSet<String> owned() {
    if (owned == null) {
      owned = new LinkedHashSet<String>();
    }
    return owned;
  }

  private ArrayList<Slot> entities() {
    if (slotEntities == null) {
      slotEntities = new ArrayList<Slot>();
      int count = CqSlotMath.slotCount(0, initialCount(), maxCount());
      for (int i = 0; i < count; i++) {
        slotEntities.add(new Slot(newSlotId(), null, null, null));
      }
      effectsDirty = true;
    }
    return slotEntities;
  }

  private ArrayList<Layout> layouts() {
    if (layouts == null) {
      layouts = new ArrayList<Layout>();
    }
    while (layouts.size() > LAYOUT_SLOTS) {
      layouts.remove(layouts.size() - 1);
    }
    for (int i = 0; i < LAYOUT_SLOTS; i++) {
      if (i >= layouts.size()) {
        layouts.add(new Layout());
      } else if (layouts.get(i) == null) {
        layouts.set(i, new Layout());
      }
    }
    return layouts;
  }

}
