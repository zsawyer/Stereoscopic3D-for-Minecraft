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

import org.lwjgl.opengl.GL11;

import zsawyer.mods.stereoscopic3d.Stereoscopic3DConstants.Eye;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 
 * @author zsawyer
 */
public class ScalingRenderer extends TickBasedRenderer {

    public float xScale = 1f;
    public float yScale = 1f;
    public int scaledWidth;
    public int scaledHeight;

    @SideOnly(Side.CLIENT)
    public ScalingRenderer(float xScale, float yScale)
    {
        super();
        this.xScale = xScale;
        this.yScale = yScale;
        resize();
    }

    @Override
    public synchronized void prepareFrame()
    {
    }

    @Override
    public synchronized void cleanUpAfterFrame()
    {
        if (isPlaying())
        {
            GL11.glFlush();
        }
    }

    @Override
    public void moveCamera(float xCorrectionFactor)
    {
        if (isPlaying())
        {
            setupView(false);

            super.moveCamera(xCorrectionFactor);
        }
    }

    public void setupView(boolean eager)
    {
        if ((currentEye == Eye.LEFT) ^ swapSides)
        {
            setupImageOne(eager);
        }
        else
        {
            setupImageTwo(eager);
        }
    }

    protected void setupImageOne(boolean eager)
    {
        setupView(0, 0, scaledWidth, scaledHeight, eager);
    }

    protected void setupImageTwo(boolean eager)
    {
        setupView(mc.displayWidth - scaledWidth, mc.displayHeight - scaledHeight, scaledWidth, scaledHeight, eager);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void init()
    {
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void resize()
    {
        scaledWidth = scaledWidth();
        scaledHeight = scaledHeight();
    }

    private int scaledWidth()
    {
        return Math.round(mc.displayWidth * xScale);
    }

    private int scaledHeight()
    {
        return Math.round(mc.displayHeight * yScale);
    }

    /**
     * 
     * @author zsawyer
     */
    public static class SideBySideRenderer extends ScalingRenderer {
        @SideOnly(Side.CLIENT)
        public SideBySideRenderer()
        {
            super(0.5f, 1f);
        }

        @Override
        public void setupImageTwo(boolean eager)
        {
            setupView(mc.displayWidth / 2, 0, scaledWidth, scaledHeight, eager);
        }
    }

    /**
     * 
     * @author zsawyer
     */
    public static class TopBottomRenderer extends ScalingRenderer {
        @SideOnly(Side.CLIENT)
        public TopBottomRenderer()
        {
            super(1f, 0.5f);
        }

        @Override
        public void setupImageTwo(boolean eager)
        {
            setupView(0, mc.displayHeight / 2, scaledWidth, scaledHeight, eager);
        }
    }

}
