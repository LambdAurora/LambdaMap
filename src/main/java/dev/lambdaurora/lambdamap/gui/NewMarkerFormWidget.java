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

package dev.lambdaurora.lambdamap.gui;

import dev.lambdaurora.lambdamap.map.marker.MarkerManager;
import dev.lambdaurora.lambdamap.map.marker.MarkerSource;
import dev.lambdaurora.lambdamap.map.marker.MarkerType;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.SpruceTexts;
import dev.lambdaurora.spruceui.background.SimpleColorBackground;
import dev.lambdaurora.spruceui.util.SpruceUtil;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceNamedTextFieldWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import net.minecraft.item.map.MapIcon;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class NewMarkerFormWidget extends SpruceContainerWidget {
	private final MarkerTypeButton typeButton;
	private final SpruceNamedTextFieldWidget nameField;
	private final SpruceNamedTextFieldWidget xFieldWidget;
	private final SpruceNamedTextFieldWidget zFieldWidget;
	private SpruceButtonWidget doneButton;

	public NewMarkerFormWidget(Position position, int width, int height, MarkerManager markers, MarkerListWidget list) {
		super(position, width, height);

		this.setBackground(new SimpleColorBackground(0xe00a0a0a));

		int x = 7;
		int y = 3;
		this.typeButton = new MarkerTypeButton(Position.of(this, x, y + 13), MarkerType.getVanillaMarkerType(MapIcon.Type.TARGET_POINT), type -> {
			this.doneButton.setActive(true);
		});
		this.addChild(typeButton);

		x += this.typeButton.getWidth() + 6;

		this.nameField = new SpruceNamedTextFieldWidget(new SpruceTextFieldWidget(Position.of(this, x, y),
				width < 480 ? width - 48 : width / 2 - 48,
				20, new TranslatableText("lambdamap.marker.new.name")));
		this.nameField.setChangedListener(input -> this.doneButton.setActive(true));
		this.addChild(nameField);

		if (width < 480) {
			x = 6;
			y += this.nameField.getHeight() + 4;
		} else {
			x = width / 2;
		}

		this.xFieldWidget = new SpruceNamedTextFieldWidget(new SpruceTextFieldWidget(
				Position.of(this, x, y),
				width < 480 ? 64 : 48, 20,
				new TranslatableText("lambdamap.marker.new.x")));
		this.xFieldWidget.setChangedListener(input -> this.doneButton.setActive(true));
		this.setupCoordinatesField(this.xFieldWidget);
		this.addChild(xFieldWidget);

		this.zFieldWidget = new SpruceNamedTextFieldWidget(new SpruceTextFieldWidget(
				Position.of(this, x += this.xFieldWidget.getWidth() + 4, y),
				width < 480 ? 64 : 48, 20,
				new TranslatableText("lambdamap.marker.new.z")));
		this.zFieldWidget.setChangedListener(input -> this.doneButton.setActive(true));
		this.setupCoordinatesField(this.zFieldWidget);
		this.addChild(this.zFieldWidget);

		this.addChild(new SpruceButtonWidget(Position.of(this, x + this.zFieldWidget.getWidth() + 4, y + 13),
				80, 20, new TranslatableText("lambdamap.marker.new.player_pos"), btn -> {
			BlockPos pos = this.client.player.getBlockPos();
			this.xFieldWidget.setText(String.valueOf(pos.getX()));
			this.zFieldWidget.setText(String.valueOf(pos.getZ()));
		}));

		this.doneButton = new SpruceButtonWidget(Position.of(this, width - 52, y + 13),
				50, 20, SpruceTexts.GUI_DONE, btn -> {
			String text = this.nameField.getText();
			markers.addMarker(this.typeButton.getMarkerType(), MarkerSource.USER,
					parseInt(this.xFieldWidget), 0, parseInt(this.zFieldWidget),
					180.f, text.isEmpty() ? null : new LiteralText(text));
			list.rebuildList();
			this.init();
		});
		this.addChild(this.doneButton);

		this.init();
	}

	private void init() {
		this.typeButton.setMarkerType(MarkerType.getVanillaMarkerType(MapIcon.Type.TARGET_POINT));
		this.nameField.setText("");
		this.xFieldWidget.setText("0");
		this.zFieldWidget.setText("0");

		this.doneButton.setActive(false);
	}

	private int parseInt(SpruceNamedTextFieldWidget textFieldWidget) {
		return SpruceUtil.parseIntFromString(textFieldWidget.getText());
	}

	private void setupCoordinatesField(SpruceNamedTextFieldWidget textField) {
		textField.setTextPredicate(SpruceTextFieldWidget.INTEGER_INPUT_PREDICATE);
		textField.setRenderTextProvider((displayedText, offset) -> {
			try {
				Integer.parseInt(textField.getText());
				return OrderedText.method_30747(displayedText, Style.EMPTY);
			} catch (NumberFormatException e) {
				return OrderedText.method_30747(displayedText, Style.EMPTY.withColor(Formatting.RED));
			}
		});
	}
}
