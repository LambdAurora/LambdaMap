package dev.lambdaurora.lambdamap.gui;

import dev.lambdaurora.lambdamap.LambdaMap;
import dev.lambdaurora.lambdamap.map.marker.MarkerManager;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;

public class MarkerTabWidget extends SpruceContainerWidget {
	private final MarkerListWidget list;
	private final NewMarkerFormWidget newMarkerFormWidget;
	private final ConfirmDeletionWidget confirmDeletionWidget;

	public MarkerTabWidget(LambdaMap mod, Position position, int width, int height) {
		super(position, width, height);

		MarkerManager markers = mod.getMap().getMarkerManager();

		int newMarkerFormHeight = width < 480 ? 80 : 40;
		this.list = new MarkerListWidget(this, Position.origin(), width, height - newMarkerFormHeight, markers);
		this.newMarkerFormWidget = new NewMarkerFormWidget(Position.of(this, 0, list.getHeight()), width, newMarkerFormHeight, markers, list);
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
	public void setFocused(boolean focused) {
		if(!focused) {
			this.switchBack();
		}
		super.setFocused(focused);
	}
}
