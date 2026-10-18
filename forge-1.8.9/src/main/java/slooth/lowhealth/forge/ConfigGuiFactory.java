package slooth.lowhealth.forge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import java.util.Set;
public final class ConfigGuiFactory implements IModGuiFactory {
    public void initialize(Minecraft client) {}
    public Class<? extends GuiScreen> mainConfigGuiClass(){return MenuScreen.class;}
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories(){return null;}
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element){return null;}
    public static final class MenuScreen extends SettingsScreen {
        public MenuScreen(GuiScreen parent){super(LowHealthForge.INSTANCE,parent);}
    }
}
