package slooth.lowhealth.audit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.contents.TranslatableContents;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import java.util.concurrent.*;
import java.util.function.*;
import java.nio.file.*;

/** Small real-client driver; all game/UI operations run on Minecraft's render thread. */
public final class ClientGameTestContext {
    private final Minecraft client = Minecraft.getInstance();
    private final Input input = new Input();
    private int screenshotIndex;
    public void runOnClient(Consumer<Minecraft> action) { computeOnClient(c -> {action.accept(c); return null;}); }
    public <T> T computeOnClient(Function<Minecraft,T> action) {
        CompletableFuture<T> result = new CompletableFuture<>();
        client.execute(() -> {try {result.complete(action.apply(client));}catch(Throwable e){result.completeExceptionally(e);}});
        try {return result.get(120,TimeUnit.SECONDS);} catch(Exception e){throw new AssertionError(e);}
    }
    private void waitFrames(int count) {
        for (int i=0;i<count;i++) {
            long previous=computeOnClient(c -> c.getFrameTimeNs());
            await(() -> computeOnClient(c -> c.getFrameTimeNs()!=previous),"rendered frame");
        }
    }
    public void waitTicks(int count) {
        long end=LegacyAuditRunner.ticks+count;
        await(() -> LegacyAuditRunner.ticks>=end,"client ticks");
    }
    private static void await(BooleanSupplier condition,String label) {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(120);
        while(!condition.getAsBoolean()) {
            if(System.nanoTime()>deadline) throw new AssertionError("Timed out: "+label);
            try {Thread.sleep(10);} catch(InterruptedException e){throw new AssertionError(e);}
        }
    }
    public void waitForScreen(Class<? extends Screen> type) {
        await(() -> computeOnClient(c -> {
            if(c.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) c.screen.onClose();
            return type.isInstance(c.screen) && c.getOverlay() == null;
        }),type.getSimpleName());
        waitFrames(2);
    }
    public void setScreen(Supplier<Screen> factory) {runOnClient(c -> c.setScreen(factory.get()));waitTicks(2);}
    public Input getInput(){return input;}
    public void clickScreenButton(String text) {
        runOnClient(c -> {
            Button b=(Button)c.screen.children().stream().filter(x -> x instanceof Button)
                .filter(x -> {var m=((Button)x).getMessage();return m.getString().equals(text) || m.getContents() instanceof TranslatableContents t && t.getKey().equals(text);})
                .findFirst().orElseThrow(() -> new AssertionError("No button: "+text+" on "+c.screen));
            if(!b.active) throw new AssertionError("Disabled button: "+text);
            c.screen.mouseClicked(b.getX()+b.getWidth()/2.0,b.getY()+b.getHeight()/2.0,0);
            if(c.screen!=null)c.screen.mouseReleased(b.getX()+b.getWidth()/2.0,b.getY()+b.getHeight()/2.0,0);
        });waitTicks(2);
    }
    public void takeScreenshot(String name) {
        await(() -> computeOnClient(c -> c.getOverlay() == null), "loading overlay");
        waitFrames(3);
        String file=String.format("%04d_%s.png",screenshotIndex++,name);
        Path path=client.gameDirectory.toPath().resolve("screenshots").resolve(file);
        try {Files.deleteIfExists(path);}catch(Exception e){throw new AssertionError(e);}
        runOnClient(c -> Screenshot.grab(c.gameDirectory,file,c.getMainRenderTarget(),message -> {}));
        await(() -> Files.exists(path),"screenshot "+file);
    }
    public WorldBuilder worldBuilder(){return new WorldBuilder();}
    public final class Input {
        private double x,y;
        public void resizeWindow(int w,int h){runOnClient(c -> GLFW.glfwSetWindowSize(c.getWindow().getWindow(),w,h));waitTicks(4);}
        public void pressKey(int key){runOnClient(c -> {
            if(c.screen!=null)c.screen.keyPressed(key,0,0);
            else KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(key));
        });waitTicks(2);}
        public void typeChars(String text){runOnClient(c -> {for(char ch:text.toCharArray()) c.screen.charTyped(ch,0);});}
        public void setCursorPos(double x,double y){this.x=x;this.y=y;}
        public void pressMouse(int button){runOnClient(c -> {
            double gx=x*c.screen.width/c.getWindow().getScreenWidth(),gy=y*c.screen.height/c.getWindow().getScreenHeight();
            c.screen.mouseClicked(gx,gy,button);c.screen.mouseReleased(gx,gy,button);
        });waitTicks(2);}
    }
    public final class WorldBuilder {
        public TestWorld create(){
            runOnClient(c -> CreateWorldScreen.openFresh(c,new TitleScreen()));
            waitForScreen(CreateWorldScreen.class);
            runOnClient(c -> c.screen.children().stream().filter(x->x instanceof EditBox).map(x->(EditBox)x).findFirst().ifPresent(e->e.setValue("LowHealth audit "+System.currentTimeMillis())));
            clickScreenButton("selectWorld.create");
            await(() -> computeOnClient(c -> c.player!=null && c.level!=null && c.getSingleplayerServer()!=null),"test world");
            runOnClient(c -> c.setScreen(null));waitTicks(30);
            return new TestWorld();
        }
    }
    public final class TestWorld implements AutoCloseable {
        public TestWorld getClientWorld(){return this;}
        public void waitForChunksRender(){waitTicks(40);}
        public TestWorld getServer(){return this;}
        public void runCommand(String command){
            var server=computeOnClient(c->c.getSingleplayerServer());
            try {server.submit(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command)).get(30,TimeUnit.SECONDS);}
            catch(Exception e){throw new AssertionError(e);}
        }
        public void close(){runOnClient(c->{if(c.level!=null)c.level.disconnect();c.disconnect();c.setScreen(new TitleScreen());});waitForScreen(TitleScreen.class);}
    }
}
