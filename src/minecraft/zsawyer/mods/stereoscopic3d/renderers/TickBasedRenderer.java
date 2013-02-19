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

import java.util.EnumSet;

import zsawyer.mods.stereoscopic3d.Stereoscopic3DConstants.Eye;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 
 * @author zsawyer
 */
public abstract class TickBasedRenderer extends StereoscopicRenderer implements ITickHandler {

    public TickBasedRenderer()
    {
        super();
    }

    public abstract void prepareFrame();

    public abstract void cleanUpAfterFrame();

    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData)
    {
        if (engaged)
        {
            if (tickData.length == 1 && tickData[0] instanceof Float)
            {
                currentEye = Eye.LEFT;
                prepareFrame();
            }
        }
    }

    /**
     * repeat rendering on the other eye
     */
    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData)
    {
        if (engaged)
        {
            if (tickData.length == 1 && tickData[0] instanceof Float)
            {
                cleanUpAfterFrame();

                currentEye = Eye.RIGHT;
                prepareFrame();

                mc.entityRenderer.updateCameraAndRender((Float) tickData[0]);
            }

            cleanUpAfterFrame();
        }
    }

    @Override
    public EnumSet<TickType> ticks()
    {
        return EnumSet.of(TickType.RENDER);
    }

    @Override
    public String getLabel()
    {
        return this.getClass().getSimpleName();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void engage()
    {
        super.engage();
        TickRegistry.registerTickHandler(this, Side.CLIENT);
    }

}