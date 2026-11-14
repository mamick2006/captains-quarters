package cq.campaign;

public final class CqEffectSlots {

  public static final int NONE = 0;
  public static final int FIRST = 1;
  public static final int SECOND = 2;

  private CqEffectSlots() {
  }

  public static <E extends Enum<E>> int slotOf(E kind, E kind2, E wanted) {
    if (wanted == null) {
      return NONE;
    }
    if (kind == wanted) {
      return FIRST;
    }
    if (kind2 == wanted) {
      return SECOND;
    }
    return NONE;
  }

  public static <E extends Enum<E>> boolean carries(E kind, E kind2, E wanted) {
    return slotOf(kind, kind2, wanted) != NONE;
  }

  public static <E extends Enum<E>> float pick(E kind, E kind2, E wanted, float first,
      float second) {
    switch (slotOf(kind, kind2, wanted)) {
      case FIRST:
        return first;
      case SECOND:
        return second;
      default:
        return 0f;
    }
  }
}
