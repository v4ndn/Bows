package com.v4nden.bows.Game;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import com.v4nden.bows.Bows;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Game implements Listener {
    public static boolean isRunning = false;
    public static Location gameLocation;
    public static long gameStart;
    public static ArrayList<Player> gamePlayers = new ArrayList<Player>();

    private static int taskActionTimer;
    private static int taskRenewArrowsTimer;
    private static int taskTabInfos;

    public static void startGame(Location location) {
        stopGame();
        isRunning = true;
        gameLocation = location;

        gameStart = System.currentTimeMillis();

        Random random = new Random();

        gamePlayers.addAll(Bukkit.getOnlinePlayers());
        gamePlayers.forEach((player) -> {
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.getInventory().setItem(0, new ItemStack(Material.BOW));
            player.getInventory().setItem(1, new ItemStack(Material.ARROW, 20));
            player.setPlayerListOrder(10);
            player.teleport(location.getWorld()
                    .getHighestBlockAt(
                            // location.add(random.nextInt(10, 30), random.nextInt(10, 30),
                            // random.nextInt(10, 30)))
                            location.add(0, 0, 0))
                    .getLocation().add(0, 1, 0));
        });
        taskTabInfos = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {

            gamePlayers.forEach((player) -> {
                player.setPlayerListName(player.getName() + " [" + (int) player.getHealth() + "] ["
                        + player.getLocation().getBlockY() + "]");
                player.setPlayerListOrder((int) Math.round(10 + (20 - player.getHealth())));

            });

            String center = Bukkit.getWorld("world").getWorldBorder().getCenter().getBlockX() + ", "
                    + Bukkit.getWorld("world").getWorldBorder().getCenter().getBlockZ();
            String message = "\n     Центр: " + center + "     \nРадиус: "
                    + (int) (Bukkit.getWorld("world").getWorldBorder().getSize() / 2) + "\n";

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setPlayerListFooter(message);

            }
        }, 0L, 10L);
        taskActionTimer = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
            long diff = System.currentTimeMillis() - gameStart;

            gamePlayers.forEach((player) -> {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("[" + Math.round(diff / 1000) + "]"));
            });
        }, 0L, 10L);
        taskRenewArrowsTimer = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {

            gamePlayers.forEach((player) -> {
                if (player.getInventory().getItem(1) == null) {
                    player.getInventory().setItem(1,
                            new ItemStack(Material.ARROW, 1));

                    return;
                }
                if (player.getInventory().getItem(1).getAmount() < 20) {
                    player.getInventory().setItem(1,
                            new ItemStack(Material.ARROW, player.getInventory().getItem(1).getAmount() + 1));
                }
            });
        }, 0L, 30L);

        Bukkit.getWorld("world").getWorldBorder().setSize(1400);
        Bukkit.getWorld("world").getWorldBorder().setCenter(location);
        Bukkit.getWorld("world").getWorldBorder().setSize(1, 15 * 60);
    }

    public static void stopGame() {
        Bukkit.getServer().getScheduler().cancelTask(taskActionTimer);
        Bukkit.getServer().getScheduler().cancelTask(taskRenewArrowsTimer);
        Bukkit.getServer().getScheduler().cancelTask(taskTabInfos);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setPlayerListOrder(1);
            p.setPlayerListName(p.getName());
            p.setGameMode(GameMode.ADVENTURE);
            p.teleport(new Location(Bukkit.getWorld("world"), 0, 100, 0));
            gamePlayers.remove(p);
            p.getInventory().clear();
        }
    }

    public static void eliminatePlayer(Player p) {

        gamePlayers.remove(p);
        p.setPlayerListName(p.getName());
        p.setPlayerListOrder(1);
        p.getInventory().clear();
        p.setGameMode(GameMode.SPECTATOR);

    }

    public static void checkForWinner() {

    }

    @EventHandler
    public void preventDamageFromOtherSources(EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Arrow)) {
            e.setCancelled(true);
        }
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (p.getHealth() - e.getDamage() <= 0) {
                e.setCancelled(true);
                eliminatePlayer(p);
                if (isRunning && gamePlayers.size() == 1) {
                    for (Player gp : Bukkit.getOnlinePlayers()) {
                        gp.sendMessage(ChatColor.of("#e97218") + "[🏹] " + ChatColor.of("#f4ebe5")
                                + gamePlayers.get(0).getName() + " победил!");
                    }
                    stopGame();

                }
            }
        }
    }

    @EventHandler
    public void preventBowAndArrowsClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null || e.getInventory() == null)
            return;

        if (e.getClick().equals(ClickType.NUMBER_KEY) && (e.getHotbarButton() == 0 || e.getHotbarButton() == 1)) {
            e.setCancelled(true);
            return;
        }
        if (e.getCurrentItem().getType().equals(Material.BOW) || e.getCurrentItem().getType().equals(Material.ARROW)
                || e.getCursor().getType().equals(Material.BOW) || e.getCursor().getType().equals(Material.ARROW)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventBowAndArrowsClick(PlayerDropItemEvent e) {

        if (e.getItemDrop().getItemStack().getType().equals(Material.BOW)
                || e.getItemDrop().getItemStack().getType().equals(Material.ARROW)) {
            e.setCancelled(true);
        }
    }

}
