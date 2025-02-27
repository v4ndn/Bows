package com.v4nden.bows;

import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.v4nden.bows.Utils.ArcPointsGenerator;

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

    public static Snowball lauchCharge(Player p, Material material) {

        Snowball snowball = p.getWorld().spawn(p.getLocation().add(new Vector(0, 1, 0)),
                Snowball.class);
        snowball.setVelocity(p.getLocation().getDirection().multiply(1.5));
        snowball.setItem(new ItemStack(material));
        return snowball;
    }

    public static void spawnParticleCircle(Particle particle, Location location, double radius) {
        List<double[]> points = ArcPointsGenerator.getArcPoints(radius, 0, 360, 10);

        points.forEach(point -> {
            System.out.println(point[0] + " " + point[1]);
            location.getWorld().spawnParticle(particle, location.add(point[0], 0, point[1]), 1, 0, 0, 0, .01f);
        });

    }

}
