package dev.lambdaurora.lambdamap.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.background.EmptyBackground;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

public class ConfirmDeletionWidget extends SpruceContainerWidget {
    private MarkerListWidget.MarkerEntry markerEntry;

    public ConfirmDeletionWidget(MarkerTabWidget parent, Position position, int width, int height) {
        super(position, width, height);
        this.setBackground(EmptyBackground.EMPTY_BACKGROUND);
        SpruceButtonWidget cancelButton = new SpruceButtonWidget(Position.of(this, 10, this.getHeight() / 3 * 2 - 10), this.getWidth() / 2 - 20, 20, new LiteralText("Cancel"), button -> {
            parent.switchBack();
        });
        SpruceButtonWidget deleteButton = new SpruceButtonWidget(Position.of(this, getWidth() / 2 + 10, this.getHeight() / 3 * 2 - 10), this.getWidth() / 2 - 20, 20, new LiteralText("I'm sure"), button -> {
            this.markerEntry.parent.removeMarker(markerEntry);
            parent.switchBack();
        });
        this.addChild(cancelButton);
        this.addChild(deleteButton);
    }

    @Override
    protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.forEach(child -> child.render(matrices, mouseX, mouseY, delta));

        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

        String name = this.markerEntry.marker.getName() == null ? "Unnamed Marker" : this.markerEntry.marker.getName().asString();
        OrderedText prompt = Text.of("Are you sure you want to delete " + name + "?").asOrderedText();

        DrawableHelper.drawCenteredTextWithShadow(matrices, this.client.textRenderer, prompt, this.getX() + this.getWidth() / 2, this.getHeight() / 3, 0xffffffff);

        immediate.draw();
    }

    public void setMarkerEntry(MarkerListWidget.MarkerEntry markerEntry) {
        this.markerEntry = markerEntry;
    }
}
