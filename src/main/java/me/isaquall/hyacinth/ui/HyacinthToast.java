package me.isaquall.hyacinth.ui;

import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HyacinthToast implements Toast {

    private final List<OrderedText> message;
    private final TextRenderer textRenderer;
    private final int width;

    public HyacinthToast(List<Text> message) {
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        this.width = Math.min(240, TextOps.width(textRenderer, message) + 8);
        this.message = this.wrap(message);
    }

    private Visibility visibility = Visibility.HIDE;

    @Override
    public void update(ToastManager manager, long time) {
        this.visibility = time > 10000 ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public Visibility getVisibility() {
        return this.visibility;
    }

    @Override
    public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
        var owoContext = OwoUIDrawContext.of(context);

        owoContext.fill(0, 0, this.getWidth(), this.getHeight(), 0x77000000);
        owoContext.drawRectOutline(0, 0, this.getWidth(), this.getHeight(), 0xA7FF0000);

        int xOffset = this.getWidth() / 2 - this.textRenderer.getWidth(this.message.get(0)) / 2;
        owoContext.drawTextWithShadow(this.textRenderer, this.message.get(0), 4 + xOffset, 4, 0xFFFFFF);

        for (int i = 1; i < this.message.size(); i++) {
            owoContext.drawText(this.textRenderer, this.message.get(i), 4, 4 + i * 11, 0xFFFFFF, false);
        }
    }

    @Override
    public int getHeight() {
        return 6 + this.message.size() * 11;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    private List<OrderedText> wrap(List<Text> message) {
        var list = new ArrayList<OrderedText>();
        for (var text : message) list.addAll(this.textRenderer.wrapLines(text, this.getWidth() - 8));
        return list;
    }
}
