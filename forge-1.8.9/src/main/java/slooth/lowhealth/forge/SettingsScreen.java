package slooth.lowhealth.forge;

import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import slooth.lowhealth.ConfigurationView;
import slooth.lowhealth.WarningConfig;
import slooth.lowhealth.forge.LowHealthForge;

class SettingsScreen
extends GuiScreen
implements ConfigurationView {
    private final LowHealthForge mod;
    private final GuiScreen parent;
    private WarningConfig draft;
    private GuiTextField message;
    private String error;
    private int left;
    private int top;

    SettingsScreen(LowHealthForge mod, GuiScreen parent) {
        this.mod = mod;
        this.parent = parent;
        this.draft = mod.draft();
    }

    public void initGui() {
        Keyboard.enableRepeatEvents((boolean)true);
        this.left = this.width / 2 - 150;
        this.top = Math.max(28, (this.height - 202) / 2);
        this.buttonList.clear();
        this.message = new GuiTextField(20, this.fontRendererObj, this.left + 2, this.top + 2, 296, 18);
        this.message.setMaxStringLength(120);
        this.message.setText(this.draft.warningMessage);
        this.add(0, 0, 24, 148);
        this.add(1, 152, 24, 148);
        this.add(2, 0, 46, 148);
        this.add(3, 152, 46, 148);
        this.add(4, 0, 68, 148);
        this.add(5, 152, 68, 148);
        this.add(6, 0, 90, 148);
        this.add(7, 152, 90, 148);
        this.add(8, 0, 112, 148);
        this.add(9, 152, 112, 148);
        this.add(10, 0, 134, 148);
        this.add(11, 152, 134, 148);
        this.add(12, 0, 176, 148);
        this.add(13, 152, 176, 148);
        this.labels();
    }

    private void add(int id, int x, int y, int w) {
        this.buttonList.add(new GuiButton(id, this.left + x, this.top + y, w, 20, ""));
    }

    private static String on(boolean value) {
        return value ? "On" : "Off";
    }

    private void labels() {
        String hearts = String.format(Locale.ROOT, "%.1f", Float.valueOf((float)this.draft.warningThreshold / 2.0f));
        String[] labels = new String[]{"Warning: " + SettingsScreen.on(this.draft.warningEnabled), "Display: " + (new String[]{"Overlay", "Title", "Both"})[this.draft.displayMode], "- Half heart (" + hearts + ")", "+ Half heart (" + hearts + ")", "Sound: " + SettingsScreen.on(this.draft.soundEnabled), "Volume: " + Math.round(this.draft.soundVolume * 100.0f) + "%", "Repeat: " + this.draft.soundIntervalSeconds + "s", "Count absorption: " + SettingsScreen.on(this.draft.includeAbsorption), "Creative: " + SettingsScreen.on(this.draft.warnInCreative), "Preview", "Reset defaults", "Sound test", "Done", "Cancel"};
        for (GuiButton button : this.buttonList) {
            button.displayString = labels[button.id];
        }
    }

    protected void actionPerformed(GuiButton button) {
        this.draft.warningMessage = this.message.getText();
        switch (button.id) {
            case 0: {
                this.draft.warningEnabled = !this.draft.warningEnabled;
                break;
            }
            case 1: {
                this.draft.displayMode = (this.draft.displayMode + 1) % 3;
                break;
            }
            case 2: {
                this.draft.warningThreshold = Math.max(1, this.draft.warningThreshold - 1);
                break;
            }
            case 3: {
                this.draft.warningThreshold = Math.min(40, this.draft.warningThreshold + 1);
                break;
            }
            case 4: {
                this.draft.soundEnabled = !this.draft.soundEnabled;
                break;
            }
            case 5: {
                this.draft.soundVolume = (float)((Math.round(this.draft.soundVolume * 10.0f) + 1) % 11) / 10.0f;
                break;
            }
            case 6: {
                this.draft.soundIntervalSeconds = this.draft.soundIntervalSeconds % 10 + 1;
                break;
            }
            case 7: {
                this.draft.includeAbsorption = !this.draft.includeAbsorption;
                break;
            }
            case 8: {
                this.draft.warnInCreative = !this.draft.warnInCreative;
                break;
            }
            case 9: {
                this.mc.displayGuiScreen((GuiScreen)new Preview(this, this.draft.copy().normalized()));
                return;
            }
            case 10: {
                this.draft = new WarningConfig();
                this.message.setText(this.draft.warningMessage);
                break;
            }
            case 11: {
                LowHealthForge.playSound(this.draft.soundVolume);
                break;
            }
            case 12: {
                if (this.mod.save(this.draft)) {
                    this.mc.displayGuiScreen(this.parent);
                } else {
                    this.error = "Save failed. Check the game log; edits are still here.";
                }
                return;
            }
            case 13: {
                this.mc.displayGuiScreen(this.parent);
                return;
            }
        }
        this.labels();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "Low Health Warning", this.width / 2, this.top - 23, 0xFFFFFF);
        this.message.drawTextBox();
        this.drawCenteredString(this.fontRendererObj, this.error == null ? "Done saves. Cancel or Esc discards changes." : this.error, this.width / 2, this.top + 159, this.error == null ? 0xAAAAAA : 0xFF6666);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.mc.displayGuiScreen(this.parent);
            return;
        }
        if (this.message.textboxKeyTyped(typedChar, keyCode)) {
            this.draft.warningMessage = this.message.getText();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        this.message.mouseClicked(x, y, button);
    }

    public void updateScreen() {
        this.message.updateCursorCounter();
    }

    public void onGuiClosed() {
        Keyboard.enableRepeatEvents((boolean)false);
    }

    private static final class Preview
    extends GuiScreen
    implements ConfigurationView {
        private final SettingsScreen parent;
        private final WarningConfig config;
        private boolean played;

        Preview(SettingsScreen parent, WarningConfig config) {
            this.parent = parent;
            this.config = config;
        }

        public void initGui() {
            this.buttonList.clear();
            this.buttonList.add(new GuiButton(0, this.width / 2 - 75, this.height - 30, 150, 20, "Back"));
            if (!this.played && this.config.soundEnabled) {
                LowHealthForge.playSound(this.config.soundVolume);
            }
            this.played = true;
        }

        public void drawScreen(int x, int y, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRendererObj, "Warning preview", this.width / 2, 12, 0xFFFFFF);
            LowHealthForge.drawWarning(this.config, this.width, this.height);
            super.drawScreen(x, y, partialTicks);
        }

        protected void actionPerformed(GuiButton button) {
            this.mc.displayGuiScreen((GuiScreen)this.parent);
        }

        protected void keyTyped(char c, int key) throws IOException {
            if (key == 1) {
                this.mc.displayGuiScreen((GuiScreen)this.parent);
            } else {
                super.keyTyped(c, key);
            }
        }
    }
}

