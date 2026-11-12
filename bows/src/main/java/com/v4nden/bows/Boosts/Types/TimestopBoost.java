package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Game.Teams;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class TimestopBoost implements IBoostType {
    // кто остановил время (пока оно стоит): его и сокомандников таймстоп не замораживает
    private static Player owner;

    /** Заморожен ли игрок остановленным временем (для эффекта экрана ScreenEffect). */
    public static boolean isFrozen(Player player) {
        return owner != null && Bukkit.getServerTickManager().isFrozen() && !player.equals(owner)
                && !Teams.areTeammates(owner, player);
    }

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.NETHER_STAR)
                .setRarity(5)
                .setId("timestop")
                .setIcon("timestop")
                .setName("Таймстоп")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Останавливает время для всех игроков\n на 10 секунд. В остановленном времени\n стрелы, заряды и остальные факторы\n игровой физики будут заморожены.\n Все игроки кроме вас не смогут\n двигаться. При использовании \n таймстопа в уже остановленном\n времени буст безвозвратно пропадёт")
                .setRunnable(consumer -> {
                    if (Bukkit.getServerTickManager().isFrozen()) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH,
                                    SoundCategory.MASTER,
                                    100,
                                    1);
                        }
                        return;
                    }

                    long now = System.currentTimeMillis();
                    Bukkit.getServerTickManager().setFrozen(true);
                    owner = consumer;

                    Game.gamePlayers.forEach((gamePlayer) -> {
                        gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
                        if (!gamePlayer.equals(consumer) && !Teams.areTeammates(consumer, gamePlayer)) {
                            // ночное зрение — сигнал ресурспаку: у замороженных мир без красок
                            // (shaders/include/bows_timestop.glsl), у владельца — обычный
                            gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 10, 1,
                                    false, false, false));
                            gamePlayer.setAllowFlight(true);
                        }
                    });

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE,
                                SoundCategory.MASTER,
                                100,
                                1);
                    }

                    for (int i = 0; i <= 200; i += 40) {
                        Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.playSound(p.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON,
                                        SoundCategory.MASTER,
                                        10,
                                        1);
                            }
                        }, i);
                    }
                    for (int i = 20; i <= 200; i += 40) {
                        Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.playSound(p.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF,
                                        SoundCategory.MASTER,
                                        10,
                                        1);
                            }
                        }, i);
                    }

                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        Game.gamePlayers.forEach((gamePlayer) -> {
                            gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
                            if (!gamePlayer.equals(consumer) && !Teams.areTeammates(consumer, gamePlayer)) {
                                gamePlayer.setAllowFlight(false);
                            }
                        });
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,
                                    SoundCategory.MASTER,
                                    100,
                                    1);
                        }
                        Bukkit.getServerTickManager().setFrozen(false);
                        owner = null;
                    }, 200L);

                    new TemporaryListener<>(PlayerMoveEvent.class, EventPriority.NORMAL, e -> {
                        PlayerMoveEvent event = (PlayerMoveEvent) e;
                        if (System.currentTimeMillis() - now < 10000) {
                            if (event.getPlayer().equals(consumer) || !Game.gamePlayers.contains(event.getPlayer())
                                    || Teams.areTeammates(consumer, event.getPlayer())) {
                                event.setCancelled(false);
                            } else {
                                event.setCancelled(true);
                            }
                            return false;
                        } else {
                            return true;
                        }
                    });
                })
                .setDelay(5)
                .build(player);
    }
}
