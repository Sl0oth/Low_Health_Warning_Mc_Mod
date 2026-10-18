package slooth.lowhealth;

import java.io.IOException;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import slooth.lowhealth.ConfigStore;
import slooth.lowhealth.ConfigurationView;
import slooth.lowhealth.LowHealthConfigScreen;
import slooth.lowhealth.ModernCompat;
import slooth.lowhealth.WarningConfig;
import slooth.lowhealth.WarningState;

public final class LowHealthWarningClient
implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger((String)"lowhelthwarning");
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("lowhealth_config.json");
    private static WarningConfig config = new WarningConfig();
    private static final WarningState STATE = new WarningState();

    public static WarningConfig getConfig() {
        return config.copy();
    }

    public static boolean saveConfig(WarningConfig draft) {
        WarningConfig next = draft.copy().normalized();
        try {
            ConfigStore.save(CONFIG_PATH, next);
            config = next;
            STATE.reset();
            return true;
        }
        catch (IOException e) {
            LOGGER.error("Could not save low health settings", (Throwable)e);
            return false;
        }
    }

    public void onInitializeClient() {
        try {
            config = ConfigStore.load(CONFIG_PATH);
        }
        catch (IOException e) {
            LOGGER.error("Could not load low health settings; using defaults", (Throwable)e);
        }
        KeyMapping settings = ModernCompat.registerSettingsKey();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> STATE.reset());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> STATE.reset());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settings.consumeClick()) {
                if (ModernCompat.screen(client) instanceof LowHealthConfigScreen) continue;
                ModernCompat.setScreen(client, new LowHealthConfigScreen(ModernCompat.screen(client)));
            }
            if (client.player == null || client.level == null) {
                STATE.reset();
                return;
            }
            if (client.isPaused()) {
                return;
            }
            LocalPlayer player = client.player;
            boolean active = WarningState.shouldWarn(config, player.getHealth(), player.getAbsorptionAmount(), player.isAlive(), player.isSpectator(), player.getAbilities().instabuild);
            if (STATE.tick(active, LowHealthWarningClient.config.soundEnabled, LowHealthWarningClient.config.soundIntervalSeconds) && !(ModernCompat.screen(client) instanceof ConfigurationView)) {
                LowHealthWarningClient.playSound(LowHealthWarningClient.config.soundVolume);
            }
        });
        ModernCompat.registerHud(context -> {
            Minecraft client = Minecraft.getInstance();
            if (STATE.isActive() && client.player != null && client.player.isAlive() && ModernCompat.screen(client) == null && !ModernCompat.hudHidden(client)) {
                LowHealthWarningClient.renderWarning(context, config);
            }
        });
    }

    public static void playSound(float volume) {
        Minecraft.getInstance().getSoundManager().play((SoundInstance)SimpleSoundInstance.forUI((SoundEvent)((SoundEvent)SoundEvents.NOTE_BLOCK_PLING.value()), (float)0.8f, (float)volume));
    }

    public static void renderWarning(GuiGraphicsExtractor context, WarningConfig settings) {
        int width = context.guiWidth();
        int height = context.guiHeight();
        Component message = LowHealthWarningClient.parseMessage(settings.warningMessage);
        if (settings.displayMode != 1) {
            LowHealthWarningClient.drawFitted(context, message, width, height - 80, 1.0f);
        }
        if (settings.displayMode != 0) {
            LowHealthWarningClient.drawFitted(context, message, width, height / 2 - 40, 2.0f);
        }
    }

    private static void drawFitted(GuiGraphicsExtractor context, Component message, int width, int y, float desiredScale) {
        Font font = Minecraft.getInstance().font;
        float scale = Math.min(desiredScale, ((float)width - 20.0f) / (float)Math.max(1, font.width((FormattedText)message)));
        context.pose().pushMatrix();
        context.pose().translate((float)width / 2.0f, (float)y);
        context.pose().scale(scale, scale);
        context.centeredText(font, message, 0, 0, -43691);
        context.pose().popMatrix();
    }

    public static Component parseMessage(String message) {
        MutableComponent result = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder segment = new StringBuilder();
        for (int i = 0; i < message.length(); ++i) {
            ChatFormatting format;
            char ch = message.charAt(i);
            ChatFormatting chatFormatting = format = (ch == '\u00a7' || ch == '&') && i + 1 < message.length() ? ChatFormatting.getByCode((char)message.charAt(i + 1)) : null;
            if (format != null) {
                result.append((Component)Component.literal((String)segment.toString()).withStyle(style));
                segment.setLength(0);
                style = format == ChatFormatting.RESET ? Style.EMPTY : style.applyLegacyFormat(format);
                ++i;
                continue;
            }
            segment.append(ch);
        }
        return result.append((Component)Component.literal((String)segment.toString()).withStyle(style));
    }
}

