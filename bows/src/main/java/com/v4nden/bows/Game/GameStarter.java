package com.v4nden.bows.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.v4nden.bows.Bows;
import com.v4nden.bows.BowsUtils;

/**
 * Подготовка игры: случайная точка мира, проверка местности, назначение каждому
 * участнику точки появления и прегенерация 5x5 чанков вокруг каждой, затем старт.
 *
 * Местность проверяется по биомам из шумов генератора (getComputedBiome не
 * генерирует чанки): в центре не океан и не река, суши — больше половины.
 */
public class GameStarter {
    private static final int WORLD_LIMIT = 1_000_000;
    // половина стартового размера границы (Game.startGame ставит 1400)
    private static final int MAP_RADIUS = 700;
    // точки появления — в стольких блоках от центра (как в Game.startGame)
    private static final int SPAWN_SPREAD = 100;
    // прегенерируются только чанки SPAWN_CHUNKS x SPAWN_CHUNKS вокруг точки каждого участника
    private static final int SPAWN_CHUNKS = 5;
    private static final int SAMPLES_PER_AXIS = 21;
    private static final double MIN_LAND = .5;
    private static final int MAX_ATTEMPTS = 200;
    private static final int CHUNKS_IN_FLIGHT = 32;
    private static final int PROGRESS_STEP_PERCENT = 10;

    private static boolean preparing = false;
    // тестовая игра: готовится для одного игрока, сообщения — только ему
    private static Player testPlayer = null;
    // номер подготовки: отменённая не должна стартовать игру из своих колбэков
    private static int preparation = 0;

    public static boolean isPreparing() {
        return preparing;
    }

    public static void start() {
        start(null);
    }

    /** testFor — игрок тестовой игры, null — обычная игра для всех онлайн. */
    public static void start(Player testFor) {
        if (preparing || Game.isRunning) {
            BowsUtils.broadcastSystemMessage(testFor != null ? List.of(testFor) : Bukkit.getOnlinePlayers(),
                    "Игра уже готовится или идёт");
            return;
        }
        preparing = true;
        testPlayer = testFor;
        preparation++;
        attempt(Bukkit.getWorld("world"), 1, preparation);
    }

    public static void cancel() {
        if (preparing) {
            preparing = false;
            preparation++;
            announce("Подготовка игры отменена");
            testPlayer = null;
        }
    }

