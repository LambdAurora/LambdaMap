package dev.lambdaurora.lambdamap.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.background.EmptyBackground;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.SpruceWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceParentWidget;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Matrix4f;

public class ConfirmDeletionWidget extends SpruceContainerWidget {
    private MarkerListWidget.MarkerEntry markerEntry;

    public ConfirmDeletionWidget(MarkerTabWidget parent, Position position, int width, int height) {
        super(position, width, height);
        this.setBackground(EmptyBackground.EMPTY_BACKGROUND);
        int x = 25;
        SpruceButtonWidget cancelButton = new SpruceButtonWidget(Position.of(this, 10, this.getHeight() / 3 * 2 - 10), this.getWidth() / 2 - 20, 20, new LiteralText("Cancel"), button -> {
            parent.switchBack();
        });
        SpruceButtonWidget deleteButton = new SpruceButtonWidget(Position.of(this, getWidth() / 2 + 10, this.getHeight() / 3 * 2 - 10), this.getWidth() / 2 - 20, 20, new LiteralText("I'm sure"), button -> {
            markerEntry.parent.removeMarker(markerEntry);
            parent.switchBack();
        });
        addChild(cancelButton);
        addChild(deleteButton);
    }

    @Override
    protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.forEach(child -> child.render(matrices, mouseX, mouseY, delta));
        int light = LightmapTextureManager.pack(15, 15);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        Matrix4f model = matrices.peek().getPositionMatrix();
        String name = markerEntry.marker.getName() == null ? "Unnamed" : markerEntry.marker.getName().asString();
        this.client.textRenderer.draw("Are you sure you want to delete " + name + "?", this.getX() + 15, height / 3.f, 0xffffffff, true, model, immediate, false, 0, light);
        immediate.draw();
    }

    public void setMarkerEntry(MarkerListWidget.MarkerEntry markerEntry) {
        this.markerEntry = markerEntry;
    }
}
