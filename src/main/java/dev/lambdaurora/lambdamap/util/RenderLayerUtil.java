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

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Today in "How to commit crimes in a Fabric environment: MethodHandles and VarHandles"
 * <p>
 * I swear it better be just Proguard doing shit to visibility and not something intended. >:(
 *
 * @author LambdAurora
 */
public final class RenderLayerUtil {
    private static final MethodHandle OF_METHOD;
    private static final MethodHandle MULTI_PHASE_PARAMETERS_BUILDER_METHOD;
    private static final MethodHandle MULTI_PHASE_PARAMETERS_BUILD_METHOD;
    private static final VarHandle RENDER_PHASE_COLOR_SHADER;
    private static final MethodHandle MULTI_PHASE_PARAMETERS_BUILDER_SHADER_METHOD;

    private RenderLayerUtil() {
        throw new UnsupportedOperationException("Cannot instantiate RenderLayerUtil as it only contains static definitions.");
    }

    public static RenderLayer newRenderLayer(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize,
                                             Object phaseData) {
        try {
            return (RenderLayer) OF_METHOD.invoke(name, vertexFormat, drawMode, expectedBufferSize, phaseData);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Couldn't instantiate render layer", throwable);
        }
    }

    public static MultiPhaseParametersBuilder newMultiPhaseParametersBuilder() {
        try {
            return new MultiPhaseParametersBuilder(MULTI_PHASE_PARAMETERS_BUILDER_METHOD.invoke());
        } catch (Throwable throwable) {
            throw new IllegalStateException("Couldn't instantiate a MultiPhaseParameters builder.", throwable);
        }
    }

    public static Object getColorShader() {
        return RENDER_PHASE_COLOR_SHADER.get();
    }

    static {
        var lookup = MethodHandles.lookup();

        Method m = null;
        for (var declaredMethod : RenderLayer.class.getDeclaredMethods()) {
            int mods = declaredMethod.getModifiers();
            if (Modifier.isStatic(mods)) {
                var paramTypes = declaredMethod.getParameterTypes();
                if (paramTypes.length == 5 && paramTypes[0] == String.class) {
                    declaredMethod.setAccessible(true);
                    m = declaredMethod;
                    break;
                }
            }
        }

        if (m == null)
            throw new IllegalStateException("Could not find RenderLayer#of method.");

        try {
            OF_METHOD = lookup.unreflect(m);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        Class<?> multiPhaseParametersClass = null;
        for (var subClass : RenderLayer.class.getDeclaredClasses()) {
            if (!subClass.isEnum() && Modifier.isFinal(subClass.getModifiers()) && !RenderLayer.class.isAssignableFrom(subClass)) {
                multiPhaseParametersClass = subClass;
            }
        }

        if (multiPhaseParametersClass == null)
            throw new IllegalStateException("Could not find RenderLayer$MultiPhaseParameters class.");

        m = null;
        for (var declaredMethod : multiPhaseParametersClass.getDeclaredMethods()) {
            int mods = declaredMethod.getModifiers();
            if (Modifier.isStatic(mods)) {
                var paramTypes = declaredMethod.getParameterTypes();
                if (paramTypes.length == 0) {
                    declaredMethod.setAccessible(true);
                    m = declaredMethod;
                    break;
                }
            }
        }

        if (m == null)
            throw new IllegalStateException("Could not find RenderLayer$MultiPhaseParameters#builder method.");

        try {
            MULTI_PHASE_PARAMETERS_BUILDER_METHOD = lookup.unreflect(m);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        var builderSubClass = m.getReturnType();
        m = null;
        for (var declaredMethod : builderSubClass.getDeclaredMethods()) {
            if (declaredMethod.getReturnType() == multiPhaseParametersClass) {
                var paramTypes = declaredMethod.getParameterTypes();
                if (paramTypes.length == 1 && paramTypes[0] == boolean.class) {
                    declaredMethod.setAccessible(true);
                    m = declaredMethod;
                    break;
                }
            }
        }

        if (m == null)
            throw new IllegalStateException("Could not find RenderLayer$MultiPhaseParameters$Builder#build method.");

        try {
            MULTI_PHASE_PARAMETERS_BUILD_METHOD = lookup.unreflect(m);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        var mappingsResolver = FabricLoader.getInstance().getMappingResolver();
        var fieldName = mappingsResolver.mapFieldName("intermediary",
                "net.minecraft.class_4668", "field_29442", "Lnet/minecraft/class_4668$class_5942;");

        try {
            var colorShaderField = RenderPhase.class.getDeclaredField(fieldName);
            colorShaderField.setAccessible(true);
            RENDER_PHASE_COLOR_SHADER = lookup.unreflectVarHandle(colorShaderField);

            var shaderMethod = builderSubClass.getMethod(
                    mappingsResolver.mapMethodName("intermediary", "net.minecraft.class_1921$class_4688$class_4689",
                            "method_34578", "(Lnet/minecraft/class_4668$class_5942;)Lnet/minecraft/class_1921$class_4688$class_4689;"),
                    colorShaderField.getType());
            shaderMethod.setAccessible(true);
            MULTI_PHASE_PARAMETERS_BUILDER_SHADER_METHOD = lookup.unreflect(shaderMethod);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * I fucking hate this.
     * <p>
     * Someone save me please.
     */
    public static final class MultiPhaseParametersBuilder {
        private final Object builder;

        private MultiPhaseParametersBuilder(Object builder) {
            this.builder = builder;
        }

        public MultiPhaseParametersBuilder shader(Object shader) {
            try {
                MULTI_PHASE_PARAMETERS_BUILDER_SHADER_METHOD.invoke(this.builder, shader);
            } catch (Throwable throwable) {
                throw new IllegalStateException("Couldn't set a shader to a MultiPhaseParameters.", throwable);
            }
            return this;
        }

        public Object build(boolean affectsOutline) {
            try {
                return MULTI_PHASE_PARAMETERS_BUILD_METHOD.invoke(this.builder, affectsOutline);
            } catch (Throwable throwable) {
                throw new IllegalStateException("Couldn't build a MultiPhaseParameters.", throwable);
            }
        }
    }
}
