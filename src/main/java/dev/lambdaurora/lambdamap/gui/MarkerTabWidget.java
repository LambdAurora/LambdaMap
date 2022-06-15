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

import dev.lambdaurora.lambdamap.LambdaMap;
import dev.lambdaurora.lambdamap.map.marker.MarkerManager;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.navigation.NavigationDirection;
import dev.lambdaurora.spruceui.navigation.NavigationUtils;
import dev.lambdaurora.spruceui.widget.SpruceWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;

import java.util.ArrayList;
import java.util.List;

public class MarkerTabWidget extends SpruceContainerWidget {
	private final List<SpruceWidget> nonModalChildren = new ArrayList<>();
	private final MarkerListWidget list;
	private final NewMarkerFormWidget newMarkerFormWidget;
	private final ConfirmDeletionWidget confirmDeletionWidget;

	public MarkerTabWidget(LambdaMap mod, Position position, int width, int height) {
		super(position, width, height);

		MarkerManager markers = mod.getMap().getMarkerManager();

		int newMarkerFormHeight = width < 480 ? 80 : 40;
		this.list = new MarkerListWidget(this, Position.origin(), width, height - newMarkerFormHeight, markers);
		this.newMarkerFormWidget = new NewMarkerFormWidget(Position.of(this, 0, list.getHeight()), width, newMarkerFormHeight, markers, list);
		nonModalChildren.add(list);
		nonModalChildren.add(newMarkerFormWidget);
		this.confirmDeletionWidget = new ConfirmDeletionWidget(this, Position.origin(), width, height);

		this.addChild(list);
		this.addChild(newMarkerFormWidget);
		this.addChild(confirmDeletionWidget);
		this.switchBack();
	}

	public void promptForDeletion(MarkerListWidget.MarkerEntry entry) {
		this.list.setVisible(false);
		this.newMarkerFormWidget.setVisible(false);
		this.confirmDeletionWidget.setVisible(true);
		this.confirmDeletionWidget.setMarkerEntry(entry);
	}

	public void switchBack() {
		this.list.setVisible(true);
		this.newMarkerFormWidget.setVisible(true);
		this.confirmDeletionWidget.setVisible(false);
	}

	@Override
	public boolean onNavigation(NavigationDirection direction, boolean tab) {
		boolean result;
		if(list.isVisible()) {
			result = NavigationUtils.tryNavigate(direction, tab, nonModalChildren, getFocused(), this::setFocused, false);
		} else {
			result = confirmDeletionWidget.onNavigation(direction, tab);
		}

		if (result)
			this.setFocused(true);
		return result;
	}

	@Override
	public void setFocused(boolean focused) {
		if(!focused) {
			this.switchBack();
		}
		super.setFocused(focused);
	}
}
