package slooth.lowhealth;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Screen-local preview of the unsaved draft, sharing the live renderer. */
public class LowHealthPreviewScreen extends Screen implements ConfigurationView {
    private final Screen parent;
    private final WarningConfig preview;
    private boolean soundPlayed;

    public LowHealthPreviewScreen(Screen parent, WarningConfig draft) {
        super(Component.translatable("lowhealth.preview.title"));
        this.parent = parent;
        preview = draft.copy().normalized();
    }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("lowhealth.preview.back"), b -> onClose())
                .bounds(width/2-75,height-30,150,20).build());
        if (preview.soundEnabled && !soundPlayed) LowHealthWarningClient.playSound(preview.soundVolume);
        soundPlayed = true;
    }
    @Override public void onClose() { ModernCompat.setScreen(minecraft, parent); }
    @Override public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context,mouseX,mouseY,delta);
        context.centeredText(font,title,width/2,16,0xFFFFFFFF);
        int y = 30;
        for(var line : font.split(Component.translatable("lowhealth.preview.detail"),width-24)) {
            context.centeredText(font,line,width/2,y,0xFFAAAAAA);
            y += font.lineHeight + 1;
        }
        LowHealthWarningClient.renderWarning(context,preview);
    }
}
