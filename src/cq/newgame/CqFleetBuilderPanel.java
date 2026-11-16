package cq.newgame;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import cq.campaign.CqSettings;
import cq.campaign.CqStrings;
import cq.newgame.pure.CqStartRules;
import cq.newgame.pure.CqStartRules.Flavour;
import cq.newgame.pure.CqStartRules.Problem;
import cq.topbar.CqShipCard;
import cq.ui.CqUi;

public final class CqFleetBuilderPanel implements CustomVisualDialogDelegate {

  private static final float PAD = 10f;
  private static final float HEAD_H = 58f;
  private static final float ROW2_Y = 32f;
  private static final float FILTER_H = 26f;
  private static final float ROW_H = 72f;
  private static final float ICON = 64f;
  private static final float BTN_H = 24f;
  private static final float FOOT_H = 30f;
  private static final float SEARCH_W = 150f;

  private final InteractionDialogAPI dialog;
  private final Map<String, MemoryAPI> memoryMap;
  private final Router router = new Router();
  private final Backdrop backdrop = new Backdrop();

  private CustomPanelAPI root;
  private DialogCallbacks callbacks;
  private float w;
  private float h;
  private float leftW;
  private float rightX;
  private float rightW;
  private float listTop;

  private CustomPanelAPI headPanel;
  private CustomPanelAPI filterPanel;
  private CustomPanelAPI poolPanel;
  private CustomPanelAPI fleetPanel;
  private TooltipMakerAPI poolList;
  private final List<PoolRow> poolRows = new ArrayList<PoolRow>();
  private int poolBudget;
  private TextFieldAPI search;
  private String lastSearch = "";
  private String pendingSearch = "";
  private float searchStill = 0f;
  private static final float SEARCH_PAUSE = 0.3f;
  private MutableCharacterStatsAPI cardStats;

  private final CqStartRules.Budgets budgets = CqSettings.fleetBudgets();
  private final List<CqFleetPool.Entry> pool;
  private final List<String> sources = new ArrayList<String>();
  private final Map<String, FleetMemberAPI> members = new HashMap<String, FleetMemberAPI>();
  private CampaignFleetAPI displayFleet;

  private Flavour flavour = Flavour.SMALL;
  private boolean officer = false;
  private boolean chooseLocation = false;
  private final List<CqFleetPool.Entry> picks = new ArrayList<CqFleetPool.Entry>();
  private String sizeFilter = null;
  private int sourceFilter = -1;
  private boolean sourcesOpen = false;
  private int sort = 0;
  private boolean sortChosen = false;
  private static final int SORTS = 3;

  public interface Finish {
    void finish(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, Flavour flavour,
        boolean officer, List<String> variantIds, boolean chooseLocation);
  }

  private final Finish nex;

  public CqFleetBuilderPanel(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, Finish nex) {
    this.dialog = dialog;
    this.memoryMap = memoryMap;
    this.nex = nex;
    this.pool = CqFleetPool.build(budgets);
    TreeSet<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    for (CqFleetPool.Entry e : pool) {
      if (!CqFleetPool.VANILLA_SOURCE.equals(e.source)) {
        names.add(e.source);
      }
    }
    sources.add(CqFleetPool.VANILLA_SOURCE);
    sources.addAll(names);
  }

  public static void open(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, Finish nex) {
    float sw = Global.getSettings().getScreenWidth();
    float sh = Global.getSettings().getScreenHeight();
    float pw = Math.min(1160f, sw - 80f);
    float ph = Math.min(760f, sh - 120f);
    dialog.showCustomVisualDialog(pw, ph, new CqFleetBuilderPanel(dialog, memoryMap, nex));
  }

