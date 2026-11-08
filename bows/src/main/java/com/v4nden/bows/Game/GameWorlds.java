package com.v4nden.bows.Game;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import com.v4nden.bows.Bows;

import net.kyori.adventure.util.TriState;

/**
 * Отдельный мир на каждую игру: создаётся с настройками WorldSettings, после
 * игры выгружается и удаляется целиком — основной мир не растёт.
 */
public class GameWorlds {
    public static final String PREFIX = "bows_game_";

    public static World lobby() {
        return Bukkit.getWorld("world");
    }

    public static Location lobbySpawn() {
        return new Location(lobby(), 0, 100, 0);
    }

    public static boolean isGameWorld(World world) {
        return world != null && world.getName().startsWith(PREFIX);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static World create() {
        World world = new WorldCreator(PREFIX + System.currentTimeMillis())
                .type(WorldSettings.type())
                .seed(WorldSettings.seed())
                .generateStructures(WorldSettings.structures())
                // спавн мира не нужен: игроки появляются в найденной точке
                .keepSpawnLoaded(TriState.FALSE)
                .createWorld();
        world.setDifficulty(WorldSettings.difficulty());
        world.setAutoSave(false);
        // правила игры — как в основном мире
        World lobby = lobby();
        for (GameRule rule : GameRule.values()) {
            // в списке есть правила, недоступные этой версии мира (экспериментальные и т.п.)
            try {
                Object value = lobby.getGameRuleValue(rule);
                if (value != null) {
                    world.setGameRule(rule, value);
                }
            } catch (IllegalArgumentException e) {
                // правило недоступно — пропускаем
            }
        }
        return world;
    }

    /** Выгружает мир игры и удаляет его папку. Кто в нём остался — в лобби. */
    public static void delete(World world) {
        if (!isGameWorld(world)) {
            return;
        }
        for (Player player : world.getPlayers()) {
            player.teleport(lobbySpawn());
        }
        File folder = world.getWorldFolder();
        if (!Bukkit.unloadWorld(world, false)) {
            Bows.instance.getLogger().warning("Не удалось выгрузить мир " + world.getName()
                    + ", он удалится при следующем включении плагина");
            return;
        }
        if (Bows.instance.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(Bows.instance, () -> deleteFolder(folder.toPath()));
        }
        // при выключении плагина папку удалит cleanupLeftovers при следующем включении
    }

    /**
     * Убирает миры игр, оставшиеся после падения или перезагрузки: загруженные
     * выгружает и удаляет, папки незагруженных удаляет. Вызывается при включении
     * плагина, когда игры точно нет.
     */
    public static void cleanupLeftovers() {
        for (World world : Bukkit.getWorlds()) {
            if (isGameWorld(world)) {
                delete(world);
            }
        }
        File[] folders = Bukkit.getWorldContainer().listFiles(
                file -> file.isDirectory() && file.getName().startsWith(PREFIX) && Bukkit.getWorld(file.getName()) == null);
        if (folders == null) {
            return;
        }
        for (File folder : folders) {
            Bukkit.getScheduler().runTaskAsynchronously(Bows.instance, () -> deleteFolder(folder.toPath()));
        }
    }

    private static void deleteFolder(Path folder) {
        if (!Files.exists(folder)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            Bows.instance.getLogger().warning("Не удалось удалить " + folder + ": " + e.getMessage());
        }
    }
}
