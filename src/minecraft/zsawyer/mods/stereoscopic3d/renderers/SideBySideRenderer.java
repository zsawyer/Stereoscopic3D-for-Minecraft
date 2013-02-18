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

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.GLAllocation;

import org.lwjgl.opengl.GL11;

import zsawyer.mods.stereoscopic3d.DebugUtil;
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
public class SideBySideRenderer extends StereoscopicRenderer implements ITickHandler {

    private static final int DATA_TYPE = GL11.GL_UNSIGNED_BYTE;
    private static final int BYTES_PER_COMPONENT = 1;

    private static final int PIXEL_FORMAT = GL11.GL_RGB;
    private static final int NUMBER_OF_COMPONENTS = 3;

    private final Map<Eye, ByteBuffer> frameBuffers;
    private int previousDrawBuffer;
    private int previousReadBuffer;
    private int lastWidth;
    private int lastHeight;
    private int previousZoomX;
    private int previousZoomY;
    private boolean previousAlphaTest;
    private boolean resolutionChanged;

    public float zoomFactorX()
    {
        return 0.5f;
    }

    public float zoomFactorY()
    {
        return 1f;
    }

    public float secondImageStartingPositionX()
    {
        return lastWidth / 2;
    }

    public float secondImageStartingPositionY()
    {
        return 0;
    }

    public SideBySideRenderer()
    {
        super();

        updateDimensions();

        frameBuffers = new HashMap<Eye, ByteBuffer>(Eye.values().length);
        updateBuffers();
    }

    @Override
    public void init()
    {
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void engage()
    {
        super.engage();
        TickRegistry.registerTickHandler(this, Side.CLIENT);
    }

    @Override
    public void resize()
    {
        resolutionChanged = resolutionChanged();
    }

    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData)
    {
        if (engaged)
        {
            if (this.mc.theWorld != null && !this.mc.skipRenderWorld)
            {
                this.currentEye = Eye.LEFT;
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
            if (this.mc.theWorld != null && !this.mc.skipRenderWorld)
            {
                if (tickData.length == 1 && tickData[0] instanceof Float)
                {
                    GL11.glFlush();
                    saveFrame();

                    this.currentEye = Eye.RIGHT;
                    mc.entityRenderer.updateCameraAndRender((Float) tickData[0]);
                    saveFrame();

                    mergeFrames();
                }
            }
        }
    }

    private void mergeFrames()
    {
        savePreviousState();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        setupView(lastWidth, lastHeight);

        GL11.glDrawBuffer(GL11.GL_BACK);
        GL11.glClearColor(0F, 0F, 0F, 1F);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glFlush();

        GL11.glPixelZoom(zoomFactorX(), zoomFactorY());
        synchronized (frameBuffers)
        {
            setRasterPosition(true);
            GL11.glDrawPixels(lastWidth, lastHeight, PIXEL_FORMAT, DATA_TYPE, frameBuffers.get(Eye.LEFT));
            DebugUtil.checkGLError();
            GL11.glFlush();

            setRasterPosition(false);
            GL11.glDrawPixels(lastWidth, lastHeight, PIXEL_FORMAT, DATA_TYPE, frameBuffers.get(Eye.RIGHT));
            DebugUtil.checkGLError();
        }
        GL11.glFlush();

        recoverPreviousState();
    }

    private void setRasterPosition(boolean primaryImage)
    {
        if (primaryImage ^ swapSides)
        {
            GL11.glRasterPos2f(0, 0);
        }
        else
        {
            GL11.glRasterPos2f(secondImageStartingPositionX(), secondImageStartingPositionY());
        }
    }

    private void saveFrame()
    {
        savePreviousState();

        synchronized (frameBuffers)
        {
            GL11.glReadBuffer(GL11.GL_BACK);
            GL11.glReadPixels(0, 0, lastWidth, lastHeight, PIXEL_FORMAT, DATA_TYPE, frameBuffers.get(currentEye));
        }
        recoverPreviousState();
    }

    private synchronized void allocateNewBuffer(Eye eye)
    {
        ByteBuffer newBuffer = GLAllocation.createDirectByteBuffer((lastWidth * lastHeight) * NUMBER_OF_COMPONENTS * BYTES_PER_COMPONENT);// ByteBuffer.allocateDirect
        // newBuffer.order(ByteOrder.nativeOrder());
        newBuffer.rewind();
        frameBuffers.put(eye, newBuffer);
    }

    private boolean resolutionChanged()
    {
        if (lastWidth != mc.displayWidth || lastHeight != mc.displayHeight)
        {
            updateDimensions();

            updateBuffers();

            return true;
        }

        return false;
    }

    private synchronized void updateBuffers()
    {
        synchronized (frameBuffers)
        {
            for (Eye eye : Eye.values())
            {
                allocateNewBuffer(eye);
            }
        }
    }

    private void updateDimensions()
    {
        lastWidth = mc.displayWidth;
        lastHeight = mc.displayHeight;
    }

    @Override
    protected void savePreviousState()
    {
        super.savePreviousState();
        previousDrawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
        previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        previousZoomX = GL11.glGetInteger(GL11.GL_ZOOM_X);
        previousZoomY = GL11.glGetInteger(GL11.GL_ZOOM_Y);
    }

    @Override
    protected void recoverPreviousState()
    {
        super.recoverPreviousState();
        GL11.glDrawBuffer(previousDrawBuffer);
        GL11.glReadBuffer(previousReadBuffer);
        GL11.glPixelZoom(previousZoomX, previousZoomY);
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

    public int getLastWidth()
    {
        return lastWidth;
    }

    public int getLastHeight()
    {
        return lastHeight;
    }
}
