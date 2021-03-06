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

package zsawyer.mods.stereoscopic3d;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import zsawyer.mods.stereoscopic3d.Stereoscopic3DConstants.Eye;

import cpw.mods.fml.client.FMLClientHandler;

/**
 * 
 * @author zsawyer
 */
public class DebugUtil {
    private DebugUtil()
    {
    }

    /**
     * Checks for an OpenGL error. If there is one, throws a runtime exception.
     */
    public static void checkGLError() throws RuntimeException
    {
        int var2 = GL11.glGetError();

        if (var2 != 0)
        {
            String var3 = GLU.gluErrorString(var2);
            throw new RuntimeException("GL ERROR: " + var3 + "(" + var2 + ")");
        }
    }

    public static void debugSwapBuffers(Eye eye)
    {
        if (eye == Stereoscopic3D.instance.renderer.currentEye)
        {
            try
            {
                GL11.glFlush();
                Display.swapBuffers();
                Thread.sleep(1000);
                Display.swapBuffers();
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void drawStereoscopicSquare(Eye eye)
    {
        // switch position depending on eye
        int offset = 10 * eye.ordinal();
        // switch color depending on eye
        GL11.glColor3f(eye.ordinal(), 0, 1 - eye.ordinal());
        // draw quad
        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glVertex2f(100 + offset, 100);
            GL11.glVertex2f(100 + 200 + offset, 100);
            GL11.glVertex2f(100 + 200 + offset, 100 + 200);
            GL11.glVertex2f(100 + offset, 100 + 200);
        }
        GL11.glEnd();
        checkGLError();

        GL11.glFlush();
    }

    public static void printStencilMap()
    {
        try
        {
            Minecraft mc = FMLClientHandler.instance().getClient();

            int stencilWidth = mc.displayWidth;
            int stencilHeight = mc.displayHeight;
            IntBuffer stencilMap = ByteBuffer.allocateDirect(stencilWidth * stencilHeight * 4).asIntBuffer();
            stencilMap.rewind();

            // stencilMap.order(ByteOrder.nativeOrder());
            GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, GL11.GL_TRUE);
            GL11.glReadPixels(0, 0, stencilWidth, stencilHeight, GL11.GL_STENCIL_INDEX, GL11.GL_INT, stencilMap);
            checkGLError();

            stencilMap.rewind();
            PrintWriter out = new PrintWriter("stencilMap.bitmap");
            int yPos = 0;
            while (stencilMap.hasRemaining())
            {
                // int c = Math.abs(stencilMap.get()) % 10;
                int c = stencilMap.get();
                out.print((c & 1));
                if (yPos >= stencilWidth - 1)
                {
                    out.println("");
                    yPos = -1;
                }
                yPos++;
            }
            out.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
}
