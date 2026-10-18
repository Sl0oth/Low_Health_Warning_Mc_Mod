package slooth.lowhealth;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Draft settings match the published mod: Done saves, Cancel/Escape discard. */
public class LowHealthConfigScreen extends Screen implements ConfigurationView {
    private final Screen parent;
    private WarningConfig draft = LowHealthWarningClient.getConfig();
    private boolean saveFailed;
    private int top;

    public LowHealthConfigScreen(Screen parent) {
        super(Component.translatable("screen.lowhelthwarning.title"));
        this.parent = parent;
    }
    private static Component label(String key, Object... args) {
        return Component.translatable("option.lowhelthwarning." + key, args);
    }
    private static Component onOff(boolean on) { return Component.translatable(on ? "options.on" : "options.off"); }

    @Override
    protected void init() {
        top = Math.max(28, (height - 202) / 2);
        int left = width / 2 - 150;
        var message = new EditBox(font, left, top + 12, 300, 20, label("message"));
        message.setMaxLength(120);
        message.setValue(draft.warningMessage);
        message.setResponder(text -> draft.warningMessage = text);
        addRenderableWidget(message);
        int y = top + 38;
        button(left, y, () -> label("enabled", onOff(draft.warningEnabled)), () -> draft.warningEnabled = !draft.warningEnabled);
        button(left + 154, y, () -> label("mode", label("mode." + draft.displayMode)), () -> draft.displayMode = (draft.displayMode + 1) % 3);
        y += 24;
        slider(left, y, (draft.warningThreshold - 1) / 39.0, value -> draft.warningThreshold = 1 + (int)Math.round(value * 39),
                () -> label("threshold", draft.warningThreshold / 2.0));
        button(left + 154, y, () -> label("absorption", onOff(draft.includeAbsorption)), () -> draft.includeAbsorption = !draft.includeAbsorption);
        y += 24;
        button(left, y, () -> label("sound", onOff(draft.soundEnabled)), () -> draft.soundEnabled = !draft.soundEnabled);
        slider(left + 154, y, draft.soundVolume, value -> draft.soundVolume = value.floatValue(), () -> label("volume", Math.round(draft.soundVolume * 100)));
        y += 24;
        button(left, y, () -> label("interval", draft.soundIntervalSeconds), () -> draft.soundIntervalSeconds = draft.soundIntervalSeconds % 10 + 1);
        button(left + 154, y, () -> label("creative", onOff(draft.warnInCreative)), () -> draft.warnInCreative = !draft.warnInCreative);
        y += 24;
        button(left, y, () -> label("preview"), () -> ModernCompat.setScreen(minecraft, new LowHealthPreviewScreen(this, draft)));
        button(left + 154, y, () -> label("reset"), () -> { draft = new WarningConfig(); clearWidgets(); init(); });
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(left, top + 176, 146, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            if (LowHealthWarningClient.saveConfig(draft)) onClose(); else saveFailed = true;
        }).bounds(left + 154, top + 176, 146, 20).build());
    }
    private void button(int x, int y, Supplier<Component> text, Runnable action) {
        addRenderableWidget(Button.builder(text.get(), b -> { action.run(); b.setMessage(text.get()); }).bounds(x,y,146,20).build());
    }
    private void slider(int x, int y, double initial, Consumer<Double> change, Supplier<Component> text) {
        addRenderableWidget(new AbstractSliderButton(x,y,146,20,text.get(),initial) {
            @Override protected void updateMessage() { setMessage(text.get()); }
            @Override protected void applyValue() { change.accept(value); updateMessage(); }
        });
    }
    @Override public void onClose() { ModernCompat.setScreen(minecraft, parent); }
    @Override public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font,title,width/2,top-18,0xFFFFFFFF);
        context.text(font,label("message"),width/2-150,top,0xFFFFFFFF);
        context.centeredText(font,label(saveFailed ? "save_failed" : "hint"),width/2,top+158,saveFailed ? 0xFFFF5555 : 0xFFAAAAAA);
    }
}
