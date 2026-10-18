package slooth.lowhealth.forge;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;
import slooth.lowhealth.ConfigStore;
import slooth.lowhealth.ConfigurationView;
import slooth.lowhealth.WarningConfig;
import slooth.lowhealth.WarningState;
import slooth.lowhealth.forge.SettingsScreen;

@Mod(modid="lowhelthwarning", name="Low Health Warning", version="1.2.1-rc.1", guiFactory="slooth.lowhealth.forge.ConfigGuiFactory", acceptedMinecraftVersions="[1.8.9]", acceptableRemoteVersions="*", clientSideOnly=true)
public final class LowHealthForge {
    @Mod.Instance("lowhelthwarning")
    public static LowHealthForge INSTANCE;
    private final WarningState state = new WarningState();
    private final KeyBinding settings = new KeyBinding("key.lowhelthwarning.settings", 35, "key.categories.lowhelthwarning");
    private WarningConfig config = new WarningConfig();
    private Path configPath;
    private Logger logger;

    @Mod.EventHandler
    public void initialize(FMLPreInitializationEvent event) {
        this.logger = event.getModLog();
        this.configPath = event.getModConfigurationDirectory().toPath().resolve("lowhealth_config.json");
        try {
            this.config = ConfigStore.load(this.configPath);
        }
        catch (IOException e) {
            this.logger.error("Could not load Low Health Warning config; using defaults", (Throwable)e);
        }
        ClientRegistry.registerKeyBinding((KeyBinding)this.settings);
        FMLCommonHandler.instance().bus().register((Object)this);
        MinecraftForge.EVENT_BUS.register((Object)this);
    }

    WarningConfig draft() {
        return this.config.copy();
    }

    boolean save(WarningConfig next) {
        try {
            ConfigStore.save(this.configPath, next);
            this.config = next.copy().normalized();
            this.state.reset();
            return true;
        }
        catch (IOException e) {
            this.logger.error("Could not save Low Health Warning config", (Throwable)e);
            return false;
        }
    }

    private boolean shouldWarn(Minecraft mc) {
        return mc.theWorld != null && mc.thePlayer != null && WarningState.shouldWarn(this.config, mc.thePlayer.getHealth(), mc.thePlayer.getAbsorptionAmount(), mc.thePlayer.isEntityAlive(), mc.thePlayer.isSpectator(), mc.thePlayer.capabilities.isCreativeMode);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        while (this.settings.isPressed()) {
            if (mc.currentScreen != null) continue;
            mc.displayGuiScreen((GuiScreen)new SettingsScreen(this, null));
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            this.state.reset();
            return;
        }
        if (mc.isGamePaused()) {
            return;
        }
        if (this.state.tick(this.shouldWarn(mc), this.config.soundEnabled && !(mc.currentScreen instanceof ConfigurationView), this.config.soundIntervalSeconds)) {
            LowHealthForge.playSound(this.config.soundVolume);
        }
    }

    static void playSound(final float soundVolume) {
        Minecraft.getMinecraft().getSoundHandler().playSound((ISound)new PositionedSound(new ResourceLocation("note.pling")){
            {
                this.volume = soundVolume;
                this.pitch = 0.8f;
                this.attenuationType = ISound.AttenuationType.NONE;
            }
        });
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || mc.gameSettings.hideGUI || mc.currentScreen != null || !this.shouldWarn(mc)) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        LowHealthForge.drawWarning(this.config, resolution.getScaledWidth(), resolution.getScaledHeight());
    }

    static void drawWarning(WarningConfig config, int width, int height) {
        if (config.displayMode == 0 || config.displayMode == 2) {
            LowHealthForge.drawText(config.warningMessage, width, height - 80, 1.0f);
        }
        if (config.displayMode == 1 || config.displayMode == 2) {
            LowHealthForge.drawText(config.warningMessage, width, height / 2 - 40, 2.0f);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void drawText(String text, int width, int y, float desiredScale) {
        Minecraft mc = Minecraft.getMinecraft();
        text = text.replaceAll("(?i)&([0-9a-fk-or])", "§$1");
        int textWidth = mc.fontRendererObj.getStringWidth(text);
        float scale = Math.min(desiredScale, ((float)width - 16.0f) / (float)Math.max(1, textWidth));
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate((float)((float)width / 2.0f), (float)y, (float)0.0f);
            GlStateManager.scale((float)scale, (float)scale, (float)1.0f);
            mc.fontRendererObj.drawStringWithShadow(text, (float)(-textWidth) / 2.0f, 0.0f, 0xFFFFFF);
        }
        finally {
            GlStateManager.popMatrix();
            GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        }
    }
}

