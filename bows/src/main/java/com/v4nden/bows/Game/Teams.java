package com.v4nden.bows.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Командный режим (/startgame N): игроки случайно делятся на команды по N,
 * последняя команда может быть меньше. Свои не ранят своих, бусты на своих не
 * действуют (Game.targetsFor), побеждает последняя живая команда.
 */
public class Teams {
    // цвета ника в табе и над головой; при большом числе команд идут по кругу
    private static final ChatColor[] COLORS = { ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, ChatColor.YELLOW,
            ChatColor.LIGHT_PURPLE, ChatColor.AQUA, ChatColor.GOLD, ChatColor.DARK_GREEN, ChatColor.DARK_PURPLE,
            ChatColor.DARK_AQUA, ChatColor.DARK_RED, ChatColor.DARK_BLUE, ChatColor.WHITE, ChatColor.GRAY };

    private static final Map<UUID, Integer> teamOf = new HashMap<>();
    private static int teamCount = 0;

    public static boolean isActive() {
        return teamCount > 0;
    }

    /**
     * Делит игроков на команды по size. Игроки из fixedNames — ровно одна
     * команда, сколько бы их ни было; остальные делятся случайно.
     */
    public static List<List<Player>> assign(List<Player> players, int size, List<String> fixedNames) {
        reset();
        List<List<Player>> teams = new ArrayList<>();
        List<Player> fixed = players.stream()
                .filter(player -> fixedNames.stream().anyMatch(name -> name.equalsIgnoreCase(player.getName())))
                .toList();
        if (!fixed.isEmpty()) {
            for (Player player : fixed) {
                teamOf.put(player.getUniqueId(), teams.size());
            }
            teams.add(new ArrayList<>(fixed));
        }
        List<Player> shuffled = new ArrayList<>(players);
        shuffled.removeAll(fixed);
        Collections.shuffle(shuffled);
        for (int i = 0; i < shuffled.size(); i += size) {
            List<Player> team = new ArrayList<>(shuffled.subList(i, Math.min(shuffled.size(), i + size)));
            for (Player player : team) {
                teamOf.put(player.getUniqueId(), teams.size());
            }
            teams.add(team);
        }
        teamCount = teams.size();
        return teams;
    }

    public static void reset() {
        teamOf.clear();
        teamCount = 0;
    }

    /** Номер команды или -1, если игрок не в команде (или режим не командный). */
    public static int teamOf(Entity entity) {
        return entity == null ? -1 : teamOf.getOrDefault(entity.getUniqueId(), -1);
    }

    /** Разные сущности одной команды. Себя самого сокомандником не считаем. */
    public static boolean areTeammates(Entity a, Entity b) {
        if (a == null || b == null || a.equals(b)) {
            return false;
        }
        int team = teamOf(a);
        return team >= 0 && team == teamOf(b);
    }

    public static ChatColor colorOf(int team) {
        return COLORS[team % COLORS.length];
    }

    /** Цвет ника игрока: цвет команды или null, если команд нет. */
    public static ChatColor colorOf(Player player) {
        int team = teamOf(player);
        return team < 0 ? null : colorOf(team);
    }

    /** Все игроки команды, включая выбывших (для победы). */
    public static List<UUID> membersOf(int team) {
        List<UUID> members = new ArrayList<>();
        teamOf.forEach((uuid, t) -> {
            if (t == team) {
                members.add(uuid);
            }
        });
        return members;
    }
}
