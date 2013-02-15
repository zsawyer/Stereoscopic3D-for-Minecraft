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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.GLU;

import zsawyer.mods.stereoscopic3d.Eye;
import zsawyer.mods.stereoscopic3d.Stereoscopic3D;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class Interlaced3DRenderer {

	private final static Minecraft mc = FMLClientHandler.instance().getClient();

	// @ForgeSubscribe
	// public void render(RenderWorldLastEvent event) {
	//
	// }

	/** int: GL11.[...] constants that apply for glEnable/glDisable */
	private Map<Integer, Boolean> previousState;
	private int[] enablesToCheck = new int[] { GL11.GL_TEXTURE_2D,
			GL11.GL_ALPHA_TEST, GL11.GL_COLOR_MATERIAL, GL11.GL_BLEND,
			GL11.GL_DEPTH_TEST, GL12.GL_RESCALE_NORMAL, GL11.GL_CULL_FACE,
			GL11.GL_COLOR_MATERIAL, GL11.GL_LIGHTING, GL11.GL_COLOR_LOGIC_OP,
			GL11.GL_FOG, GL11.GL_POLYGON_OFFSET_FILL, GL11.GL_LIGHT0,
			GL11.GL_LIGHT1 };

	public void prepareFrame(Eye eye) {
		if (this.mc.theWorld != null && !this.mc.skipRenderWorld) {			
			if (eye == Eye.LEFT) {
				// left image
				GL11.glDrawBuffer(GL11.GL_BACK);
				GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0x01);
				// Clear the screen and depth buffer
				GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT
						| GL11.GL_COLOR_BUFFER_BIT);
			} else if (eye == Eye.RIGHT) {
				// right image
				GL11.glDrawBuffer(GL11.GL_BACK);
				GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0x01);
				// Clear the depth buffer
				GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
			}
		} else {
			GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0x01);
		}
	}

	@SideOnly(Side.CLIENT)
	public void init() {
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

		// TODO: remove this debug call
		printStencilMap();
	}

	private void setupView(int width, int height) {
		/*
		 * // attempt to inject "withStencilBits(8)" try { DisplayMode mode =
		 * Display.getDisplayMode(); String title = Display.getTitle();
		 * 
		 * Display.destroy();
		 * 
		 * Display.setParent(mc.mcCanvas); Display.setTitle(title);
		 * Display.setDisplayMode(mode); if (mc_func_90020_K() > 0) {
		 * Display.sync(EntityRenderer.performanceToFps(mc_func_90020_K())); }
		 * Display.setFullscreen(mc.isFullScreen());
		 * Display.setVSyncEnabled(mc.gameSettings.enableVsync);
		 * 
		 * Display.create((new PixelFormat()).withDepthBits(24)
		 * .withStencilBits(8)); } catch (LWJGLException e) { throw new
		 * RuntimeException("could not create stencil buffer"); }
		 */

		GL11.glViewport(0, 0, width, height);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GLU.gluOrtho2D(0f, width, 0f, height);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
	}

	/**
	 * private function copied from Minecraft.java (MC 1.4.7 | MCP 7.26a)
	 */
	private static int mc_func_90020_K() {
		return mc.currentScreen != null
				&& mc.currentScreen instanceof GuiMainMenu ? 2
				: mc.gameSettings.limitFramerate;
	}

	private void prepareStencilBuffer() {
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

	private void drawStencilPattern(int width, int height) {
		// drawing stencil pattern
		// alfa is 0 not to interfere with alpha tests
		GL11.glColor4f(1, 1, 1, 0);
		// draw lines (actually using quads)
		// for anticipating aliasing and drawing pixels at precise locations I
		// find QUADS are easier to figure out than LINES (lines would require
		// an offset of +0.5)
		GL11.glBegin(GL11.GL_QUADS);
		{
			for (int gliY = 0; gliY <= height; gliY += 2) {
				GL11.glVertex2f(0, gliY);
				GL11.glVertex2f(0, gliY + 1);
				GL11.glVertex2f(width, gliY + 1);
				GL11.glVertex2f(width, gliY);
			}
		}
		GL11.glEnd();
	}

	private void savePreviousState() {
		previousState = new HashMap<Integer, Boolean>();
		for (int enabler : enablesToCheck) {
			previousState.put(enabler, GL11.glIsEnabled(enabler));
			checkGLError();
		}
	}

	private void initStencilState() {
		for (int enabler : enablesToCheck) {
			GL11.glDisable(enabler);
			checkGLError();
		}
	}

	private void recoverPreviousState() {
		for (int enabler : enablesToCheck) {
			if (previousState.get(enabler)) {
				GL11.glEnable(enabler);
				checkGLError();
			}
		}
	}

	/**
	 * Checks for an OpenGL error. If there is one, throws a runtime exception.
	 */
	private static void checkGLError() throws RuntimeException {
		int var2 = GL11.glGetError();

		if (var2 != 0) {
			String var3 = GLU.gluErrorString(var2);
			throw new RuntimeException("GL ERROR: " + var3 + "(" + var2 + ")");
		}
	}

	private void printStencilMap() {
		try {
			int stencilWidth = mc.displayWidth;
			int stencilHeight = mc.displayHeight;
			IntBuffer stencilMap = ByteBuffer.allocateDirect(
					stencilWidth * stencilHeight * 4).asIntBuffer();
			stencilMap.rewind();

			// stencilMap.order(ByteOrder.nativeOrder());
			GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, GL11.GL_TRUE);
			GL11.glReadPixels(0, 0, stencilWidth, stencilHeight,
					GL11.GL_STENCIL_INDEX, GL11.GL_INT, stencilMap);
			checkGLError();

			stencilMap.rewind();
			PrintWriter out = new PrintWriter("stencilMap.bitmap");
			int yPos = 0;
			while (stencilMap.hasRemaining()) {
				// int c = Math.abs(stencilMap.get()) % 10;
				int c = stencilMap.get();
				out.print((c & 1));
				if (yPos >= stencilWidth - 1) {
					out.println("");
					yPos = -1;
				}
				yPos++;
			}
			// out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void drawStereoscopicSquare(Eye eye) {
		int width = mc.displayWidth;
		int height = mc.displayHeight;

		savePreviousState();
		initStencilState();

		setupView(width, height);

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
		Stereoscopic3D.checkGLError();

		GL11.glFlush();

		recoverPreviousState();
	}

	public void cleanUpAfterFrames() {
		GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0x01);
	}
}