    private static void attempt(World world, int attempt, int id) {
        if (id != preparation) {
            return;
        }
        if (attempt > MAX_ATTEMPTS) {
            preparing = false;
            announce("Не нашлось подходящего места за " + MAX_ATTEMPTS + " попыток, попробуй ещё раз");
            testPlayer = null;
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int limit = WORLD_LIMIT - MAP_RADIUS;
        int centerX = random.nextInt(-limit, limit + 1);
        int centerZ = random.nextInt(-limit, limit + 1);

        if (isWaterBiome(world, centerX, centerZ) || landShare(world, centerX, centerZ) <= MIN_LAND) {
            // следующая попытка в следующем тике: перебор не подвешивает сервер
            Bukkit.getScheduler().runTask(Bows.instance, () -> attempt(world, attempt + 1, id));
            return;
        }
        pregenerate(world, centerX, centerZ, id);
    }

    // доля суши по сетке точек на всей будущей карте
    private static double landShare(World world, int centerX, int centerZ) {
        double step = 2.0 * MAP_RADIUS / (SAMPLES_PER_AXIS - 1);
        int land = 0;
        for (int i = 0; i < SAMPLES_PER_AXIS; i++) {
            for (int j = 0; j < SAMPLES_PER_AXIS; j++) {
                if (!isWaterBiome(world, (int) (centerX - MAP_RADIUS + i * step), (int) (centerZ - MAP_RADIUS + j * step))) {
                    land++;
                }
            }
        }
        return land / (double) (SAMPLES_PER_AXIS * SAMPLES_PER_AXIS);
    }

    // биом считается из шумов генератора — для несгенерированных чанков ничего не генерируется
    private static boolean isWaterBiome(World world, int x, int z) {
        String biome = world.getComputedBiome(x, world.getSeaLevel(), z).getKey().getKey();
        return biome.contains("ocean") || biome.contains("river");
    }

    private static void pregenerate(World world, int centerX, int centerZ, int id) {
        // участники фиксируются сейчас; зашедшие позже станут наблюдателями
        List<Player> participants = testPlayer != null ? List.of(testPlayer)
                : new ArrayList<>(Bukkit.getOnlinePlayers());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<UUID, Location> spawns = new HashMap<>();
        Set<Long> chunkKeys = new LinkedHashSet<>();
        int half = SPAWN_CHUNKS / 2;
        for (Player participant : participants) {
            int x = centerX + random.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD);
            int z = centerZ + random.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD);
            spawns.put(participant.getUniqueId(), new Location(world, x, world.getMaxHeight(), z));
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    // совпадающие чанки у соседних точек генерируются один раз
                    chunkKeys.add(((long) ((x >> 4) + dx) << 32) | (((z >> 4) + dz) & 0xFFFFFFFFL));
                }
            }
        }
        List<int[]> chunks = new ArrayList<>();
        for (long key : chunkKeys) {
            chunks.add(new int[] { (int) (key >> 32), (int) key });
        }
        announce("Генерация точек появления: " + participants.size() + " игроков, " + chunks.size() + " чанков");

        new Pregenerator(world, chunks, id, () -> {
            preparing = false;
            Location center = new Location(world, centerX + .5, world.getMaxHeight(), centerZ + .5);
            List<Player> online = participants.stream().filter(Player::isOnline).toList();
            if (testPlayer != null) {
                if (!online.isEmpty()) {
                    announce("Карта готова, тестовая игра запущена, остановить: /stopgame");
                    Game.startGame(center, online, true, spawns);
                }
            } else if (online.isEmpty()) {
                announce("Все участники вышли, игра не начата");
            } else {
                announce("Карта готова, старт!");
                Game.startGame(center, online, false, spawns);
            }
            testPlayer = null;
        }).next();
    }

    private static void announce(String message) {
        if (testPlayer != null) {
            BowsUtils.broadcastSystemMessage(testPlayer, message);
        } else {
            BowsUtils.broadcastSystemMessage(Bukkit.getOnlinePlayers(), message);
        }
    }

    // держит в работе не больше CHUNKS_IN_FLIGHT генераций, прогресс — каждые 10%
    private static class Pregenerator {
        private final World world;
        private final List<int[]> chunks;
        private final int id;
        private final Runnable done;
        private int requested = 0;
        private int completed = 0;
        private int inFlight = 0;
        private int lastPercent = 0;
        private final long startedAt = System.currentTimeMillis();

        Pregenerator(World world, List<int[]> chunks, int id, Runnable done) {
            this.world = world;
            this.chunks = chunks;
            this.id = id;
            this.done = done;
        }

        // колбэки getChunkAtAsync приходят в основной поток
        void next() {
            if (id != preparation) {
                return;
            }
            while (inFlight < CHUNKS_IN_FLIGHT && requested < chunks.size()) {
                int[] chunk = chunks.get(requested++);
                inFlight++;
                world.getChunkAtAsync(chunk[0], chunk[1], true).whenComplete((result, error) -> {
                    inFlight--;
                    completed++;
                    report();
                    if (completed == chunks.size()) {
                        if (id == preparation) {
                            done.run();
                        }
                    } else {
                        next();
                    }
                });
            }
        }

        private void report() {
            int percent = completed * 100 / chunks.size();
            if (percent >= lastPercent + PROGRESS_STEP_PERCENT && percent < 100) {
                lastPercent = percent - percent % PROGRESS_STEP_PERCENT;
                long seconds = (System.currentTimeMillis() - startedAt) / 1000;
                announce("Генерация карты: " + lastPercent + "% (" + completed + "/" + chunks.size() + ", " + seconds
                        + " с)");
            }
        }
    }
}
