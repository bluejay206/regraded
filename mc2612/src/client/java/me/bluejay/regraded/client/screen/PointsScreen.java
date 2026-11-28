package me.bluejay.regraded.client.screen;

import me.bluejay.regraded.JobStore;
import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.TreeSet;

/**
 * Point list with LevelHeaded selection behavior.
 * Shift extends a range, Ctrl toggles, Ctrl+A selects all.
 * Mouse wheel scrolls the view and does not move the selection.
 * Arrow keys, Home/End, Page Up/Down navigate. Delete removes the selection.
 */
public final class PointsScreen extends Screen {

    private static final int ROW = 12;
    private static final int TOP = 36;
    private static final int BOTTOM_PAD = 36;
    private static final int LIST_W = 280;

    private static final int KEY_UP = 265;
    private static final int KEY_DOWN = 264;
    private static final int KEY_HOME = 268;
    private static final int KEY_END = 269;
    private static final int KEY_PAGE_UP = 266;
    private static final int KEY_PAGE_DOWN = 267;
    private static final int KEY_A = 65;
    private static final int KEY_DELETE = 261;

    private final Screen parent;
    private int scroll;
    private int selected;
    private int anchor;
    private final TreeSet<Integer> multi = new TreeSet<>();
    private boolean dragging;

    public PointsScreen(Screen parent) {
        super(Component.literal("ReGraded Points"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 140;
        int y = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.literal("Delete"), b -> deleteSelected())
                .bounds(x, y, 70, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds(x + 76, y, 70, 20)
                .build());
    }

    private List<SurveyPoint> visible() {
        return Points.list().points();
    }

    private int rowsThatFit() {
        int h = this.height - TOP - BOTTOM_PAD;
        return Math.max(1, h / ROW);
    }

    private int maxScroll() {
        return Math.max(0, visible().size() - rowsThatFit());
    }

    private int listX() {
        return this.width / 2 - 140;
    }

    private int trackX() {
        return this.width / 2 + 150;
    }

    private void select(int index) {
        int size = visible().size();
        if (size == 0) {
            selected = 0;
            scroll = 0;
            return;
        }
        selected = Math.max(0, Math.min(size - 1, index));
        if (selected < scroll) scroll = selected;
        int rows = rowsThatFit();
        if (selected >= scroll + rows) scroll = selected - rows + 1;
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    private void selectOnly(int index) {
        select(index);
        anchor = selected;
        multi.clear();
        multi.add(selected);
    }

    private void toggle(int index) {
        int size = visible().size();
        if (size == 0) return;
        index = Math.max(0, Math.min(size - 1, index));
        if (multi.contains(index)) {
            multi.remove(index);
            if (selected == index && !multi.isEmpty()) {
                selected = multi.first();
            }
        } else {
            multi.add(index);
            selected = index;
            anchor = index;
        }
        if (index < scroll) scroll = index;
        int rows = rowsThatFit();
        if (index >= scroll + rows) scroll = index - rows + 1;
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    private void selectRange(int index) {
        select(index);
        multi.clear();
        int lo = Math.min(anchor, selected);
        int hi = Math.max(anchor, selected);
        for (int i = lo; i <= hi; i++) multi.add(i);
    }

    private void selectAll() {
        multi.clear();
        int size = visible().size();
        for (int i = 0; i < size; i++) multi.add(i);
        if (!multi.isEmpty()) {
            anchor = multi.first();
            selected = multi.last();
            select(selected);
        }
    }

    private boolean isSelected(int index) {
        return multi.contains(index);
    }

    private void deleteSelected() {
        List<SurveyPoint> list = visible();
        if (list.isEmpty() || multi.isEmpty()) return;
        for (int i : multi.descendingSet()) {
            Points.list().removeAt(i);
        }
        JobStore.save();
        multi.clear();
        selectOnly(Math.min(selected, Math.max(0, visible().size() - 1)));
    }

    private boolean onScrollbar(double mouseX, double mouseY) {
        if (visible().size() <= rowsThatFit()) return false;
        int trackY = TOP;
        int trackH = rowsThatFit() * ROW;
        int tx = trackX();
        return mouseX >= tx - 2 && mouseX <= tx + 6
                && mouseY >= trackY && mouseY <= trackY + trackH;
    }

    private void scrollFromTrack(double mouseY) {
        int total = visible().size();
        int rows = rowsThatFit();
        if (total <= rows) return;
        int trackY = TOP;
        int trackH = rows * ROW;
        double t = (mouseY - trackY) / Math.max(1, trackH);
        t = Math.max(0, Math.min(1, t));
        scroll = (int) Math.round(t * maxScroll());
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        int mods = event.modifiers();
        boolean shift = (mods & 1) != 0;
        boolean ctrl = (mods & 2) != 0 || (mods & 8) != 0;
        double mouseX = event.x();
        double mouseY = event.y();
        if (onScrollbar(mouseX, mouseY)) {
            dragging = true;
            scrollFromTrack(mouseY);
            return true;
        }
        int x = listX();
        if (mouseX >= x && mouseX < x + LIST_W && mouseY >= TOP) {
            int row = (int) ((mouseY - TOP) / ROW);
            if (row >= 0 && row < rowsThatFit() && scroll + row < visible().size()) {
                int index = scroll + row;
                if (shift) selectRange(index);
                else if (ctrl) toggle(index);
                else selectOnly(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            scrollFromTrack(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        boolean shift = (event.modifiers() & 1) != 0;
        boolean ctrl = (event.modifiers() & 2) != 0;
        if (ctrl && key == KEY_A) {
            selectAll();
            return true;
        }
        if (key == KEY_DELETE) {
            deleteSelected();
            return true;
        }
        Integer next = null;
        if (key == KEY_UP) next = selected - 1;
        else if (key == KEY_DOWN) next = selected + 1;
        else if (key == KEY_HOME) next = 0;
        else if (key == KEY_END) next = visible().size() - 1;
        else if (key == KEY_PAGE_UP) next = selected - rowsThatFit();
        else if (key == KEY_PAGE_DOWN) next = selected + rowsThatFit();
        if (next != null) {
            if (shift) selectRange(next);
            else selectOnly(next);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical > 0) {
            scroll = Math.max(0, scroll - 1);
        } else if (vertical < 0) {
            scroll = Math.min(maxScroll(), scroll + 1);
        }
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        List<SurveyPoint> list = visible();
        int x = listX();
        int rows = rowsThatFit();
        graphics.text(this.font, list.size() + " points  (" + multi.size() + " selected)",
                x, 20, 0xFFA0A0A0, false);
        if (list.isEmpty()) {
            graphics.text(this.font, "No points. Use Add Node or the rod tool.",
                    x, TOP, 0xFF808080, false);
            return;
        }
        for (int shown = 0; shown < rows && scroll + shown < list.size(); shown++) {
            int i = scroll + shown;
            SurveyPoint p = list.get(i);
            String line = String.format("%s  E%.1f N%.1f Z%.1f  %s",
                    p.id(), p.easting(), p.northing(), p.elevation(), p.description());
            int rowY = TOP + shown * ROW;
            if (isSelected(i)) {
                graphics.fill(x - 2, rowY - 1, x + LIST_W, rowY + ROW - 1, 0xC0406090);
            }
            int color = isSelected(i) ? 0xFFFFFFFF : 0xFFC0C0C0;
            graphics.text(this.font, line, x, rowY, color, false);
        }
        if (list.size() > rows) {
            int tx = trackX();
            int trackH = rows * ROW;
            graphics.fill(tx, TOP, tx + 4, TOP + trackH, 0x40000000);
            int thumbH = Math.max(8, trackH * rows / list.size());
            int thumbY = TOP + (trackH - thumbH) * scroll / Math.max(1, maxScroll());
            graphics.fill(tx, thumbY, tx + 4, thumbY + thumbH, 0xFFA0A0A0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
