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
import java.nio.IntBuffer;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.EntityRenderer;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import zsawyer.mods.stereoscopic3d.DebugUtil;
import zsawyer.mods.stereoscopic3d.MinecraftCopy;
import zsawyer.mods.stereoscopic3d.Stereoscopic3DConstants.Eye;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 
 * @author zsawyer
 */
public abstract class StencilingRenderer extends TickBasedRenderer {

    boolean reinitPending = false;

    private final int stencilTestWidth = 1;
    private final int stencilTestHeight = 1;
    private final IntBuffer stencilMap = ByteBuffer.allocateDirect(stencilTestWidth * stencilTestHeight * 4).asIntBuffer();

    @SideOnly(Side.CLIENT)
    @Override
    public void init()
    {
        createNewDisplay();
        reinit();
    }

    protected void createNewDisplay()
    {
        try
        {
            DisplayMode mode = Display.getDisplayMode();
            String title = Display.getTitle();

            Display.destroy();

            Display.setParent(mc.mcCanvas);
            Display.setTitle(title);
            Display.setDisplayMode(mode);
            if (MinecraftCopy.mc_func_90020_K() > 0)
            {
                Display.sync(EntityRenderer.performanceToFps(MinecraftCopy.mc_func_90020_K()));
            }
            Display.setFullscreen(mc.isFullScreen());
            Display.setVSyncEnabled(mc.gameSettings.enableVsync);

            Display.create((new PixelFormat()).withDepthBits(24).withStencilBits(8));
            MinecraftCopy.loadScreen();
            this.mc.fontRenderer = new FontRenderer(this.mc.gameSettings, "/font/default.png", this.mc.renderEngine, false);
            this.mc.standardGalacticFontRenderer = new FontRenderer(this.mc.gameSettings, "/font/alternate.png", this.mc.renderEngine, false);
        }
        catch (LWJGLException e)
        {
            throw new RuntimeException("could not create stencil buffer");
        }
    }

    protected synchronized void reinit()
    {
        reinitPending = true;

        if (!Display.isDirty())
        {
            int width = mc.displayWidth;
            int height = mc.displayHeight;

            savePreviousState();
            initStencilState();

            setupView(width, height);

            prepareStencilBuffer();

            drawStencilPattern(width, height);

            // disabling changes in stencil buffer
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            GL11.glFlush();

            recoverPreviousState();

            reinitPending = false;
        }
    }

    protected void initStencilState()
    {
        for (int enabler : enablesToCheck)
        {
            GL11.glDisable(enabler);
            DebugUtil.checkGLError();
        }
    }

    protected void prepareStencilBuffer()
    {
        GL11.glDrawBuffer(GL11.GL_BACK);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xff);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);// | GL11.GL_COLOR_BUFFER_BIT
        // | GL11.GL_DEPTH_BUFFER_BIT);

        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
        // to avoid interaction with stencil content
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0x01);
    }

    protected abstract void drawStencilPattern(int width, int height);

    @SideOnly(Side.CLIENT)
    public void resize()
    {
        reinit();
    }

    protected boolean isReinitRequired()
    {
        stencilMap.rewind();

        GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, GL11.GL_TRUE);
        GL11.glReadPixels(0, 0, stencilTestWidth, stencilTestHeight, GL11.GL_STENCIL_INDEX, GL11.GL_INT, stencilMap);
        DebugUtil.checkGLError();

        return stencilMap.get(0) != 1;
    }

    public void prepareFrame()
    {
        if (this.mc.theWorld != null && !this.mc.skipRenderWorld)
        {
            if (reinitPending || isReinitRequired())
            {
                reinit();
            }

            if (currentEye == Eye.LEFT ^ swapSides)
            {
                setupImageOne();
            }
            else
            {
                setupImageTwo();
            }
        }
        else
        {
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0x01);
        }
    }

    protected void setupImageOne()
    {
        setupImage(GL11.GL_EQUAL);
    }

    protected void setupImageTwo()
    {
        setupImage(GL11.GL_NOTEQUAL);
    }

    protected void setupImage(int stencilFunc)
    {
        GL11.glDrawBuffer(GL11.GL_BACK);
        GL11.glStencilFunc(stencilFunc, 1, 0x01);
        // Clear the screen and depth buffer
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);// | GL11.GL_COLOR_BUFFER_BIT);
    }

    public void cleanUpAfterFrame()
    {
        if (this.mc.theWorld != null && !this.mc.skipRenderWorld)
        {
            if (currentEye == Eye.LEFT)
            {
                GL11.glFlush();
            }
            else
            {
                GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0x01);
            }
        }
    }

    /**
     * 
     * @author zsawyer
     */
    public static class InterlacedRenderer extends StencilingRenderer {
        @SideOnly(Side.CLIENT)
        public InterlacedRenderer()
        {
            super();
        }

        @Override
        protected void drawStencilPattern(int width, int height)
        {
            GL11.glColor4f(1, 1, 1, 0);
            // draw lines (actually using quads)
            // for anticipating aliasing and drawing pixels at precise locations
            // I find QUADS are easier to figure out than LINES (lines would
            // require
            // an offset of +0.5)
            GL11.glBegin(GL11.GL_QUADS);
            {
                for (int gliY = 0; gliY <= height; gliY += 2)
                {
                    GL11.glVertex2f(0, gliY);
                    GL11.glVertex2f(0, gliY + 1);
                    GL11.glVertex2f(width, gliY + 1);
                    GL11.glVertex2f(width, gliY);
                }
            }
            GL11.glEnd();
            DebugUtil.checkGLError();
        }
    }

    /**
     * 
     * @author zsawyer
     */
    public static class CheckerboardRenderer extends StencilingRenderer {
        @SideOnly(Side.CLIENT)
        public CheckerboardRenderer()
        {
            super();
        }

        @Override
        protected void drawStencilPattern(int width, int height)
        {
            GL11.glColor4f(1, 1, 1, 0);
            GL11.glBegin(GL11.GL_POINTS);
            {
                for (int rows = 0; rows <= height; rows += 1)
                {
                    for (int cols = 0; cols <= width; cols += 1)
                    {
                        if (!(((cols % 2) == 0) ^ ((rows % 2) == 0)))
                        {
                            GL11.glVertex2f(cols + 0.5f, rows + 0.5f);
                        }
                    }
                }
            }
            GL11.glEnd();
            DebugUtil.checkGLError();
        }
    }
}
