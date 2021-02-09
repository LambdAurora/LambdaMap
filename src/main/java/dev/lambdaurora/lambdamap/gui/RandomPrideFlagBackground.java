/*
 * Copyright (c) 2020 LambdAurora <aurora42lambda@gmail.com>
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

package dev.lambdaurora.lambdamap.gui;

import dev.lambdaurora.spruceui.background.Background;
import dev.lambdaurora.spruceui.background.SimpleColorBackground;
import dev.lambdaurora.spruceui.util.ColorUtil;
import dev.lambdaurora.spruceui.widget.SpruceWidget;
import io.github.queerbric.pride.PrideFlag;
import io.github.queerbric.pride.PrideFlagShapes;
import io.github.queerbric.pride.PrideFlags;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.Random;

/**
 * Displays a pride flag.
 * <p>
 * If you have an issue with this, I don't care.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class RandomPrideFlagBackground implements Background {
    private static final Background SECOND_LAYER = new SimpleColorBackground(0xe0101010);
    private static final Random RANDOM = new Random();

    private final PrideFlag flag;

    public RandomPrideFlagBackground(PrideFlag flag) {
        this.flag = flag;
    }

    @Override
    public void render(MatrixStack matrices, SpruceWidget widget, int vOffset, int mouseX, int mouseY, float delta) {
        int x = widget.getX();
        int y = widget.getY();

        if (this.flag.getShape() == PrideFlagShapes.get(new Identifier("pride", "horizontal_stripes"))) {
            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
            VertexConsumer vertices = immediate.getBuffer(RenderLayer.of("lambdamap:random_pride_flag_background",
                    VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES, 256,
                    RenderLayer.MultiPhaseParameters.builder().build(false)));

            int width = widget.getWidth();
            int height = widget.getHeight();

            float partHeight = height / (this.flag.getColors().size() - 1.f);

            // First one
            float rightY = y;
            float leftY = y;

            int[] color = ColorUtil.unpackARGBColor(this.flag.getColors().getInt(0));
            vertices.vertex(x + width, rightY + partHeight, 0).color(color[0], color[1], color[2], color[3]).next();
            vertices.vertex(x + width, rightY, 0).color(color[0], color[1], color[2], color[3]).next();
            vertices.vertex(x, leftY, 0).color(color[0], color[1], color[2], color[3]).next();

            rightY += partHeight;

            for (int i = 1; i < this.flag.getColors().size() - 1; i++) {
                color = ColorUtil.unpackARGBColor(this.flag.getColors().getInt(i));

                vertices.vertex(x + width, rightY + partHeight, 0).color(color[0], color[1], color[2], color[3]).next();
                vertices.vertex(x + width, rightY, 0).color(color[0], color[1], color[2], color[3]).next();
                vertices.vertex(x, leftY, 0).color(color[0], color[1], color[2], color[3]).next();

                vertices.vertex(x + width, rightY + partHeight, 0).color(color[0], color[1], color[2], color[3]).next();
                vertices.vertex(x, leftY, 0).color(color[0], color[1], color[2], color[3]).next();
                vertices.vertex(x, leftY + partHeight, 0).color(color[0], color[1], color[2], color[3]).next();

                rightY += partHeight;
                leftY += partHeight;
            }

            // Last one
            color = ColorUtil.unpackARGBColor(this.flag.getColors().getInt(this.flag.getColors().size() - 1));
            vertices.vertex(x + width, rightY, 0).color(color[0], color[1], color[2], color[3]).next();
            vertices.vertex(x, leftY, 0).color(color[0], color[1], color[2], color[3]).next();
            vertices.vertex(x, y + height, 0).color(color[0], color[1], color[2], color[3]).next();

            immediate.draw();
        } else this.flag.render(matrices, x, y, widget.getWidth(), widget.getHeight());

        SECOND_LAYER.render(matrices, widget, vOffset, mouseX, mouseY, delta);
    }

    /**
     * Returns a random pride flag as background.
     *
     * @return the background
     */
    public static Background random() {
        return new RandomPrideFlagBackground(PrideFlags.getRandomFlag(RANDOM));
    }
}
