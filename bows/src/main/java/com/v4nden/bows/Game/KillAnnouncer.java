package com.v4nden.bows.Game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import com.v4nden.bows.BowsUtils;

import net.md_5.bungee.api.ChatColor;

/**
 * Объявления убийств как в доте: первая кровь и 2/3/4/5+ убийств одного игрока
 * за игру. Звуки — bows:announcer.* из ресурспака.
 */
public class KillAnnouncer {
    private static final Map<UUID, Integer> killsThisGame = new HashMap<>();
    private static boolean firstBloodTaken = false;

    public static void reset() {
        killsThisGame.clear();
        firstBloodTaken = false;
    }

    public static void onKill(Player killer, String victimName) {
        int kills = killsThisGame.merge(killer.getUniqueId(), 1, Integer::sum);

        String accent = ChatColor.of("#e97218").toString();
        String reset = ChatColor.RESET.toString();
        String killerName = accent + killer.getName() + reset;
        String victim = accent + victimName + reset;

        if (!firstBloodTaken) {
            firstBloodTaken = true;
            announce("announcer.first_blood", killerName + " пролил первую кровь, убив " + victim + "!");
        }

        switch (kills) {
            case 2 -> announce("announcer.double_kill", killerName + " — двойное убийство!");
            case 3 -> announce("announcer.triple_kill", killerName + " — тройное убийство!");
            case 4 -> announce("announcer.ultra_kill", killerName + " — ультра-убийство!");
            default -> {
                // с пятого убийства — буйство на каждое следующее
                if (kills >= 5) {
                    announce("announcer.rampage", killerName + " — " + accent + "БУЙСТВО!" + reset
                            + (kills > 5 ? " (" + kills + " убийств)" : ""));
                }
            }
        }
    }

    private static void announce(String sound, String message) {
        BowsUtils.broadcastSystemMessage(Bukkit.getOnlinePlayers(), message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), "bows:" + sound, SoundCategory.MASTER, 1, 1);
        }
    }
}
