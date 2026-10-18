package slooth.lowhealth.audit;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import slooth.lowhealth.ConfigAuditGameTest;
import java.nio.file.Files;

/** Test-only runner for releases predating Fabric's client game-test framework. */
public final class LegacyAuditRunner implements ClientModInitializer {
    public static volatile long ticks;
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> ticks++);
        Thread worker = new Thread(() -> {
            try {
                ClientGameTestContext ctx = new ClientGameTestContext();
                ctx.waitForScreen(net.minecraft.client.gui.screens.TitleScreen.class);
                ctx.runOnClient(c -> c.options.pauseOnLostFocus = false);
                new ConfigAuditGameTest().runTest(ctx);
                Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("audit-result.txt"),"PASS\n");
                System.out.println("LOWHEALTH_LEGACY_AUDIT_PASS");
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
            } catch (Throwable e) {
                e.printStackTrace();
                System.err.println("LOWHEALTH_LEGACY_AUDIT_FAILED");
                System.exit(1);
            }
        }, "lowhealth-legacy-audit");
        worker.setDaemon(true);
        worker.start();
    }
}
