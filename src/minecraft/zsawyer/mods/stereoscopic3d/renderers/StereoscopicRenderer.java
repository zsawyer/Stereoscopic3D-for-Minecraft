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

import net.minecraftforge.client.event.PerspectiveSetupEvent;
import net.minecraftforge.client.event.ScreenResizeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.ForgeSubscribe;

import org.lwjgl.opengl.GL11;

import zsawyer.mods.stereoscopic3d.Stereoscopic3DConstants.Eye;

/**
 * 
 * @author zsawyer
 */
public abstract class StereoscopicRenderer {

    public Eye currentEye = Eye.LEFT;
    public boolean swapSides = false;

    public void init(boolean swapSides)
    {
        this.swapSides = swapSides;
        init();
    }

    public abstract void init();

    public abstract void resize();

    public void engage()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void disengage()
    {
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
}
