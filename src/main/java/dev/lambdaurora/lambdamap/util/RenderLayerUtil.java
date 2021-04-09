/*
 * Copyright (c) 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.lambdamap.util;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Today in "How to commit crimes in a Fabric environment: MethodHandles"
 *
 * @author LambdAurora
 */
public final class RenderLayerUtil {
    private static final MethodHandle OF_METHOD;

    private RenderLayerUtil() {
        throw new UnsupportedOperationException("Cannot instantiate RenderLayerUtil as it only contains static definitions.");
    }

    public static RenderLayer newRenderLayer(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize,
                                             RenderLayer.MultiPhaseParameters phaseData) {
        try {
            return (RenderLayer) OF_METHOD.invoke(name, vertexFormat, drawMode, expectedBufferSize, phaseData);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Couldn't instanciate render layer", throwable);
        }
    }

    static {
        Method m = null;
        for (Method declaredMethod : RenderLayer.class.getDeclaredMethods()) {
            int mods = declaredMethod.getModifiers();
            if (Modifier.isStatic(mods) && Modifier.isPrivate(mods)) {
                Class<?>[] paramTypes = declaredMethod.getParameterTypes();
                if (paramTypes.length == 5 && paramTypes[0] == String.class) {
                    declaredMethod.setAccessible(true);
                    m = declaredMethod;
                }
            }
        }

        if (m == null)
            throw new IllegalStateException("Could not find RenderLayer#of method.");

        try {
            OF_METHOD = MethodHandles.lookup().unreflect(m);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
