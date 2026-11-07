package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Stats;
import com.v4nden.bows.Game.Teams;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class VampirismBoost implements IBoostType {
    private static final long DURATION_TICKS = 20 * 15L;
    private static final double LIFESTEAL = 0.5;
    private static final Particle.DustOptions BLOOD = new Particle.DustOptions(Color.fromRGB(150, 10, 20), 1.2f);

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.NETHER_BRICK)
                .setRarity(3)
                .setId("vampirism")
                .setName("Вампиризм")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n На 15 секунд каждое\n попадание по противнику\n восстанавливает владельцу\n половину нанесённого урона")
                .setRunnable(consumer -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_WITCH_DRINK,
                                SoundCategory.MASTER, 1,
                                .7f);
                    }

                    // LOWEST: смертельный урон игра потом отменяет (выбывание вместо смерти),
                    // на MONITOR последнее попадание бы не засчиталось. Поэтому повторяем
                    // фильтр урона из Game.preventDamageFromOtherSources здесь.
                    TemporaryListener<EntityDamageByEntityEvent> hitListener = new TemporaryListener<EntityDamageByEntityEvent>(
                            EntityDamageByEntityEvent.class, EventPriority.LOWEST, e -> {
                                if (e.isCancelled() || !(e.getEntity() instanceof LivingEntity victim)
                                        || victim.equals(consumer) || !consumer.isOnline()) {
                                    return;
                                }
                                if (!(e.getDamager() instanceof Arrow) && !(e.getDamager() instanceof TNTPrimed)
                                        && !(e.getDamager() instanceof LightningStrike)) {
                                    return;
                                }
                                if (!consumer.equals(Stats.findAttacker(e)) || Teams.areTeammates(consumer, victim)) {
                                    return;
                                }
                                double dealt = Math.min(e.getFinalDamage(), victim.getHealth());
                                double maxHealth = consumer.getAttribute(Attribute.MAX_HEALTH).getValue();
                                consumer.setHealth(Math.min(maxHealth, consumer.getHealth() + dealt * LIFESTEAL));
                                consumer.getWorld().spawnParticle(Particle.HEART, consumer.getLocation().add(0, 2, 0),
                                        3, .3, .2, .3);
                            });

                    // пока буст активен, на игроке раз в секунду видна кровь
                    int particleTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
                        if (consumer.isOnline()) {
                            consumer.getWorld().spawnParticle(Particle.DUST, consumer.getLocation().add(0, 1, 0), 20,
                                    .4, .6, .4, BLOOD);
                        }
                    }, 0L, 20L);

                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        Bukkit.getScheduler().cancelTask(particleTask);
                        hitListener.unregister();
                    }, DURATION_TICKS);
                })
                .setDelay(1)
                .build(player);
    }
}
