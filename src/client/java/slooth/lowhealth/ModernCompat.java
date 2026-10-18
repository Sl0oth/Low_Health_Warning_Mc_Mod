package slooth.lowhealth;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Cached adapter for the two unremapped 26.x screen APIs, allowing one tested jar. */
final class ModernCompat {
    private static final Field SCREEN;
    private static final Method SET_SCREEN;
    private static final Method GUI_SCREEN;
    private static final Field HUD;
    private static final Method HUD_HIDDEN;
    private static final Field HIDE_GUI;
    static {
        try {
            Method guiScreen;
            try { guiScreen = net.minecraft.client.gui.Gui.class.getMethod("screen"); }
            catch (NoSuchMethodException oldApi) { guiScreen = null; }
            GUI_SCREEN = guiScreen;
            if (guiScreen == null) {
                SCREEN = Minecraft.class.getField("screen");
                SET_SCREEN = Minecraft.class.getMethod("setScreen", Screen.class);
                HIDE_GUI = net.minecraft.client.Options.class.getField("hideGui");
                HUD = null; HUD_HIDDEN = null;
            } else {
                SCREEN = null; HIDE_GUI = null;
                SET_SCREEN = net.minecraft.client.gui.Gui.class.getMethod("setScreen", Screen.class);
                HUD = net.minecraft.client.gui.Gui.class.getField("hud");
                HUD_HIDDEN = HUD.getType().getMethod("isHidden");
            }
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    static Screen screen(Minecraft client) {
        try { return (Screen)(GUI_SCREEN == null ? SCREEN.get(client) : GUI_SCREEN.invoke(client.gui)); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot read Minecraft screen",e); }
    }
    static void setScreen(Minecraft client, Screen screen) {
        try { SET_SCREEN.invoke(GUI_SCREEN == null ? client : client.gui,screen); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot set Minecraft screen",e); }
    }
    static boolean hudHidden(Minecraft client) {
        try { return GUI_SCREEN == null ? HIDE_GUI.getBoolean(client.options) : (boolean)HUD_HIDDEN.invoke(HUD.get(client.gui)); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot read HUD visibility",e); }
    }
    static net.minecraft.client.KeyMapping registerSettingsKey() {
        return net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
            new net.minecraft.client.KeyMapping("key.lowhelthwarning.settings", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, 72,
                net.minecraft.client.KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("lowhelthwarning", "settings"))));
    }
    static void registerHud(java.util.function.Consumer<net.minecraft.client.gui.GuiGraphicsExtractor> render) {
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementBefore(
            net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
            net.minecraft.resources.Identifier.fromNamespaceAndPath("lowhelthwarning", "warning"),
            (context, delta) -> render.accept(context));
    }
}

