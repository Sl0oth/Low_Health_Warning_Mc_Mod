package slooth.lowhealth;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

public class ConfigAuditGameTest implements FabricClientGameTest {
    private final AtomicInteger sounds = new AtomicInteger();
    private volatile float lastVolume;

    @Override public void runTest(ClientGameTestContext ctx) {
        checkCoreAndStorage();
        ctx.waitForScreen(TitleScreen.class);
        ctx.runOnClient(c -> check(LowHealthWarningClient.saveConfig(new WarningConfig()), "Initialize config"));
        SoundEventListener listener = (sound, event, range) -> {
            if (sound.getIdentifier().toString().equals("minecraft:block.note_block.pling")) {
                sounds.incrementAndGet(); lastVolume = sound.getVolume();
            }
        };
        ctx.runOnClient(c -> c.getSoundManager().addListener(listener));
        try {
            for (int[] size : new int[][]{{640,480,2},{1280,720,3},{1920,1080,4}}) {
                ctx.getInput().resizeWindow(size[0],size[1]);
                ctx.runOnClient(c -> {c.options.guiScale().set(size[2]); c.resizeGui();});
                ctx.setScreen(() -> new LowHealthConfigScreen(new TitleScreen()));
                ctx.runOnClient(c -> {
                    var widgets = ModernCompat.screen(c).children().stream().filter(x -> x instanceof AbstractWidget).map(x -> (AbstractWidget)x).toList();
                    for (var w : widgets) {
                        check(w.getX() >= 0 && w.getY() >= 0 && w.getRight() <= ModernCompat.screen(c).width && w.getBottom() <= ModernCompat.screen(c).height,"Clipped control");
                        for (var o : widgets) if (w != o) check(w.getRight() <= o.getX() || o.getRight() <= w.getX() || w.getBottom() <= o.getY() || o.getBottom() <= w.getY(),"Overlapping controls");
                    }
                    edit(ModernCompat.screen(c)).setValue("&c" + "Long warning ".repeat(9));
                });
                ctx.takeScreenshot("settings-"+size[0]+"-scale-"+size[2]);
                click(ctx,"mode"); click(ctx,"mode");
                click(ctx,"preview");
                ctx.takeScreenshot("long-preview-"+size[0]);
                ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
                ctx.waitForScreen(LowHealthConfigScreen.class);
                ctx.clickScreenButton("gui.cancel");
            }
            ctx.runOnClient(c -> check(LowHealthWarningClient.getConfig().warningMessage.equals("§cLow Health!"),"Cancel changed saved settings"));
            ctx.setScreen(() -> new LowHealthConfigScreen(new TitleScreen()));
            click(ctx,"enabled"); click(ctx,"absorption"); click(ctx,"sound"); click(ctx,"creative"); click(ctx,"interval");
            ctx.runOnClient(c -> {var e=edit(ModernCompat.screen(c)); e.setValue(""); ModernCompat.screen(c).setFocused(e); e.setFocused(true);});
            ctx.getInput().typeChars("&aAudit &ltest&r!");
            slider(ctx,0,true); slider(ctx,1,false);
            ctx.clickScreenButton("gui.done");
            ctx.runOnClient(c -> {
                var v=LowHealthWarningClient.getConfig();
                check(!v.warningEnabled && v.includeAbsorption && v.soundEnabled && v.warnInCreative && v.soundIntervalSeconds==4 && v.warningThreshold==40 && v.soundVolume==0,"Settings did not apply");
                check(v.warningMessage.equals("&aAudit &ltest&r!"),"Text input failed");
                try {
                    var disk=ConfigStore.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("lowhealth_config.json"));
                    check(disk.warningMessage.equals(v.warningMessage) && disk.warningThreshold==40 && disk.soundEnabled && disk.warnInCreative && disk.includeAbsorption && disk.soundIntervalSeconds==4 && disk.soundVolume==0,"Persistence failed");
                } catch(Exception e) {throw new AssertionError(e);}
            });
            ctx.setScreen(() -> new LowHealthConfigScreen(new TitleScreen()));
            slider(ctx,0,false); slider(ctx,1,true);
            ctx.clickScreenButton("gui.done");
            ctx.runOnClient(c -> check(LowHealthWarningClient.getConfig().warningThreshold==1 && LowHealthWarningClient.getConfig().soundVolume==1,"Slider endpoints failed"));
            ctx.setScreen(() -> new LowHealthConfigScreen(new TitleScreen()));
            click(ctx,"reset"); ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
            ctx.runOnClient(c -> check(LowHealthWarningClient.getConfig().warningThreshold==1,"Escape saved reset draft"));
            ctx.setScreen(() -> new LowHealthConfigScreen(new TitleScreen()));
            click(ctx,"reset"); ctx.clickScreenButton("gui.done");
            ctx.runOnClient(c -> check(LowHealthWarningClient.getConfig().warningThreshold==8 && !LowHealthWarningClient.getConfig().soundEnabled,"Reset failed"));
            for(int mode=0;mode<3;mode++) {
                final int m=mode;
                ctx.runOnClient(c -> {var v=new WarningConfig();v.warningEnabled=false;v.soundEnabled=true;v.warningMessage="&cTest &lwarning";v.displayMode=m;LowHealthWarningClient.saveConfig(v);});
                ctx.setScreen(() -> new LowHealthConfigScreen(new TitleScreen()));
                int before=sounds.get();
                click(ctx,"preview");ctx.waitTicks(5);
                check(sounds.get()==before+1,"Preview sound missing/repeated");
                ctx.takeScreenshot("preview-mode-"+mode);
                ctx.getInput().resizeWindow(1000+mode*20,700);
                ctx.waitTicks(3);check(sounds.get()==before+1,"Resize replayed sound");
                ctx.clickScreenButton("lowhealth.preview.back");ctx.clickScreenButton("gui.cancel");
            }
            try(var world=ctx.worldBuilder().create()) {
                world.getClientLevel().waitForChunksRender();
                ctx.runOnClient(c -> {var v=new WarningConfig();v.warningThreshold=20;v.displayMode=2;v.soundEnabled=true;v.soundIntervalSeconds=1;v.soundVolume=.25f;LowHealthWarningClient.saveConfig(v);});
                int before=sounds.get();ctx.waitTicks(42);
                ctx.runOnClient(c -> check(active(),"Warning missing at threshold"));
                check(sounds.get()>=before+2 && Math.abs(lastVolume-.25f)<.001f,"Sound repeat/volume failed");
                ctx.takeScreenshot("live-warning-both");
                ctx.runOnClient(c -> {var v=LowHealthWarningClient.getConfig();v.soundEnabled=false;LowHealthWarningClient.saveConfig(v);});
                before=sounds.get();ctx.waitTicks(25);check(sounds.get()==before,"Disabled sound continued");
                world.getServer().runCommand("effect give @a minecraft:absorption 60 0 true");ctx.waitTicks(5);
                ctx.runOnClient(c -> check(active(),"Absorption incorrectly counted by default"));
                ctx.runOnClient(c -> {var v=LowHealthWarningClient.getConfig();v.includeAbsorption=true;LowHealthWarningClient.saveConfig(v);});
                ctx.waitTicks(3);ctx.runOnClient(c -> check(!active(),"Absorption toggle ignored"));
                world.getServer().runCommand("effect clear @a");ctx.waitTicks(5);
                world.getServer().runCommand("gamemode creative @a");ctx.waitTicks(5);
                ctx.runOnClient(c -> check(!active(),"Warning in Creative despite toggle off"));
                ctx.runOnClient(c -> {var v=LowHealthWarningClient.getConfig();v.warnInCreative=true;LowHealthWarningClient.saveConfig(v);});
                ctx.waitTicks(3);ctx.runOnClient(c -> check(active(),"Creative toggle ignored"));
                world.getServer().runCommand("gamemode spectator @a");ctx.waitTicks(5);
                ctx.runOnClient(c -> check(!active(),"Spectator warning"));
                world.getServer().runCommand("gamemode survival @a");ctx.waitTicks(5);
                ctx.getInput().pressKey(GLFW.GLFW_KEY_H);ctx.waitForScreen(LowHealthConfigScreen.class);
                click(ctx,"enabled");ctx.clickScreenButton("gui.done");ctx.waitTicks(3);
                ctx.runOnClient(c -> check(!active(),"Disable did not clear warning"));
                ctx.takeScreenshot("disabled-live-warning");
                float health=ctx.computeOnClient(c -> c.player.getHealth());
                for(int mode=0;mode<3;mode++) {
                    final int m=mode;
                    ctx.runOnClient(c -> {var v=LowHealthWarningClient.getConfig();v.displayMode=m;LowHealthWarningClient.saveConfig(v);});
                    ctx.setScreen(() -> new LowHealthConfigScreen(null));click(ctx,"preview");ctx.waitTicks(3);
                    ctx.takeScreenshot("world-preview-"+mode);
                    ctx.runOnClient(c -> check(c.player.getHealth()==health && !LowHealthWarningClient.getConfig().warningEnabled,"Preview mutated health/settings"));
                    ctx.clickScreenButton("lowhealth.preview.back");ctx.clickScreenButton("gui.cancel");
                }
                ctx.takeScreenshot("world-preview-closed");
                ctx.runOnClient(c -> {var v=LowHealthWarningClient.getConfig();v.warningEnabled=true;v.warningThreshold=19;LowHealthWarningClient.saveConfig(v);});
                ctx.waitTicks(3);ctx.runOnClient(c -> check(!active(),"Warning above threshold"));
            }
        } finally {ctx.runOnClient(c -> c.getSoundManager().removeListener(listener));}
    }
    private static boolean active() {
        try {var f=LowHealthWarningClient.class.getDeclaredField("STATE");f.setAccessible(true);return ((WarningState)f.get(null)).isActive();}
        catch(Exception e){throw new AssertionError(e);}
    }
    private static EditBox edit(net.minecraft.client.gui.screens.Screen s) {return (EditBox)s.children().stream().filter(x->x instanceof EditBox).findFirst().orElseThrow();}
    private static void click(ClientGameTestContext ctx,String key) {
        String text=ctx.computeOnClient(c -> ModernCompat.screen(c).children().stream().filter(x->x instanceof Button).map(x->(Button)x)
            .filter(b->b.getMessage().getContents() instanceof TranslatableContents t && t.getKey().equals("option.lowhelthwarning."+key)).findFirst().orElseThrow().getMessage().getString());
        ctx.clickScreenButton(text);
    }
    private static void slider(ClientGameTestContext ctx,int index,boolean max) {
        double[] point=ctx.computeOnClient(c->{var w=ModernCompat.screen(c).children().stream().filter(x->x instanceof AbstractSliderButton).map(x->(AbstractSliderButton)x).toList().get(index);
            return new double[]{(max?w.getRight()-2:w.getX()+2)*(double)c.getWindow().getScreenWidth()/ModernCompat.screen(c).width,(w.getY()+10.0)*c.getWindow().getScreenHeight()/ModernCompat.screen(c).height};});
        ctx.getInput().setCursorPos(point[0],point[1]);ctx.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
    }
    private static void checkCoreAndStorage() {
        var v=new WarningConfig();
        check(v.warningEnabled && v.warningThreshold==8 && v.displayMode==0 && v.warningMessage.equals("§cLow Health!") && !v.soundEnabled && v.soundVolume==.5f && v.soundIntervalSeconds==3 && !v.includeAbsorption && !v.warnInCreative,"Published defaults changed");
        check(WarningState.shouldWarn(v,8,4,true,false,false),"Threshold equality");v.includeAbsorption=true;
        check(!WarningState.shouldWarn(v,8,4,true,false,false),"Absorption handling");
        check(!WarningState.shouldWarn(v,0,0,false,false,false) && !WarningState.shouldWarn(v,4,0,true,true,false),"Dead/spectator filtering");
        var state=new WarningState();int count=0;for(int i=0;i<61;i++)if(state.tick(true,true,3))count++;
        check(count==2,"Sound cadence");state.tick(false,true,3);check(!state.isActive(),"State reset");
        try {
            var dir=Files.createTempDirectory("lowhealth-config-test-");var file=dir.resolve("config.json");
            Files.writeString(file,"{\"warningMessage\":\"&cLegacy\",\"warningThreshold\":6,\"displayMode\":2,\"warningEnabled\":false}");
            var old=ConfigStore.load(file);check(old.warningThreshold==6 && old.displayMode==2 && !old.warningEnabled && !old.soundEnabled && !old.includeAbsorption,"Old config migration");
            old.soundEnabled=true;old.soundVolume=.7f;old.includeAbsorption=true;old.warnInCreative=true;old.soundIntervalSeconds=7;ConfigStore.save(file,old);
            var reloaded=ConfigStore.load(file);check(reloaded.soundEnabled && reloaded.soundVolume==.7f && reloaded.soundIntervalSeconds==7 && reloaded.includeAbsorption && reloaded.warnInCreative,"Restored settings persistence");
            Files.writeString(file,"broken");boolean failed=false;try{ConfigStore.load(file);}catch(java.io.IOException expected){failed=true;}
            check(failed && Files.list(dir).anyMatch(p->p.getFileName().toString().startsWith("lowhealth_config.broken-")),"Broken config backup");
        } catch(Exception e){throw new AssertionError(e);}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
