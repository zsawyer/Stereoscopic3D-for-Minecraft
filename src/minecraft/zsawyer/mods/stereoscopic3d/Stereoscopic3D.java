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

import java.util.Arrays;

import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import zsawyer.mods.stereoscopic3d.Stereoscopic3DConstants.ConfigKeys;
import zsawyer.mods.stereoscopic3d.Stereoscopic3DConstants.Format;
import zsawyer.mods.stereoscopic3d.renderers.StereoscopicRenderer;
import zsawyer.mods.stereoscopic3d.renderers.StereoscopicRendererFactory;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = "Stereoscopic3D", name = "Stereoscopic3D Renderer", version = "0.0.2")
@NetworkMod(clientSideRequired = true, serverSideRequired = false)
public class Stereoscopic3D {

    // The instance of your mod that Forge uses.
    @Instance("Stereoscopic3D")
    public static Stereoscopic3D instance;

    // private Stereoscopic3DRenderer renderer;
    public StereoscopicRenderer renderer;

    private Property rendererFormat;

    private Property swapSides;

    @PreInit
    public void preInit(FMLPreInitializationEvent event)
    {
        if (FMLCommonHandler.instance().getSide().isServer() && ObfuscationReflectionHelper.obfuscation)
            throw new RuntimeException("Stereoscopic3D should not be installed on a server!");

        initConfig(event);
        initRenderer();
    }

    private void initConfig(FMLPreInitializationEvent event)
    {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());

        config.load();

        rendererFormat = config.get(Configuration.CATEGORY_GENERAL, ConfigKeys.format.toString(), Format.Interlaced.toString(),
                "sterescopic 3D output format to be used" + System.lineSeparator() + "available values: " + Arrays.toString(Format.values()));
        swapSides = config.get(Configuration.CATEGORY_GENERAL, ConfigKeys.swapSides.toString(), false, "whether to swap the left and right image");

        config.save();
    }

    private void initRenderer()
    {
        renderer = StereoscopicRendererFactory.getRenderer(rendererFormat);
        renderer.init(swapSides.getBoolean(false));
    }

    @SideOnly(Side.CLIENT)
    @Init
    public void load(FMLInitializationEvent event)
    {
        renderer.engage();
    }

    @SideOnly(Side.CLIENT)
    @PostInit
    public void postInit(FMLPostInitializationEvent event)
    {
        // Stub Method
    }
}