package slooth.lowhealth;

import net.minecraft.client.gui.screens.Screen;

public class LowHealthConfigUI {

    private static final LowHealthConfigUI instance = new LowHealthConfigUI();

    public static LowHealthConfigUI getInstance() {
        return instance;
    }

    public Screen getScreen(Screen parent) {
        return new LowHealthConfigScreen(parent);
    }
}
