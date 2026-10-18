package slooth.lowhealth.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.lang.reflect.*;
import slooth.lowhealth.*;

@Mod(modid="lowhealthaudit",name="Low Health Audit Fixture",version="1",dependencies="required-after:lowhelthwarning",clientSideOnly=true,acceptableRemoteVersions="*")
public final class ForgeAudit {
    private static volatile long ticks;
    private static volatile boolean shutdownRequested;
    private static volatile int sounds;
    private static volatile float volume;
    private int screenshot;
    private final Minecraft mc=Minecraft.getMinecraft();
    @Mod.EventHandler public void init(FMLInitializationEvent e){
        FMLCommonHandler.instance().bus().register(this);MinecraftForge.EVENT_BUS.register(this);
        Thread t=new Thread(()->{try {run();Files.write(mc.mcDataDir.toPath().resolve("audit-result.txt"),Arrays.asList("PASS"));System.out.println("LOWHEALTH_FORGE_AUDIT_PASS");shutdownRequested=true;}
            catch(Throwable error){error.printStackTrace();System.err.println("LOWHEALTH_FORGE_AUDIT_FAILED");FMLCommonHandler.instance().exitJava(1,false);}},"lowhealth-forge-audit");t.setDaemon(true);t.start();
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END){ticks++;if(shutdownRequested){shutdownRequested=false;mc.shutdown();}}}
    @SubscribeEvent public void sound(PlaySoundEvent e){if(e.sound.getSoundLocation().toString().equals("minecraft:note.pling")){sounds++;volume=e.sound.getVolume();}}
    private <T>T client(Callable<T> work){try{return mc.addScheduledTask(work).get(90,TimeUnit.SECONDS);}catch(Exception e){throw new AssertionError(e);}}
    private void waitUntil(BooleanSupplier test){long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(90);while(!test.getAsBoolean()){if(System.nanoTime()>end)throw new AssertionError("Timeout");try{Thread.sleep(10);}catch(Exception e){throw new AssertionError(e);}}}
    private void waitTicks(int n){long end=ticks+n;waitUntil(()->ticks>=end);}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private Object field(Object object,String name){try{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new AssertionError(e);}}
    @SuppressWarnings("unchecked") private List<GuiButton> buttons(GuiScreen screen){try{return (List<GuiButton>)ReflectionHelper.findField(GuiScreen.class,"buttonList","field_146292_n").get(screen);}catch(Exception e){throw new AssertionError(e);}}
    private void open(){client(()->{mc.displayGuiScreen(new SettingsScreen(LowHealthForge.INSTANCE,mc.theWorld==null?new GuiMainMenu():null));return null;});waitTicks(2);}
    private void click(int id){client(()->{GuiScreen s=mc.currentScreen;GuiButton b=buttons(s).stream().filter(x->x.id==id).findFirst().get();check(b.enabled&&b.visible,"button unavailable: "+id);
        if(s instanceof SettingsScreen)((SettingsScreen)s).mouseClicked(b.xPosition+3,b.yPosition+3,0);
        else {ReflectionHelper.findMethod((Class)s.getClass(),s,new String[]{"actionPerformed","func_146284_a"},GuiButton.class).invoke(s,b);}
        return null;});waitTicks(2);}
    private void backPreview(){click(0);}
    private void escape(){client(()->{((SettingsScreen)mc.currentScreen).keyTyped('\0',1);return null;});waitTicks(2);}
    private void shot(String name){waitTicks(3);client(()->{ScreenShotHelper.saveScreenshot(mc.mcDataDir,String.format("%04d_%s.png",screenshot++,name),mc.displayWidth,mc.displayHeight,mc.getFramebuffer());return null;});}
    private boolean active(){return client(()->((WarningState)field(LowHealthForge.INSTANCE,"state")).isActive());}
    private WarningConfig config(){return client(()->LowHealthForge.INSTANCE.draft());}
    private void save(WarningConfig c){client(()->{check(LowHealthForge.INSTANCE.save(c),"save config");return null;});waitTicks(2);}
    private void command(String cmd){try{mc.getIntegratedServer().addScheduledTask(()->mc.getIntegratedServer().getCommandManager().executeCommand(mc.getIntegratedServer(),cmd)).get(30,TimeUnit.SECONDS);}catch(Exception e){throw new AssertionError(e);}waitTicks(5);}
    private void run() throws Exception {
        waitUntil(()->client(()->mc.currentScreen instanceof GuiMainMenu));
        client(()->{mc.gameSettings.pauseOnLostFocus=false;return null;});
        client(()->{net.minecraftforge.fml.client.GuiModList menu=new net.minecraftforge.fml.client.GuiModList(new GuiMainMenu());mc.displayGuiScreen(menu);
            List<net.minecraftforge.fml.common.ModContainer> mods=(List<net.minecraftforge.fml.common.ModContainer>)field(menu,"mods");
            for(int i=0;i<mods.size();i++)if(mods.get(i).getModId().equals("lowhelthwarning"))menu.selectModIndex(i);
            return null;});waitTicks(3);click(20);
        check(client(()->mc.currentScreen instanceof SettingsScreen),"Forge Mods config entry");click(13);
        check(client(()->mc.currentScreen instanceof net.minecraftforge.fml.client.GuiModList),"return to Mods menu");
        client(()->{mc.displayGuiScreen(new GuiMainMenu());return null;});
        WarningConfig defaults=new WarningConfig();save(defaults);
        check(defaults.warningThreshold==8&&!defaults.soundEnabled&&defaults.soundVolume==.5f&&defaults.soundIntervalSeconds==3,"defaults");
        Path temp=Files.createTempDirectory("lowhealth-forge-config");Path cfg=temp.resolve("config.json");
        Files.write(cfg,Arrays.asList("{\"warningEnabled\":false,\"warningThreshold\":6}"));
        check(ConfigStore.load(cfg).warningThreshold==6&&!ConfigStore.load(cfg).soundEnabled,"migration");
        Files.write(cfg,Arrays.asList("broken"));boolean failed=false;try{ConfigStore.load(cfg);}catch(java.io.IOException expected){failed=true;}
        check(failed&&Files.list(temp).anyMatch(p->p.getFileName().toString().startsWith("lowhealth_config.broken-")),"broken backup");
        for(int[] size:new int[][]{{640,480,2},{1280,720,3},{1920,1080,4}}){
            client(()->{Display.setDisplayMode(new DisplayMode(size[0],size[1]));mc.gameSettings.guiScale=size[2];
                ReflectionHelper.findMethod(Minecraft.class,mc,new String[]{"resize","func_71370_a"},int.class,int.class).invoke(mc,size[0],size[1]);return null;});
            open();client(()->{GuiScreen s=mc.currentScreen;for(GuiButton b:buttons(s)){check(b.xPosition>=0&&b.yPosition>=0&&b.xPosition+b.width<=s.width&&b.yPosition+b.height<=s.height,"clipped button");}
                ((GuiTextField)field(s,"message")).setText("&cLong warning Long warning Long warning Long warning Long warning Long warning");return null;});
            shot("settings-"+size[0]);click(1);click(1);click(9);shot("long-preview-"+size[0]);backPreview();click(13);
        }
        check(config().warningMessage.equals(defaults.warningMessage),"cancel changed config");
        open();for(int i=0;i<12;i++)click(2);click(12);check(config().warningThreshold==1,"minimum threshold");
        open();for(int i=0;i<45;i++)click(3);click(12);check(config().warningThreshold==40,"maximum threshold");
        open();for(int i=0;i<11;i++)click(5);int testSound=sounds;click(11);check(sounds==testSound+1,"sound-test button");click(12);check(config().soundVolume==.5f,"volume cycle");
        open();click(10);click(12);

        open();click(0);click(4);click(5);click(6);click(7);click(8);click(3);
        client(()->{GuiTextField text=(GuiTextField)field(mc.currentScreen,"message");text.setText("");text.setFocused(true);for(char ch:"&aAudit &ltest".toCharArray())((SettingsScreen)mc.currentScreen).keyTyped(ch,0);return null;});click(12);
        WarningConfig v=config();check(!v.warningEnabled&&v.soundEnabled&&v.soundVolume==.6f&&v.soundIntervalSeconds==4&&v.includeAbsorption&&v.warnInCreative&&v.warningThreshold==9&&v.warningMessage.equals("&aAudit &ltest"),"controls");
        WarningConfig disk=ConfigStore.load(mc.mcDataDir.toPath().resolve("config/lowhealth_config.json"));check(disk.soundEnabled&&disk.warningMessage.equals(v.warningMessage)&&disk.warnInCreative,"persistence");
        open();click(10);escape();check(config().warningThreshold==9,"escape saved reset");open();click(10);click(12);check(config().warningThreshold==8&&!config().soundEnabled,"reset done");
        for(int mode=0;mode<3;mode++){v=new WarningConfig();v.displayMode=mode;v.warningEnabled=false;v.soundEnabled=true;v.soundVolume=.25f;save(v);open();int before=sounds;click(9);waitTicks(3);check(sounds==before+1&&Math.abs(volume-.25f)<.001,"preview sound");shot("preview-"+mode);client(()->{mc.currentScreen.setWorldAndResolution(mc,mc.currentScreen.width,mc.currentScreen.height);return null;});waitTicks(3);check(sounds==before+1,"resize replayed sound");backPreview();click(13);}
        client(()->{mc.launchIntegratedServer("lowhealth-audit-"+System.currentTimeMillis(),"LowHealth audit",new WorldSettings(123L,WorldSettings.GameType.SURVIVAL,true,false,WorldType.FLAT));return null;});
        waitUntil(()->client(()->mc.thePlayer!=null&&mc.theWorld!=null));waitTicks(40);
        command("gamerule doMobSpawning false");command("time set day");
        v=new WarningConfig();v.warningThreshold=20;v.displayMode=2;v.soundEnabled=true;v.soundIntervalSeconds=1;v.soundVolume=.25f;save(v);int before=sounds;waitTicks(42);check(active(),"live threshold");check(sounds>=before+2&&Math.abs(volume-.25f)<.001,"live sound");shot("live-warning");
        v=config();v.soundEnabled=false;save(v);before=sounds;waitTicks(25);check(sounds==before,"sound disabled");
        command("effect @a 22 60 0 true");check(active(),"absorption excluded");v=config();v.includeAbsorption=true;save(v);waitTicks(3);check(!active(),"absorption included");command("effect @a clear");
        command("gamemode creative @a");check(!active(),"creative excluded");v=config();v.warnInCreative=true;save(v);waitTicks(3);check(active(),"creative included");command("gamemode spectator @a");check(!active(),"spectator");command("gamemode survival @a");
        client(()->{KeyBinding.onTick(35);return null;});waitTicks(3);check(client(()->mc.currentScreen instanceof SettingsScreen),"H shortcut");click(0);click(12);waitTicks(3);check(!active(),"disabled");
        float health=client(()->mc.thePlayer.getHealth());for(int mode=0;mode<3;mode++){v=config();v.displayMode=mode;save(v);open();click(9);shot("world-preview-"+mode);check(client(()->mc.thePlayer.getHealth())==health&&!config().warningEnabled,"preview state");backPreview();click(13);}
        v=config();v.warningEnabled=true;v.warningThreshold=19;save(v);waitTicks(3);check(!active(),"above threshold");shot("warning-cleared");

    }
}
