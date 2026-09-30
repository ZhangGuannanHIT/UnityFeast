package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.entity.GreenBottle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Single-client model photography fixture, run only in its own fresh local test world. */
@EventBusSubscriber(modid = FeastGameTests.ID)
public final class BottleModelScene {
    static final Path ROOT = Path.of(System.getProperty("unity_feast.project", "."));
    static final Path CONTROL = ROOT.resolve(".tools/tmp/bottle-model-fix");
    static final Path DOCUMENT = ROOT.resolve("docs/v1.3.0/bottle-model-fix");
    static final List<String> VIEWS = List.of("FRONT", "SIDE", "BACK", "TOP", "GLOW", "INVISIBLE");
    private static final List<UUID> bottles = new ArrayList<>();
    private static String phase = "INIT";
    private static String runId;
    private static int age, total;

    private static boolean enabled() {
        return System.getProperty("unity_feast.scenario", "").equals("bottle-model");
    }

    private static void phase(String value) throws Exception {
        phase = value;
        age = 0;
        Files.writeString(CONTROL.resolve("phase.txt"), value);
        System.out.println("BOTTLE MODEL SCENE " + value);
    }

    private static void view(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
        player.teleportTo(player.level(), x, y, z, Set.of(), yaw, pitch, true);
    }

    private static void orient(Mob mob, float yaw) {
        mob.setYRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setYHeadRot(yaw);
    }

    private static GreenBottle bottle(ServerLevel level, int index) {
        UUID uuid = bottles.get(index);
        var entity = level.getEntity(uuid);
        if (!(entity instanceof GreenBottle bottle) || bottle.isRemoved()) {
            throw new IllegalStateException("Live fixture bottle missing: uuid=" + uuid + " entity=" + entity);
        }
        return bottle;
    }

