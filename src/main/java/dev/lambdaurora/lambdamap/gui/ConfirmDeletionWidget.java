/*
 * Copyright (c) 2021-2022 LambdAurora <email@lambdaurora.dev>
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

import com.mojang.blaze3d.vertex.Tessellator;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.background.EmptyBackground;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ConfirmDeletionWidget extends SpruceContainerWidget {
	private MarkerListWidget.MarkerEntry markerEntry;

	public ConfirmDeletionWidget(MarkerTabWidget parent, Position position, int width, int height) {
		super(position, width, height);
		this.setBackground(EmptyBackground.EMPTY_BACKGROUND);
		int offset = 5;
		int spacing = 10;

		int fixedY = this.getHeight() / 3 * 2 - 10;
		int fixedWidth = this.getWidth() / 2 - 2 * spacing;

		SpruceButtonWidget deleteButton = new SpruceButtonWidget(Position.of(this, spacing + offset, fixedY),
				fixedWidth, 20, ScreenTexts.PROCEED, button -> {
			this.markerEntry.parent.removeMarker(markerEntry);
			parent.switchBack();
		});
		SpruceButtonWidget cancelButton = new SpruceButtonWidget(Position.of(this, getWidth() / 2 + spacing - offset, fixedY),
				fixedWidth, 20, ScreenTexts.CANCEL, button -> parent.switchBack());

		this.addChild(deleteButton);
		this.addChild(cancelButton);
	}

	@Override
	protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.forEach(child -> child.render(matrices, mouseX, mouseY, delta));

		VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBufferBuilder());

		String name = this.markerEntry.marker.getName() == null ? "" : this.markerEntry.marker.getName().getString();
		OrderedText prompt = Text.translatable("lambdamap.marker.confirm_deletion.prompt",
				Text.translatable("lambdamap.marker.confirm_deletion.prompt.action").formatted(Formatting.RED),
				name.equals("") ? Text.translatable("lambdamap.marker.confirm_deletion.prompt.unnamed") : name)
				.asOrderedText();

		DrawableHelper.drawCenteredTextWithShadow(matrices, this.client.textRenderer, prompt, this.getX() + this.getWidth() / 2, this.getHeight() / 3, 0xffffffff);

		immediate.draw();
	}

	public void setMarkerEntry(MarkerListWidget.MarkerEntry markerEntry) {
		this.markerEntry = markerEntry;
	}
}
