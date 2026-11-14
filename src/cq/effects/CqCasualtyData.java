package cq.effects;

import cq.effects.pure.CqCasualtyMath;

public class CqCasualtyData {

  private transient CqCasualtyMath.Encounter encounter;
  private transient Object battle;

  public CqCasualtyMath.Encounter open(Object ofBattle) {
    if (encounter == null || battle != ofBattle) {
      encounter = new CqCasualtyMath.Encounter();
      battle = ofBattle;
    }
    return encounter;
  }

  public CqCasualtyMath.Encounter end(Object ofBattle) {
    if (encounter == null || battle != ofBattle) {
      return null;
    }
    CqCasualtyMath.Encounter ended = encounter;
    encounter = null;
    battle = null;
    return ended;
  }
}
