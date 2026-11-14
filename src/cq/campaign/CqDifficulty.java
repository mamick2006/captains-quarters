package cq.campaign;

public final class CqDifficulty {

  public static final String AUTO = "auto";

  public static final String SURVIVAL = "survival";
  public static final String NORMAL = "normal";
  public static final String STORY = "story";

  public static final String AUTO_LABEL = "Auto";
  public static final String SURVIVAL_LABEL = "Survival";
  public static final String NORMAL_LABEL = "Normal";
  public static final String STORY_LABEL = "Story";

  private CqDifficulty() {
  }

  public static String normalize(String stored) {
    if (stored != null) {
      if (stored.equalsIgnoreCase(SURVIVAL)) {
        return SURVIVAL;
      }
      if (stored.equalsIgnoreCase(NORMAL)) {
        return NORMAL;
      }
      if (stored.equalsIgnoreCase(STORY)) {
        return STORY;
      }
    }
    return AUTO;
  }

  public static String resolve(String normalized, boolean survivalModeGame) {
    if (SURVIVAL.equals(normalized) || NORMAL.equals(normalized) || STORY.equals(normalized)) {
      return normalized;
    }
    return survivalModeGame ? SURVIVAL : NORMAL;
  }
}
