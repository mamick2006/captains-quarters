package cq.topbar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public final class CqShipCard {

  private CqShipCard() {
  }

  private static final String FACTORY_CLASS = "com.fs.starfarer.ui.impl.StandardTooltipV2";
  private static final String FACTORY_METHOD = "createFleetMemberTooltipPreDeploy";

  private static boolean dead = false;
  private static Object factory;
  private static Object setTooltip;
  private static Class<?> setTooltipOwner;

  public static boolean attach(TooltipMakerAPI cell, FleetMemberAPI member) {
    return attach(cell, member, null);
  }

  public static boolean attach(TooltipMakerAPI cell, FleetMemberAPI member,
      MutableCharacterStatsAPI defaultStats) {
    if (dead || cell == null || member == null) {
      return false;
    }
    try {
      if (factory == null) {
        factory = CqCoreReflect.methodNamed(Class.forName(FACTORY_CLASS), FACTORY_METHOD, 2);
        if (factory == null) {
          throw new NoSuchMethodException(FACTORY_CLASS + "." + FACTORY_METHOD);
        }
      }
      if (setTooltipOwner != cell.getClass()) {
        Object m = CqCoreReflect.methodNamed(cell.getClass(), "setTooltip", 2);
        if (m == null) {
          throw new NoSuchMethodException("setTooltip(float, card) on " + cell.getClass().getName());
        }
        setTooltip = m;
        setTooltipOwner = cell.getClass();
      }
      MutableCharacterStatsAPI stats = member.getCaptain() != null ? member.getCaptain().getStats()
          : defaultStats != null ? defaultStats : Global.getFactory().createPerson().getStats();
      Object card = CqCoreReflect.invokeMethod(factory, null, new Object[] { member, stats });
      Class<?>[] pt = CqCoreReflect.paramTypes(setTooltip);
      if (pt.length != 2 || pt[0] != float.class || !pt[1].isInstance(card)) {
        throw new NoSuchMethodException("setTooltip(float, card) does not take "
            + (card == null ? "null" : card.getClass().getName()));
      }
      CqCoreReflect.invokeMethod(setTooltip, cell, new Object[] { Float.valueOf(0f), card });
      return true;
    } catch (Throwable t) {
      dead = true;
      Global.getLogger(CqShipCard.class).warn("Captain's Quarters: the game's ship card could not be"
          + " attached (" + t.getClass().getSimpleName() + ": " + t.getMessage() + "); the starting"
          + " fleet builder shows the basic ship card, without the Codex link.");
      return false;
    }
  }
}
