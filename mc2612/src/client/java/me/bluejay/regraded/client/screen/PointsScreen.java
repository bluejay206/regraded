package me.bluejay.regraded.client.screen;

import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Simple scrollable list of the current ReGraded point list. */
public final class PointsScreen extends Screen {

    private final Screen parent;
    private int scroll;

    public PointsScreen(Screen parent) {
        super(Component.literal("ReGraded Points"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds((this.width - 100) / 2, this.height - 28, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        List<SurveyPoint> pts = Points.list().points();
        int y = 32 - scroll;
        graphics.drawString(this.font, "Points: " + pts.size(), 16, 12, 0xFFFFFF);
        for (SurveyPoint p : pts) {
            if (y > 24 && y < this.height - 36) {
                String line = String.format("%s  E%.1f N%.1f Z%.1f  %s",
                        p.id(), p.easting(), p.northing(), p.elevation(), p.description());
                graphics.drawString(this.font, line, 16, y, 0xA0A0A0);
            }
            y += 12;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) (scrollY * 12));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
