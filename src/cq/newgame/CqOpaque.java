package cq.newgame;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;

class CqOpaque extends BaseCustomUIPanelPlugin {
  private float x;
  private float y;
  private float w;
  private float h;

  @Override
  public void positionChanged(PositionAPI position) {
    x = position.getX();
    y = position.getY();
    w = position.getWidth();
    h = position.getHeight();
  }

  @Override
  public void renderBelow(float alphaMult) {
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(0f, 0f, 0f, alphaMult);
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(x, y + h);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(x + w, y);
    GL11.glEnd();
    GL11.glPopAttrib();
  }
}
