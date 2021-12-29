package dev.lambdaurora.lambdamap.gui;

import dev.lambdaurora.lambdamap.LambdaMap;
import dev.lambdaurora.lambdamap.map.marker.MarkerManager;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.navigation.NavigationDirection;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;

public class MarkerTabWidget extends SpruceContainerWidget {
    private final MarkerListWidget list;
    private final NewMarkerFormWidget newMarkerFormWidget;
    private final ConfirmDeletionWidget confirmDeletionWidget;
    private boolean promptForDeletion = false;

    public MarkerTabWidget(LambdaMap mod, Position position, int width, int height) {
        super(position, width, height);

        MarkerManager markers = mod.getMap().getMarkerManager();

        int newMarkerFormHeight = width < 480 ? 80 : 40;
        list = new MarkerListWidget(this, Position.origin(), width, height - newMarkerFormHeight, markers);
        newMarkerFormWidget = new NewMarkerFormWidget(Position.of(this, 0, list.getHeight()), width, newMarkerFormHeight, markers, list);
        confirmDeletionWidget = new ConfirmDeletionWidget(this, Position.origin(), width, height);

        addChild(list);
        addChild(newMarkerFormWidget);
        addChild(confirmDeletionWidget);
        switchBack();
    }

    public void promptForDeletion(MarkerListWidget.MarkerEntry entry) {
        this.promptForDeletion = true;
        list.setVisible(false);
        newMarkerFormWidget.setVisible(false);
        confirmDeletionWidget.setVisible(true);
        confirmDeletionWidget.setMarkerEntry(entry);
    }

    public void switchBack() {
        this.promptForDeletion = false;
        list.setVisible(true);
        newMarkerFormWidget.setVisible(true);
        confirmDeletionWidget.setVisible(false);
    }

    @Override
    public boolean onNavigation(NavigationDirection direction, boolean tab) {
        switchBack();
        return super.onNavigation(direction, tab);
    }


}
