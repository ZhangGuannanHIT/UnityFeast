package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.entity.GreenBottle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Captures the unmodified game framebuffer, never a synthetic model preview. */
@EventBusSubscriber(modid = FeastGameTests.ID, value = Dist.CLIENT)
public final class BottleModelSceneClient {
    private static final Set<String> VIEWS = Set.of("FRONT", "SIDE", "BACK", "TOP", "GLOW", "INVISIBLE");
    private static String phase = "";
    private static int age;
    private static int synchronizedTicks;
    private static boolean captureRequested;

    private static boolean synchronizedState(Minecraft mc, Path control, boolean log) throws Exception {
        UUID focusUuid = UUID.fromString(Files.readString(control.resolve("focus-uuid.txt")).trim());
        var nearby = mc.level.getEntitiesOfClass(GreenBottle.class, mc.player.getBoundingBox().inflate(16));
        GreenBottle focus = null;
        boolean neighborsUnchanged = true;
        for (var bottle : nearby) {
            if (log) {
                System.out.println("BOTTLE MODEL CLIENT STATE phase=" + phase + " age=" + age
                        + " id=" + bottle.getId() + " uuid=" + bottle.getUUID() + " removed=" + bottle.isRemoved()
                        + " flags={isInvisible=" + bottle.isInvisible() + ",isCurrentlyGlowing=" + bottle.isCurrentlyGlowing()
                        + ",isInvisibleToPlayer=" + bottle.isInvisibleTo(mc.player) + "} tickCount=" + bottle.tickCount
                        + " gamemode=" + mc.gameMode.getPlayerMode() + " chunk=" + bottle.chunkPosition()
                        + " level=" + bottle.level().dimension());
            }
            if (bottle.getUUID().equals(focusUuid)) focus = bottle;
            else if (bottle.isInvisible() || bottle.isCurrentlyGlowing()) neighborsUnchanged = false;
        }
        boolean glow = phase.equals("GLOW") || phase.equals("INVISIBLE");
        boolean invisible = phase.equals("INVISIBLE");
        boolean ready = nearby.size() == 3 && neighborsUnchanged && focus != null && !focus.isRemoved()
                && focus.tickCount > 0 && focus.isCurrentlyGlowing() == glow && focus.isInvisible() == invisible
                && (!glow || mc.gameMode.getPlayerMode() == GameType.CREATIVE)
                && (!invisible || focus.isInvisibleTo(mc.player));
        if (log) System.out.println("BOTTLE MODEL CLIENT SYNC phase=" + phase + " ready=" + ready
                + " nearbyCount=" + nearby.size() + " focusUuid=" + focusUuid);
        return ready;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) throws Exception {
        if (!System.getProperty("unity_feast.scenario", "").equals("bottle-model")) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!mc.player.getName().getString().equals("FeastOne")) return;
        Path root = Path.of(System.getProperty("unity_feast.project", "."));
        Path control = root.resolve(".tools/tmp/bottle-model-fix");
        if (!Files.exists(control.resolve("phase.txt"))) return;
        String next = Files.readString(control.resolve("phase.txt")).trim();
        if (next.isEmpty()) return;
        if (!phase.equals(next)) {
            phase = next;
            age = 0;
            synchronizedTicks = 0;
            captureRequested = false;
            mc.setScreen(null);
            mc.options.hideGui = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.fov().set(50);
            mc.options.bobView().set(false);
            mc.options.entityShadows().set(true);
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keyShift.setDown(false);
            mc.options.keySprint.setDown(false);
            mc.getWindow().setWindowed(1280, 800);
        }
        age++;
        if (VIEWS.contains(phase) && age >= 65 && !captureRequested) {
            boolean log = age == 65 || age % 20 == 0;
            if (!synchronizedState(mc, control, log)) {
                synchronizedTicks = 0;
                if (age >= 300) Files.writeString(control.resolve("failure.txt"),
                        "Client entity flags did not synchronize for phase " + phase + "; no screenshot accepted");
                return;
            }
            // Give the render thread several frames after the synchronized flags arrive.
            if (++synchronizedTicks < 5) return;
            if (!log) synchronizedState(mc, control, true);
            final String capturedPhase = phase;
            final String runId = Files.readString(control.resolve("run-id.txt"));
            Path document = root.resolve("docs/v1.3.0/bottle-model-fix");
            Files.createDirectories(document.resolve("screenshots"));
            String fileName = phase.toLowerCase(Locale.ROOT) + ".png";
            Path output = document.resolve("screenshots").resolve(fileName);
            var framebuffer = mc.getMainRenderTarget();
            if (framebuffer.width != 1280 || framebuffer.height != 800) {
                Files.writeString(control.resolve("failure.txt"),
                        "Expected 1280x800 framebuffer, got " + framebuffer.width + "x" + framebuffer.height);
                return;
            }
            long captureStarted = System.currentTimeMillis();
            captureRequested = true;
            Screenshot.grab(document.toFile(), fileName, framebuffer, 1, message -> {
                System.out.println("BOTTLE MODEL SCREENSHOT " + capturedPhase + " " + message.getString());
                try {
                    if (!Files.isRegularFile(output) || Files.getLastModifiedTime(output).toMillis() < captureStarted) {
                        throw new IllegalStateException("Screenshot was not written: " + message.getString());
                    }
                    Files.writeString(control.resolve(capturedPhase + "-captured.txt"), runId);
                } catch (Exception exception) {
                    try {
                        Files.writeString(control.resolve("failure.txt"), exception.toString());
                    } catch (Exception writeFailure) {
                        writeFailure.printStackTrace();
                    }
                }
            });
        }
        if (phase.equals("LEAVE") && age == 10) mc.stop();
    }
}