  @Override
  public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
    this.root = panel;
    this.callbacks = callbacks;
    w = panel.getPosition().getWidth();
    h = panel.getPosition().getHeight();
    leftW = Math.round(w * 0.58f);
    rightX = leftW + 2f * PAD;
    rightW = w - rightX - 8f;
    listTop = HEAD_H + PAD + FILTER_H + PAD;
    buildSearch();
    rebuild(true);
  }

  @Override
  public CustomUIPanelPlugin getCustomPanelPlugin() {
    return backdrop;
  }

  @Override
  public float getNoiseAlpha() {
    return 0f;
  }

  @Override
  public void advance(float amount) {
  }

  @Override
  public void reportDismissed(int option) {
    members.clear();
    displayFleet = null;
  }

  private void pressed(Object data) {
    if (!(data instanceof Btn)) {
      return;
    }
    Btn b = (Btn) data;
    switch (b.kind) {
      case FLAVOUR:
        flavour = (Flavour) b.arg;
        if (!sortChosen) {
          sort = flavour == Flavour.LONE ? 1 : 0;
        }
        if (!flavour.officerAllowed) {
          officer = false;
        }
        if (flavour == Flavour.SMALL && CqStartRules.DESTROYER.equals(sizeFilter)) {
          sizeFilter = null;
        }
        break;
      case OFFICER:
        officer = !officer;
        break;
      case LOCATION:
        chooseLocation = !chooseLocation;
        break;
      case SIZE:
        sizeFilter = (String) b.arg;
        break;
      case SOURCE:
        sourcesOpen = !sourcesOpen;
        break;
      case PICK_SOURCE:
        sourceFilter = ((Integer) b.arg).intValue();
        sourcesOpen = false;
        break;
      case SORT:
        sort = (sort + 1) % SORTS;
        sortChosen = true;
        break;
      case ADD:
        picks.add((CqFleetPool.Entry) b.arg);
        break;
      case REMOVE:
        picks.remove(((Integer) b.arg).intValue());
        break;
      case FLAGSHIP: {
        CqFleetPool.Entry e = picks.remove(((Integer) b.arg).intValue());
        picks.add(0, e);
        break;
      }
      case BACK:
        callbacks.dismissDialog();
        return;
      case CONFIRM:
        if (problem() != null) {
          return;
        }
        List<String> variants = new ArrayList<String>();
        for (CqFleetPool.Entry e : picks) {
          variants.add(e.variantId);
        }
        callbacks.dismissDialog();
        if (nex != null) {
          nex.finish(dialog, memoryMap, flavour, officer, variants, chooseLocation);
        } else {
          CqFleetStart.confirm(dialog, memoryMap, flavour, officer, variants, chooseLocation);
        }
        return;
      default:
        return;
    }
    if (b.kind == Kind.ADD || b.kind == Kind.REMOVE || b.kind == Kind.FLAGSHIP
        || b.kind == Kind.OFFICER) {
      fleetPanel = replace(fleetPanel, buildFleet(), rightX, HEAD_H + PAD);
      if (sourcesOpen) {
        poolPanel = replace(poolPanel, buildPool(), 0f, listTop);
      } else if (poolBudget != budget()) {
        poolPanel = replace(poolPanel, buildPool(), 0f, listTop);
      } else {
        refreshPool();
      }
      return;
    }
    rebuild(true);
  }

  private void refreshPool() {
    int left = budget() - spent();
    boolean full = picks.size() >= flavour.maxShips;
    for (PoolRow r : poolRows) {
      CqUi.setEnabledSayingWhy(r.add, !full && r.entry.value <= left);
      r.cost.setColor(r.entry.value <= left ? CqUi.hl() : CqUi.bad());
    }
  }

  private String addBlocked(CqFleetPool.Entry e) {
    int left = budget() - spent();
    if (picks.size() >= flavour.maxShips) {
      return s("add_full");
    }
    if (e.value > left) {
      return String.format(s("add_cost"), credits(e.value), credits(Math.max(0, left)));
    }
    return null;
  }

  private static final class PoolRow {
    final CqFleetPool.Entry entry;
    final ButtonAPI add;
    final LabelAPI cost;

    PoolRow(CqFleetPool.Entry entry, ButtonAPI add, LabelAPI cost) {
      this.entry = entry;
      this.add = add;
      this.cost = cost;
    }
  }

  private Problem problem() {
    List<String> sizes = new ArrayList<String>();
    List<Integer> values = new ArrayList<Integer>();
    for (CqFleetPool.Entry e : picks) {
      sizes.add(e.size);
      values.add(Integer.valueOf(e.value));
    }
    return CqStartRules.problem(flavour, officer, sizes, values, budgets);
  }

  private int spent() {
    int t = 0;
    for (CqFleetPool.Entry e : picks) {
      t += e.value;
    }
    return t;
  }

  private int budget() {
    return budgets.budget(flavour, officer);
  }

  private void rebuild(boolean resetScroll) {
    float scroll = 0f;
    if (!resetScroll && poolList != null && poolList.getExternalScroller() != null) {
      scroll = poolList.getExternalScroller().getYOffset();
    }
    headPanel = replace(headPanel, buildHead(), 0f, 0f);
    filterPanel = replace(filterPanel, buildFilters(), SEARCH_W + PAD, HEAD_H + PAD);
    fleetPanel = replace(fleetPanel, buildFleet(), rightX, HEAD_H + PAD);
    poolPanel = replace(poolPanel, buildPool(), 0f, listTop);
    if (scroll > 0f && poolList.getExternalScroller() != null) {
      poolList.getExternalScroller().setYOffset(scroll);
    }
  }

  private CustomPanelAPI replace(CustomPanelAPI old, CustomPanelAPI fresh, float x, float y) {
    if (old != null) {
      root.removeComponent(old);
    }
    root.addComponent(fresh).inTL(x, y);
    return fresh;
  }

  private CustomPanelAPI buildHead() {
    CustomPanelAPI p = root.createCustomPanel(w, HEAD_H, router);
    TooltipMakerAPI title = p.createUIElement(240f, HEAD_H, false);
    title.setParaOrbitronLarge();
    title.addPara(s("title"), Misc.getBasePlayerColor(), 2f);
    p.addUIElement(title).inTL(0f, 0f);

    float x = 0f;
    x = toggle(p, x, ROW2_Y, 360f, s("flavour_lone"), flavour == Flavour.LONE,
        new Btn(Kind.FLAVOUR, Flavour.LONE), loneTip());
    x = toggle(p, x, ROW2_Y, 330f, s("flavour_small"), flavour == Flavour.SMALL,
        new Btn(Kind.FLAVOUR, Flavour.SMALL), smallTip());
    float locW = 260f;
    toggle(p, w - locW - 8f, 2f, locW, chooseLocation ? s("location_choose") : s("location_random"), false,
        new Btn(Kind.LOCATION, null), CqUi.tip(s("location_tip")));
    return p;
  }

  private TooltipMakerAPI.TooltipCreator loneTip() {
    return CqUi.tip(String.format(s("flavour_lone_tip"), credits(budgets.lone)));
  }

  private TooltipMakerAPI.TooltipCreator smallTip() {
    return CqUi.tip(String.format(s("flavour_small_tip"), credits(budgets.small),
        credits(budgets.officer())));
  }

  private float toggle(CustomPanelAPI p, float x, float bw, String text, boolean checked, Btn id,
      TooltipMakerAPI.TooltipCreator tip) {
    return toggle(p, x, 2f, bw, text, checked, id, tip);
  }

  private float toggle(CustomPanelAPI p, float x, float y, float bw, String text, boolean checked,
      Btn id,
      TooltipMakerAPI.TooltipCreator tip) {
    TooltipMakerAPI el = p.createUIElement(bw, BTN_H, false);
    ButtonAPI b = el.addAreaCheckbox(text, id, CqUi.base(), CqUi.dark(), CqUi.bright(), bw, BTN_H, 0f);
    b.setChecked(checked);
    if (tip != null) {
      el.addTooltipToPrevious(tip, TooltipMakerAPI.TooltipLocation.BELOW);
    }
    p.addUIElement(el).inTL(x, y);
    return x + bw + 4f;
  }

  private void buildSearch() {
    CustomPanelAPI p = root.createCustomPanel(SEARCH_W, FILTER_H, router);
    TooltipMakerAPI label = p.createUIElement(60f, FILTER_H, false);
    label.addPara(s("search"), CqUi.gray(), 5f);
    p.addUIElement(label).inTL(0f, 0f);
    TooltipMakerAPI el = p.createUIElement(SEARCH_W - 56f, FILTER_H, false);
    search = el.addTextField(SEARCH_W - 56f, FILTER_H, "graphics/fonts/insignia15LTaa.fnt", 0f);
    search.setMaxChars(30);
    search.setHandleCtrlV(true);
    el.addTooltipToPrevious(CqUi.tip(s("search_tip")), TooltipMakerAPI.TooltipLocation.BELOW);
    p.addUIElement(el).inTL(56f, 0f);
    root.addComponent(p).inTL(0f, HEAD_H + PAD);
  }

  private CustomPanelAPI buildFilters() {
    float fw = leftW - SEARCH_W - PAD;
    CustomPanelAPI p = root.createCustomPanel(fw, FILTER_H, router);
    float x = 0f;
    x = toggle(p, x, 44f, s("size_all"), sizeFilter == null, new Btn(Kind.SIZE, null), null);
    x = toggle(p, x, 72f, s("size_FRIGATE"), CqStartRules.FRIGATE.equals(sizeFilter),
        new Btn(Kind.SIZE, CqStartRules.FRIGATE), null);
    if (flavour.allowsSize(CqStartRules.DESTROYER)) {
      x = toggle(p, x, 82f, s("size_DESTROYER"), CqStartRules.DESTROYER.equals(sizeFilter),
          new Btn(Kind.SIZE, CqStartRules.DESTROYER), null);
    }
    float sortW = 150f;
    String src = sourceFilter < 0 ? s("source_all") : sources.get(sourceFilter);
    x = button(p, x + 6f, fw - x - 6f - sortW - 4f, src, new Btn(Kind.SOURCE, null), CqUi.tip(s(sourcesOpen ? "source_tip_close" : "source_tip")));
    button(p, x, sortW, sortLabel(), new Btn(Kind.SORT, null), null);
    return p;
  }

  private String sortLabel() {
    switch (sort) {
      case 1:
        return s("sort_dear");
      case 2:
        return s("sort_name");
      default:
        return s("sort_cheap");
    }
  }

  private float button(CustomPanelAPI p, float x, float bw, String text, Btn id,
      TooltipMakerAPI.TooltipCreator tip) {
    TooltipMakerAPI el = p.createUIElement(bw, BTN_H, false);
    el.addButton(text, id, CqUi.base(), CqUi.dark(), bw, BTN_H, 0f);
    if (tip != null) {
      el.addTooltipToPrevious(tip, TooltipMakerAPI.TooltipLocation.BELOW);
    }
    p.addUIElement(el).inTL(x, 1f);
    return x + bw + 4f;
  }

  private boolean takes(CqFleetPool.Entry e) {
    if (!flavour.allowsSize(e.size) || e.value > budget()) return false;
    return sizeFilter == null || sizeFilter.equals(e.size);
  }

  private List<CqFleetPool.Entry> shown() {
    String q = lastSearch.trim().toLowerCase(Locale.ROOT);
    List<CqFleetPool.Entry> out = new ArrayList<CqFleetPool.Entry>();
    for (CqFleetPool.Entry e : pool) {
      if (!takes(e)) continue;
      if (sourceFilter >= 0 && !sources.get(sourceFilter).equals(e.source)) continue;
      if (!q.isEmpty() && !e.name.toLowerCase(Locale.ROOT).contains(q)
          && !e.source.toLowerCase(Locale.ROOT).contains(q)) continue;
      out.add(e);
    }
    if (sort == 1) {
      Collections.reverse(out);
    } else if (sort == 2) {
      Collections.sort(out, new Comparator<CqFleetPool.Entry>() {
        @Override
        public int compare(CqFleetPool.Entry a, CqFleetPool.Entry b) {
          return a.name.compareToIgnoreCase(b.name);
        }
      });
    }
    return out;
  }

  private CustomPanelAPI buildPool() {
    long started = System.nanoTime();
    float ph = h - listTop;
    CustomPanelAPI p = root.createCustomPanel(leftW, ph, router);
    poolList = p.createUIElement(leftW, ph, true);
    poolRows.clear();
    poolBudget = budget();
    if (sourcesOpen) {
      buildSources(p);
      p.addUIElement(poolList).inTL(0f, 0f);
      return p;
    }
    List<CqFleetPool.Entry> rows = shown();
    makeMembers(rows);
    if (rows.isEmpty()) {
      poolList.addPara(s("pool_empty"), CqUi.gray(), 4f);
    }
    float rowW = leftW - 16f;
    for (final CqFleetPool.Entry e : rows) {
      CustomPanelAPI row = p.createCustomPanel(rowW, ROW_H, router);
      TooltipMakerAPI text = row.createUIElement(rowW - ICON - 190f, ROW_H, false);
      text.addPara(e.name, CqUi.text(), 6f);
      String size = s("size_" + e.size);
      text.addPara(e.designation.isEmpty() || e.designation.equalsIgnoreCase(size) ? size
          : e.designation + ", " + size, CqUi.text(), 2f);
      text.addPara(e.source, CqUi.gray(), 2f);
      row.addUIElement(text).inTL(ICON + PAD, 0f);
      addIcon(row, e, 0f, rowW - 190f);

      TooltipMakerAPI cost = row.createUIElement(90f, ROW_H, false);
      LabelAPI c = cost.addPara(credits(e.value), CqUi.hl(), 26f);
      c.setAlignment(Alignment.RMID);
      row.addUIElement(cost).inTL(rowW - 180f, 0f);

      TooltipMakerAPI add = row.createUIElement(70f, BTN_H, false);
      ButtonAPI b = add.addButton(s("add"), new Btn(Kind.ADD, e), CqUi.base(), CqUi.dark(), 70f, BTN_H, 0f);
      add.addTooltipToPrevious(new CqUi.Tip(300f) {
        @Override
        public void createTooltip(TooltipMakerAPI tt, boolean expanded, Object param) {
          String why = addBlocked(e);
          tt.addPara(why == null ? s("add_tip") : why, CqUi.text(), 0f);
        }
      }, TooltipMakerAPI.TooltipLocation.LEFT);
      row.addUIElement(add).inTL(rowW - 74f, (ROW_H - BTN_H) / 2f);
      poolList.addCustom(row, 2f);
      poolRows.add(new PoolRow(e, b, c));
    }
    refreshPool();
    p.addUIElement(poolList).inTL(0f, 0f);
    Global.getLogger(CqFleetBuilderPanel.class).info("Captain's Quarters: fleet builder pool, "
        + rows.size() + " rows in " + (System.nanoTime() - started) / 1000000L + " ms");
    return p;
  }

  private void buildSources(CustomPanelAPI p) {
    int[] counts = new int[sources.size()];
    int all = 0;
    for (CqFleetPool.Entry e : pool) {
      if (!takes(e)) continue;
      all++;
      int i = sources.indexOf(e.source);
      if (i >= 0) counts[i]++;
    }
    poolList.addPara(s("source_pick"), CqUi.gray(), 4f);
    float bw = leftW - 24f;
    sourceButton(String.format(s("source_count"), s("source_all"), "" + all), -1, bw);
    for (int i = 0; i < sources.size(); i++) {
      if (counts[i] == 0) continue;
      sourceButton(String.format(s("source_count"), sources.get(i), "" + counts[i]), i, bw);
    }
  }

  private void sourceButton(String text, int index, float bw) {
    poolList.addButton(text, new Btn(Kind.PICK_SOURCE, Integer.valueOf(index)),
        index == sourceFilter ? CqUi.bright() : CqUi.base(), CqUi.dark(), Alignment.LMID,
        CutStyle.NONE, bw, BTN_H, 3f);
  }

  private void addIcon(CustomPanelAPI row, CqFleetPool.Entry e, float y, float cardW) {
    FleetMemberAPI m = member(e);
    if (m == null) {
      return;
    }
    TooltipMakerAPI el = row.createUIElement(ICON + 4f, ICON + 4f, false);
    List<FleetMemberAPI> one = new ArrayList<FleetMemberAPI>();
    one.add(m);
    el.addShipList(1, 1, ICON, CqUi.base(), one, 0f);
    row.addUIElement(el).inTL(0f, y + (ROW_H - ICON) / 2f - 2f);
    TooltipMakerAPI card = row.createUIElement(cardW, ROW_H, false);
    if (cardStats == null) {
      cardStats = Global.getFactory().createPerson().getStats();
    }
    if (CqShipCard.attach(card, m, cardStats)) {
      row.addUIElement(card).inTL(0f, y);
    }
  }

  private FleetMemberAPI member(CqFleetPool.Entry e) {
    if (!members.containsKey(e.variantId)) {
      makeMembers(java.util.Collections.singletonList(e));
    }
    return members.get(e.variantId);
  }

  private void makeMembers(List<CqFleetPool.Entry> rows) {
    List<FleetMemberAPI> made = new ArrayList<FleetMemberAPI>();
    for (CqFleetPool.Entry e : rows) {
      if (members.containsKey(e.variantId)) continue;
      FleetMemberAPI m;
      try {
        m = Global.getFactory().createFleetMember(FleetMemberType.SHIP, e.variantId);
        if (displayFleet == null) {
          displayFleet = FleetFactoryV3.createEmptyFleet(Factions.PLAYER, FleetTypes.PATROL_SMALL, null);
          displayFleet.getCargo().addCrew(100000);
        }
        displayFleet.getFleetData().addFleetMember(m);
        made.add(m);
      } catch (Throwable t) {
        m = null;
      }
      members.put(e.variantId, m);
    }
    if (made.isEmpty()) {
      return;
    }
    displayFleet.getFleetData().setSyncNeeded();
    displayFleet.getFleetData().syncIfNeeded();
    for (FleetMemberAPI m : made) {
      m.updateStats();
      m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
    }
  }

  private CustomPanelAPI buildFleet() {
    float fh = h - HEAD_H - PAD;
    CustomPanelAPI p = root.createCustomPanel(rightW, fh, router);
    boolean officerRow = flavour.officerAllowed;
    float headH = officerRow ? 126f : 96f;
    TooltipMakerAPI head = p.createUIElement(rightW, headH, false);
    head.addSectionHeading(s("fleet_heading"), Alignment.MID, 0f);
    int budget = budget();
    int spent = spent();
    int left = budget - spent;
    Color shipsColor = picks.size() > flavour.maxShips ? CqUi.bad() : CqUi.hl();
    LabelAPI ships = head.addPara(String.format(s("limit_ships"), "" + picks.size(),
        "" + flavour.maxShips), CqUi.text(), 8f);
    ships.setHighlight("" + picks.size(), "" + flavour.maxShips);
    ships.setHighlightColors(shipsColor, CqUi.hl());
    LabelAPI value = head.addPara(String.format(s("limit_value"), credits(spent), credits(budget)),
        CqUi.text(), 4f);
    value.setHighlight(credits(spent), credits(budget));
    value.setHighlightColors(left < 0 ? CqUi.bad() : CqUi.hl(), CqUi.hl());
    LabelAPI rest = head.addPara(left >= 0 ? String.format(s("limit_left"), credits(left))
        : String.format(s("limit_over"), credits(-left)), CqUi.text(), 4f);
    rest.setHighlight(credits(Math.abs(left)));
    rest.setHighlightColors(left < 0 ? CqUi.bad() : CqUi.good());
    head.addPara(flavour == Flavour.LONE ? s("limit_sizes_lone") : s("limit_sizes_small"),
        CqUi.gray(), 4f);
    if (officerRow) {
      ButtonAPI box = head.addCheckbox(rightW - 8f, 22f, String.format(s("officer"),
          credits(budgets.officer()), credits(budgets.small)), new Btn(Kind.OFFICER, null),
          ButtonAPI.UICheckboxSize.SMALL, 8f);
      box.setChecked(officer);
      head.addTooltipToPrevious(CqUi.tip(s("officer_tip")), TooltipMakerAPI.TooltipLocation.BELOW);
    }
    p.addUIElement(head).inTL(0f, 0f);

    float noteH = 36f;
    float listH = fh - headH - FOOT_H - noteH - 2f * PAD;
    TooltipMakerAPI list = p.createUIElement(rightW, listH, true);
    if (picks.isEmpty()) {
      list.addPara(s("fleet_empty"), CqUi.gray(), 8f);
    }
    float rowW = rightW - 16f;
    for (int i = 0; i < picks.size(); i++) {
      CqFleetPool.Entry e = picks.get(i);
      CustomPanelAPI row = p.createCustomPanel(rowW, ROW_H, router);
      TooltipMakerAPI text = row.createUIElement(rowW - ICON - 110f, ROW_H, false);
      text.addPara(e.name, CqUi.text(), 6f);
      text.addPara(e.designation, CqUi.text(), 2f);
      String sub = credits(e.value);
      if (i == 0) {
        sub = s("flagship") + ", " + sub;
      } else if (i == 1 && officer && flavour.officerAllowed) {
        sub = s("officer_here") + ", " + sub;
      }
      text.addPara(sub, CqUi.gray(), 2f);
      row.addUIElement(text).inTL(ICON + PAD, 0f);
      addIcon(row, e, 0f, rowW - 110f);
      TooltipMakerAPI btns = row.createUIElement(96f, ROW_H, false);
      btns.addButton(s("remove"), new Btn(Kind.REMOVE, Integer.valueOf(i)), CqUi.base(), CqUi.dark(),
          96f, 22f, 4f);
      if (i > 0) {
        btns.addButton(s("make_flagship"), new Btn(Kind.FLAGSHIP, Integer.valueOf(i)), CqUi.base(),
            CqUi.dark(), 96f, 22f, 4f);
      }
      row.addUIElement(btns).inTL(rowW - 96f, 2f);
      list.addCustom(row, 2f);
    }
    p.addUIElement(list).inTL(0f, headH + PAD);

    TooltipMakerAPI note = p.createUIElement(rightW, noteH, false);
    note.addPara(s("damage_note"), CqUi.gray(), 0f);
    p.addUIElement(note).inTL(0f, fh - FOOT_H - noteH);

    float bw = 120f;
    TooltipMakerAPI foot = p.createUIElement(rightW, FOOT_H, false);
    foot.addButton(s("back"), new Btn(Kind.BACK, null), CqUi.base(), CqUi.dark(), bw, BTN_H, 0f);
    foot.getPrev().getPosition().inTL(0f, 3f);
    ButtonAPI go = foot.addButton(s("confirm"), new Btn(Kind.CONFIRM, null), CqUi.bright(),
        CqUi.dark(), rightW - bw - PAD, BTN_H, 0f);
    go.getPosition().inTL(bw + PAD, 3f);
    Problem problem = problem();
    CqUi.setEnabledSayingWhy(go, problem == null);
    if (problem != null) {
      foot.addTooltipToPrevious(CqUi.tip(problemText(problem)), TooltipMakerAPI.TooltipLocation.ABOVE);
    }
    p.addUIElement(foot).inTL(0f, fh - FOOT_H);
    return p;
  }

  private String problemText(Problem p) {
    switch (p) {
      case EMPTY:
        return s("problem_empty");
      case TOO_MANY:
        return flavour.maxShips == 1 ? s("problem_too_many_one")
            : String.format(s("problem_too_many"), "" + flavour.maxShips);
      case WRONG_SIZE:
        return s("problem_wrong_size");
      case OVER_BUDGET:
        return String.format(s("problem_over_budget"), credits(spent() - budget()));
      case OFFICER_NEEDS_SHIP:
        return s("problem_officer");
      default:
        return "";
    }
  }

  private static String s(String key) {
    return CqStrings.fleet(key);
  }

  private static String credits(int v) {
    return Misc.getDGSCredits(v);
  }

  private enum Kind {
    FLAVOUR, OFFICER, LOCATION, SIZE, SOURCE, PICK_SOURCE, SORT, ADD, REMOVE, FLAGSHIP, BACK, CONFIRM
  }

  private static final class Btn {
    final Kind kind;
    final Object arg;

    Btn(Kind kind, Object arg) {
      this.kind = kind;
      this.arg = arg;
    }
  }

  private final class Backdrop extends CqOpaque {
    @Override
    public void buttonPressed(Object buttonId) {
      pressed(buttonId);
    }

    @Override
    public void advance(float amount) {
      if (search == null || root == null) {
        return;
      }
      String now = search.getText();
      if (now == null) {
        now = "";
      }
      if (!now.equals(pendingSearch)) {
        pendingSearch = now;
        searchStill = 0f;
        return;
      }
      if (now.equals(lastSearch)) {
        return;
      }
      searchStill += amount;
      if (searchStill >= SEARCH_PAUSE) {
        lastSearch = now;
        sourcesOpen = false;
        poolPanel = replace(poolPanel, buildPool(), 0f, listTop);
      }
    }
  }

  private final class Router extends BaseCustomUIPanelPlugin {
    @Override
    public void buttonPressed(Object buttonId) {
      pressed(buttonId);
    }
  }
}
