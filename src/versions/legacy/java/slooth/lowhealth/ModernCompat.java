package slooth.lowhealth;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ModernCompat {
    ModernCompat() {
    }

    static Screen screen(Minecraft client) {
        return client.screen;
    }

    static void setScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }

    static boolean hudHidden(Minecraft client) {
        return client.options.hideGui;
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

