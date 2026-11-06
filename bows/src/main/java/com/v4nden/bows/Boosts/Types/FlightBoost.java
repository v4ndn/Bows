package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class FlightBoost implements IBoostType {
    private static final double LAUNCH_VELOCITY = 2.5; // ~30 блоков вверх
    private static final int APEX_TIMEOUT_TICKS = 40;
    private static final int FLIGHT_TICKS = 20 * 10;
    private static final int CHESTPLATE_SLOT = 38;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.BREEZE_ROD)
                .setRarity(3)
                .setId("flight")
                .setName("Полёт")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Запускает владельца вверх\n на ~30 блоков и выдаёт\n элитры на 10 секунд")
                .setRunnable(consumer -> {
                    consumer.setVelocity(new Vector(0, LAUNCH_VELOCITY, 0));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST,
                                SoundCategory.MASTER, 1,
                                1);
                    }

                    // скорость игрока на сервере ненадёжна, вершину ловим по высоте
                    new BukkitRunnable() {
                        int ticks = 0;
                        double lastY = consumer.getLocation().getY();

                        @Override
                        public void run() {
                            ticks++;
                            double y = consumer.getLocation().getY();
                            if (!consumer.isOnline()) {
                                cancel();
                                return;
                            }
                            if ((ticks > 3 && y <= lastY) || ticks > APEX_TIMEOUT_TICKS) {
                                cancel();
                                startFlight(consumer);
                            }
                            lastY = y;
                        }
                    }.runTaskTimer(Bows.instance, 1L, 1L);
                })
                .setDelay(1)
                .build(player);
    }

    private static void startFlight(Player consumer) {
        // элитры выдаются на время полёта, прежний нагрудник возвращается после
        ItemStack previousChestplate = consumer.getInventory().getChestplate();
        consumer.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        consumer.setGliding(true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(consumer.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA,
                    SoundCategory.MASTER, 1,
                    1);
        }

        TemporaryListener<InventoryClickEvent> armorListener = new TemporaryListener<InventoryClickEvent>(
                InventoryClickEvent.class, EventPriority.HIGHEST, e -> {
                    if (e.getWhoClicked().equals(consumer) && e.getSlot() == CHESTPLATE_SLOT) {
                        e.setCancelled(true);
                    }
                });

        Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
            armorListener.unregister();
            if (consumer.isOnline()) {
                consumer.setGliding(false);
                consumer.getInventory().setChestplate(previousChestplate);
            }
        }, FLIGHT_TICKS);
    }
}
