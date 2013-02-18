/*
 Stereoscopic3D-for-Minecraft - A Minecraft mod to allow stereoscopic 3D imaging.
 Copyright 2013 zsawyer (https://github.com/zsawyer, http://sourceforge.net/users/zsawyer)

 This file is part of Stereoscopic3D-for-Minecraft
 (https://github.com/zsawyer/Stereoscopic3D-for-Minecraft).

 Stereoscopic3D-for-Minecraft is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Stereoscopic3D-for-Minecraft is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with Stereoscopic3D-for-Minecraft.  If not, see <http://www.gnu.org/licenses/>.

 */

package zsawyer.mods.stereoscopic3d.renderers;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.PerspectiveSetupEvent;
import net.minecraftforge.client.event.ScreenResizeEvent;
import net.minecraftforge.client.event.UpdateChunksEvent;
import net.minecraftforge.client.event.UpdateFogColorEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.ForgeSubscribe;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.GLU;

import zsawyer.mods.stereoscopic3d.DebugUtil;
import zsawyer.mods.stereoscopic3d.Stereoscopic3DConstants.Eye;
import cpw.mods.fml.client.FMLClientHandler;

/**
 * 
 * @author zsawyer
 */
public abstract class StereoscopicRenderer {

    protected final static Minecraft mc = FMLClientHandler.instance().getClient();
    public boolean engaged = false;

    public Eye currentEye = Eye.LEFT;
    public boolean swapSides = false;
    /**
     * list of GL11.[...]-constants (1st parameter) which apply for
     * glEnable/glDisable and their previous enabled-state (2nd Parameter)
     */
    private Map<Integer, Boolean> previousState;
    protected int[] enablesToCheck = new int[] { GL11.GL_TEXTURE_2D, GL11.GL_ALPHA_TEST, GL11.GL_COLOR_MATERIAL, GL11.GL_BLEND, GL11.GL_DEPTH_TEST,
            GL12.GL_RESCALE_NORMAL, GL11.GL_CULL_FACE, GL11.GL_LIGHTING, GL11.GL_COLOR_LOGIC_OP, GL11.GL_FOG, GL11.GL_POLYGON_OFFSET_FILL, GL11.GL_LIGHT0,
            GL11.GL_LIGHT1 };

    public void init(boolean swapSides)
    {
        this.swapSides = swapSides;
        init();
    }

    public abstract void init();

    public abstract void resize();

    public void engage()
    {
        engaged = true;
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void disengage()
    {
        engaged = false;
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @ForgeSubscribe
    public void afterScreenResize(ScreenResizeEvent event)
    {
        resize();
    }

    @ForgeSubscribe
    public void beforePerspectiveSetup(PerspectiveSetupEvent.Before event)
    {
        moveCamera(event.xCorrectionFactor);
    }

    @ForgeSubscribe
    public void afterPerspectiveSetup(PerspectiveSetupEvent.After event)
    {
        revertCamera(event.xCorrectionFactor);
    }

    public void moveCamera(float xCorrectionFactor)
    {
        GL11.glTranslatef((float) (-(currentEye.ordinal() * 2 - 1)) * xCorrectionFactor, 0.0F, 0.0F);
    }

    public void revertCamera(float xCorrectionFactor)
    {
        GL11.glTranslatef((float) (currentEye.ordinal() * 2 - 1) * xCorrectionFactor, 0.0F, 0.0F);
    }

    @ForgeSubscribe
    public void onUpdateFogColor(UpdateFogColorEvent event)
    {
        cancelIfNotFirstEye(event);
    }

    @ForgeSubscribe
    public void onUpdateFogColor(UpdateChunksEvent event)
    {
        cancelIfNotFirstEye(event);
    }

    /**
     * Cancels the given event if the current eye position is not the first to
     * be rendered. This avoids inconsistencies across the different
     * perspectives because objects have moved or are rendered differently.
     * 
     * @param event
     *            event to possibly be canceled
     */
    protected void cancelIfNotFirstEye(Event event)
    {
        if (currentEye != Eye.LEFT)
        {
            event.setCanceled(true);
        }
    }

    protected void setupView(int width, int height)
    {
        GL11.glViewport(0, 0, width, height);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluOrtho2D(0f, width, 0f, height);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    protected void savePreviousState()
    {
        previousState = new HashMap<Integer, Boolean>();
        for (int enabler : enablesToCheck)
        {
            previousState.put(enabler, GL11.glIsEnabled(enabler));
            DebugUtil.checkGLError();
        }
    }

    protected void recoverPreviousState()
    {
        for (int enabler : enablesToCheck)
        {
            if (previousState.get(enabler))
            {
                GL11.glEnable(enabler);
                DebugUtil.checkGLError();
            }
        }
    }
}