    private static void logFocus(ServerLevel level) {
        var bottle = bottle(level, 1);
        System.out.println("BOTTLE MODEL SERVER STATE phase=" + phase + " age=" + age
                + " id=" + bottle.getId() + " uuid=" + bottle.getUUID() + " removed=" + bottle.isRemoved()
                + " isInvisible=" + bottle.isInvisible() + " isCurrentlyGlowing=" + bottle.isCurrentlyGlowing()
                + " tickCount=" + bottle.tickCount + " chunk=" + bottle.chunkPosition()
                + " entityTicking=" + level.isPositionEntityTicking(bottle.blockPosition())
                + " level=" + bottle.level().dimension());
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) throws Exception {
        if (!enabled()) return;
        Files.createDirectories(CONTROL);
        Files.createDirectories(DOCUMENT.resolve("screenshots"));
        runId = Long.toString(System.currentTimeMillis());
        Files.writeString(CONTROL.resolve("run-id.txt"), runId);
        Files.deleteIfExists(CONTROL.resolve("failure.txt"));
        Files.deleteIfExists(CONTROL.resolve("complete.txt"));
        for (String name : VIEWS) Files.deleteIfExists(CONTROL.resolve(name + "-captured.txt"));
        bottles.clear();
        total = 0;
        phase("INIT");
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) throws Exception {
        if (!enabled()) return;
        var server = event.getServer();
        var players = server.getPlayerList().getPlayers();
        total++;
        if (phase.equals("LEAVE")) {
            if (players.isEmpty() && ++age >= 30) {
                server.saveEverything(false, true, true);
                System.out.println("BOTTLE MODEL SCENE saved and halted");
                server.halt(false);
            }
            return;
        }
        try {
            if (total > 2400) throw new IllegalStateException("single-client scenario timeout");
            if (Files.exists(CONTROL.resolve("failure.txt"))) {
                throw new IllegalStateException(Files.readString(CONTROL.resolve("failure.txt")));
            }
            var player = players.stream().filter(p -> p.getName().getString().equals("FeastOne"))
                    .findFirst().orElse(null);
            if (player == null) return;
            age++;
            if (phase.equals("INIT")) {
                if (age < 80) return;
                player.setGameMode(GameType.SPECTATOR);
                view(player, 409, 64, 411.1, 180, 8);
                phase("PREPARE");
                return;
            }
            if (phase.equals("PREPARE")) {
                // Let normal player chunk loading settle before the fixture is created.
                if (age < 100) return;
                var level = server.overworld();
                level.getGameRules().set(GameRules.SPAWN_MOBS, false, server);
                level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, 0, server);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear 1000000");
                // All fixture edits stay inside this 24 x 24 x 10 volume.
                for (int x = 396; x < 420; x++) for (int z = 396; z < 420; z++) {
                    level.setBlock(new BlockPos(x, 63, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    for (int y = 64; y < 73; y++) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                player.setGameMode(GameType.SPECTATOR);
                player.removeAllEffects();
                player.getInventory().clearContent();
                for (double x : new double[] {405.8, 408, 410.2}) {
                    var bottle = UnityFeastMod.GREEN_BOTTLE.get().create(level, EntitySpawnReason.COMMAND);
                    if (bottle == null) throw new IllegalStateException("bottle creation failed");
                    bottle.snapTo(x, 64, 405, 0, 0);
                    bottle.setNoAi(true);
                    bottle.setPersistenceRequired();
                    orient(bottle, 0);
                    level.addFreshEntity(bottle);
                    bottles.add(bottle.getUUID());
                }
                Files.writeString(CONTROL.resolve("focus-uuid.txt"), bottles.get(1).toString());
                var pig = EntityType.PIG.create(level, EntitySpawnReason.COMMAND);
                if (pig == null) throw new IllegalStateException("reference pig creation failed");
                pig.snapTo(412.2, 64, 405, 0, 0);
                pig.setNoAi(true);
                pig.setPersistenceRequired();
                orient(pig, 0);
                level.addFreshEntity(pig);
                view(player, 409, 64, 411.1, 180, 8);
                phase("FRONT");
                return;
            }
            if ((phase.equals("GLOW") || phase.equals("INVISIBLE")) && age % 60 == 0) {
                logFocus(server.overworld());
            }
            if (age > 400) throw new IllegalStateException("capture did not complete: " + phase);
            if (age < 100) return;
            Path acknowledgement = CONTROL.resolve(phase + "-captured.txt");
            if (!Files.exists(acknowledgement) || !Files.readString(acknowledgement).equals(runId)) return;
            switch (phase) {
                case "FRONT" -> {
                    for (int i = 0; i < bottles.size(); i++) orient(bottle(server.overworld(), i), 90);
                    view(player, 408, 64, 408.4, 180, 14);
                    phase("SIDE");
                }
                case "SIDE" -> {
                    for (int i = 0; i < bottles.size(); i++) orient(bottle(server.overworld(), i), 0);
                    view(player, 408, 64, 401.6, 0, 14);
                    phase("BACK");
                }
                case "BACK" -> {
                    view(player, 408, 66.1, 407.1, 180, 52);
                    phase("TOP");
                }
                case "TOP" -> {
                    // Spectators can see invisible mobs. Use creative flight so the next
                    // two captures exercise the normal player's outline-only invisibility.
                    player.setGameMode(GameType.CREATIVE);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    bottle(server.overworld(), 1).setGlowingTag(true);
                    view(player, 410.6, 64, 407.6, 135, 13);
                    phase("GLOW");
                }
                case "GLOW" -> {
                    var focus = bottle(server.overworld(), 1);
                    focus.setGlowingTag(true);
                    focus.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 0, false, false));
                    phase("INVISIBLE");
                }
                case "INVISIBLE" -> {
                    Files.writeString(CONTROL.resolve("complete.txt"),
                            "Six real 1280x800 framebuffer captures written; visual rendering requires review; run=" + runId + "\n");
                    phase("LEAVE");
                }
                default -> throw new IllegalStateException("unknown scene phase: " + phase);
            }
        } catch (Exception exception) {
            Files.writeString(CONTROL.resolve("failure.txt"), exception.toString());
            System.out.println("BOTTLE MODEL SCENE FAILURE " + exception);
            exception.printStackTrace();
            phase("LEAVE");
        }
    }
}
