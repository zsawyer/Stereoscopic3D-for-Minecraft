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

import java.util.logging.Level;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraftforge.common.Property;
import zsawyer.mods.stereoscopic3d.Stereoscopic3D;
import zsawyer.mods.stereoscopic3d.Stereoscopic3DConstants.ConfigKeys;
import zsawyer.mods.stereoscopic3d.Stereoscopic3DConstants.Format;
import zsawyer.mods.stereoscopic3d.renderers.ScalingRenderer.SideBySideRenderer;
import zsawyer.mods.stereoscopic3d.renderers.ScalingRenderer.TopBottomRenderer;
import zsawyer.mods.stereoscopic3d.renderers.StencilingRenderer.CheckerboardRenderer;
import zsawyer.mods.stereoscopic3d.renderers.StencilingRenderer.InterlacedRenderer;

/**
 * 
 * @author zsawyer
 */
@SideOnly(Side.CLIENT)
public class StereoscopicRendererFactory {
    private StereoscopicRendererFactory()
    {
    }

    @SideOnly(Side.CLIENT)
    public static StereoscopicRenderer getRenderer(Format format)
    {
        switch (format)
        {
        case Interlaced:
            return new InterlacedRenderer();
        case SideBySide:
            return new SideBySideRenderer();
        case TopBottom:
            return new TopBottomRenderer();
        case Checkerboard:
            return new CheckerboardRenderer();
        default:
            break;
        }

        throw new RuntimeException("Renderer for format '" + format + "' is not yet implemented or registered.");
    }

    @SideOnly(Side.CLIENT)
    public static StereoscopicRenderer getRenderer(Property formatFromConfig)
    {
        if (!formatFromConfig.getName().equals(ConfigKeys.format.toString()))
        {
            throw new IllegalArgumentException("Property must be of name '" + ConfigKeys.format + "' but was '" + formatFromConfig.getName() + "'.");
        }

        Format format = Format.valueOf(formatFromConfig.value);
        Stereoscopic3D.LOG.log(Level.INFO, "Using " + format + " renderer.");
        return getRenderer(format);
    }
}
