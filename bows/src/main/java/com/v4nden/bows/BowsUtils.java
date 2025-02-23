package com.v4nden.bows;

import java.util.Collection;
import java.util.List;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class BowsUtils {
    public static void broadcastSystemMessage(Collection<? extends Player> players, String message) {
        for (Player p : players) {
            p.sendMessage(ChatColor.of("#e97218") + "[🏹] " + ChatColor.of("#f4ebe5") + message);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 100, 2);
        }
    }

    public static void broadcastSystemMessage(List<Player> players, String message) {
        for (Player p : players) {
            p.sendMessage(ChatColor.of("#e97218") + "[🏹] " + ChatColor.of("#f4ebe5") + message);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 100, 2);
        }
    }

    public static void broadcastSystemMessage(Player p, String message) {

        p.sendMessage(ChatColor.of("#e97218") + "[🏹] " + ChatColor.of("#f4ebe5") + message);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 100, 2);

    }

}
