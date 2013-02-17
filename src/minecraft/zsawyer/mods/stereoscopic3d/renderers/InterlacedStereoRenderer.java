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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.client.event.UpdateChunksEvent;
import net.minecraftforge.client.event.UpdateFogColorEvent;
import net.minecraftforge.event.ForgeSubscribe;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;

import zsawyer.mods.stereoscopic3d.DebugUtil;
import zsawyer.mods.stereoscopic3d.MinecraftCopy;
import zsawyer.mods.stereoscopic3d.Stereoscopic3DConstants.Eye;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class InterlacedStereoRenderer extends StereoscopicRenderer implements ITickHandler {

    private final static Minecraft mc = FMLClientHandler.instance().getClient();

    /**
     * list of GL11.[...]-constants (1st parameter) which apply for
     * glEnable/glDisable and their previous enabled-state (2nd Parameter)
     */
    private Map<Integer, Boolean> previousState;
    private int[] enablesToCheck = new int[] { GL11.GL_TEXTURE_2D, GL11.GL_ALPHA_TEST, GL11.GL_COLOR_MATERIAL, GL11.GL_BLEND, GL11.GL_DEPTH_TEST,
            GL12.GL_RESCALE_NORMAL, GL11.GL_CULL_FACE, GL11.GL_COLOR_MATERIAL, GL11.GL_LIGHTING, GL11.GL_COLOR_LOGIC_OP, GL11.GL_FOG,
            GL11.GL_POLYGON_OFFSET_FILL, GL11.GL_LIGHT0, GL11.GL_LIGHT1 };

    private boolean reinitPending = false;

    private void prepareFrame(Eye eye)
    {
        currentEye = eye;

        if (this.mc.theWorld != null && !this.mc.skipRenderWorld)
        {
            if (eye == Eye.LEFT ^ swapSides)
            {
                // left image
                GL11.glDrawBuffer(GL11.GL_BACK);
                GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0x01);
                // Clear the screen and depth buffer
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
            }
            else if (eye == Eye.RIGHT ^ swapSides)
            {
                // right image
                GL11.glDrawBuffer(GL11.GL_BACK);
                GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0x01);
                // Clear the depth buffer
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            }
        }
        else
        {
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0x01);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void init()
    {
        loadDisplaySettings();
        reinit();
    }

    private void loadDisplaySettings()
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

    @SideOnly(Side.CLIENT)
    public synchronized void reinit()
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

    private void savePreviousState()
    {
        previousState = new HashMap<Integer, Boolean>();
        for (int enabler : enablesToCheck)
        {
            previousState.put(enabler, GL11.glIsEnabled(enabler));
            DebugUtil.checkGLError();
        }
    }

    private void initStencilState()
    {
        for (int enabler : enablesToCheck)
        {
            GL11.glDisable(enabler);
            DebugUtil.checkGLError();
        }
    }

    private void setupView(int width, int height)
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

    private void prepareStencilBuffer()
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

    private void drawStencilPattern(int width, int height)
    {
        GL11.glColor4f(1, 1, 1, 0);
        // draw lines (actually using quads)
        // for anticipating aliasing and drawing pixels at precise locations I
        // find QUADS are easier to figure out than LINES (lines would require
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

    private void recoverPreviousState()
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

    public void resize()
    {
        reinit();
    }

    /*
     * render on the first eye
     */
    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData)
    {
        if (reinitPending || isReinitRequired())
        {
            reinit();
        }

        if (type.contains(TickType.RENDER))
        {
            if (tickData.length == 1 && tickData[0] instanceof Float)
            {
                prepareFrame(Eye.LEFT);
            }
        }

    }

    private boolean isReinitRequired()
    {
        int stencilWidth = 1;
        int stencilHeight = 1;
        IntBuffer stencilMap = ByteBuffer.allocateDirect(stencilWidth * stencilHeight * 4).asIntBuffer();
        stencilMap.rewind();

        GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, GL11.GL_TRUE);
        GL11.glReadPixels(0, 0, stencilWidth, stencilHeight, GL11.GL_STENCIL_INDEX, GL11.GL_INT, stencilMap);
        DebugUtil.checkGLError();

        return stencilMap.get(0) != 1;
    }

    /**
     * repeat rendering on the other eye
     */
    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData)
    {
        if (this.mc.theWorld != null && !this.mc.skipRenderWorld)
        {
            if (type.contains(TickType.RENDER))
            {
                if (tickData.length == 1 && tickData[0] instanceof Float)
                {
                    GL11.glFlush();
                    prepareFrame(Eye.RIGHT);

                    mc.entityRenderer.updateCameraAndRender((Float) tickData[0]);
                }
            }
        }

        cleanUpAfterFrames();
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

    public void cleanUpAfterFrames()
    {
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0x01);
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

    @Override
    public void engage()
    {
        super.engage();
        TickRegistry.registerTickHandler(this, Side.CLIENT);
    }
}
