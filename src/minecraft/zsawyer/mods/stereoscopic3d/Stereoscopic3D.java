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

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import zsawyer.mods.stereoscopic3d.renderers.Interlaced3DRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = "Stereoscopic3D", name = "Stereoscopic3D Renderer Addon", version = "0.0.1")
@NetworkMod(clientSideRequired = true, serverSideRequired = false)
public class Stereoscopic3D {

	public Eye currentEye = Eye.LEFT;

	// The instance of your mod that Forge uses.
	@Instance("Stereoscopic3D")
	public static Stereoscopic3D instance;

	// private Stereoscopic3DRenderer renderer;
	private Interlaced3DRenderer renderer;

	@PreInit
	public void preInit(FMLPreInitializationEvent event) {
		if (FMLCommonHandler.instance().getSide().isServer()
				&& ObfuscationReflectionHelper.obfuscation)
			throw new RuntimeException(
					"Stereoscopic3D should not be installed on a server!");
	}

	@SideOnly(Side.CLIENT)
	@Init
	public void load(FMLInitializationEvent event) {
		renderer = new Interlaced3DRenderer();
		renderer.init();

		// MinecraftForge.EVENT_BUS.register(new Interlaced3DRenderer());
	}

	@SideOnly(Side.CLIENT)
	@PostInit
	public void postInit(FMLPostInitializationEvent event) {
		// Stub Method
	}

	public void prepareFrame() {
		renderer.prepareFrame(currentEye);

	}

	public void resize() {
		renderer.init();
	}

	public void moveCamera() {
		GL11.glTranslatef((float) (-(currentEye.ordinal() * 2 - 1)) * 0.07F,
				0.0F, 0.0F);
	}

	public void revertCamera() {
		GL11.glTranslatef((float) (currentEye.ordinal() * 2 - 1) * 0.1F, 0.0F,
				0.0F);
	}

	public static void checkGLError() {
		int glError = GL11.glGetError();
		if (glError != GL11.GL_NO_ERROR) {
			throw new RuntimeException("GL Error: "
					+ GLU.gluErrorString(glError) + "(" + glError + ")");
		}
	}

	public void drawStereoscopicSquare() {
		renderer.drawStereoscopicSquare(currentEye);
	}

	public void debugSwapBuffers(Eye eye) {
		if (eye == currentEye) {
			try {
				GL11.glFlush();
				Display.swapBuffers();
				Thread.sleep(1000);
				Display.swapBuffers();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void cleanUpAfterFrames() {
		renderer.cleanUpAfterFrames();		
	}
}