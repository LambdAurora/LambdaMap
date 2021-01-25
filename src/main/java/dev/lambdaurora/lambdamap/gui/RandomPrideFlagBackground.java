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

import me.lambdaurora.spruceui.background.Background;
import me.lambdaurora.spruceui.background.SimpleColorBackground;
import me.lambdaurora.spruceui.util.ColorUtil;
import me.lambdaurora.spruceui.widget.SpruceWidget;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;

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
    private static final int[] TRANS_FLAG = new int[]{0xff5bcefa, 0xfff5a9b8, 0xffffffff, 0xfff5a9b8, 0xff5bcefa};
    private static final int[][] FLAGS = new int[][]{
            {}, // Show nothing
            {0xff292929, 0xff784e16, 0xffd62900, 0xfffe8a00, 0xffffd800, 0xff3da542, 0xff5541ef, 0xff5541ef}, // LGBTQ+
            {0xffd52d00, 0xffff9a56, 0xffffffff, 0xffd362a4, 0xff8a0253}, // lesbian
            {0xffd60270, 0xffd60270, 0xff9b4f96, 0xff0038a8, 0xff0038a8}, // bi
            TRANS_FLAG,
            {0xfffff42f, 0xffffffff, 0xff9c59d1, 0xff292929}, // non-binary
            makeThreeStripesFlag(0xffb57edc, 0xffffffff, 0xff4a8123), // gender queer
            {0xffffd800, 0xffffd800, 0xff663399, 0xffffd800, 0xffffd800, 0xff663399, 0xffffd800, 0xffffd800}, // intersex
            {0xff292929, 0xffc6c6c6, 0xffffffff, 0xff810281}, // asexual
            makeThreeStripesFlag(0xff21b1ff, 0xffffd800, 0xffff218c), // pan
            makeThreeStripesFlag(0xfff61cb9, 0xff07d569, 0xff1c92f6), // polysexual
            makeThreeStripesFlag(0xff5541ef, 0xffd62900, 0xff292929), // polyamorous
            {0xff000000, 0xffb9b9b9, 0xffffffff, 0xffb8f483, 0xffffffff, 0xffb9b9b9, 0xff000000}, // agender
            makeDemiFlag(0xfff5a9b8), // demi girl
            makeDemiFlag(0xff5bcefa), // demi boy
            {0xffff75a2, 0xffffffff, 0xffbe18d6, 0xff000000, 0xff333ebd} // genderfluid
    };

    private final int[] flag;

    public RandomPrideFlagBackground(int[] flag) {
        this.flag = flag;
    }

    @Override
    public void render(MatrixStack matrices, SpruceWidget widget, int vOffset, int mouseX, int mouseY, float delta) {
        int x = widget.getX();
        int y = widget.getY();

        if (this.flag.length > 1) {
            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
            VertexConsumer vertices = immediate.getBuffer(RenderLayer.of("lambdamap:random_pride_flag_background",
                    VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES, 256,
                    RenderLayer.MultiPhaseParameters.builder().build(false)));

            int width = widget.getWidth();
            int height = widget.getHeight();

            float partHeight = height / (this.flag.length - 1.f);

            // First one
            float rightY = y;
            float leftY = y;

            int[] color = ColorUtil.unpackARGBColor(this.flag[0]);
            vertices.vertex(x + width, rightY + partHeight, 0).color(color[0], color[1], color[2], color[3]).next();
            vertices.vertex(x + width, rightY, 0).color(color[0], color[1], color[2], color[3]).next();
            vertices.vertex(x, leftY, 0).color(color[0], color[1], color[2], color[3]).next();

            rightY += partHeight;

            for (int i = 1; i < this.flag.length - 1; i++) {
                color = ColorUtil.unpackARGBColor(this.flag[i]);

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
            color = ColorUtil.unpackARGBColor(this.flag[this.flag.length - 1]);
            vertices.vertex(x + width, rightY, 0).color(color[0], color[1], color[2], color[3]).next();
            vertices.vertex(x, leftY, 0).color(color[0], color[1], color[2], color[3]).next();
            vertices.vertex(x, y + height, 0).color(color[0], color[1], color[2], color[3]).next();

            immediate.draw();
        } else if (this.flag.length == 1) {
            DrawableHelper.fill(matrices, x, y, x + widget.getWidth(), y + widget.getHeight(), this.flag[0]);
        }

        SECOND_LAYER.render(matrices, widget, vOffset, mouseX, mouseY, delta);
    }

    private static int[] makeThreeStripesFlag(int first, int second, int third) {
        return new int[]{first, first, second, second, third, third};
    }

    private static int[] makeDemiFlag(int accentColor) {
        return new int[]{0xff838483, 0xffc6c6c6, accentColor, 0xffffffff, accentColor, 0xffc6c6c6, 0xff838483};
    }

    /**
     * Returns a random pride flag as background.
     *
     * @return the background
     */
    public static Background random() {
        int index = RANDOM.nextInt(FLAGS.length);
        return new RandomPrideFlagBackground(FLAGS[index]);
    }
}
