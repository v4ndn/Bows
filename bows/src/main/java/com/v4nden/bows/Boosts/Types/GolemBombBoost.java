package com.v4nden.bows.Boosts.Types;

import java.util.Comparator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class GolemBombBoost implements IBoostType {
    private static final double TRIGGER_RADIUS = 3;
    private static final double SCALE = .5;
    private static final double MOVEMENT_SPEED = .35; // у обычного голема .25
    private static final int LIFETIME_TICKS = 20 * 15;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.GUNPOWDER)
                .setRarity(3)
                .setId("golembomb")
                .setName("Голем-камикадзе")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Выпускает маленького\n железного голема, который\n бежит к ближайшему игроку\n и взрывается,\n когда оказывается в 3\n блоках от него. Через 15\n секунд взрывается сама")
                .setRunnable(consumer -> {
                    IronGolem golem = consumer.getWorld().spawn(consumer.getLocation(), IronGolem.class, s -> {
                        s.setPersistent(false);
                        s.setCustomName(ChatColor.of("#e97218") + "💣");
                        s.setCustomNameVisible(true);
                        s.getAttribute(Attribute.SCALE).setBaseValue(SCALE);
                        s.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(MOVEMENT_SPEED);
                    });
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.MASTER, 1, 1.5f);
                    }

                    // владельца не трогает, даже если тот его ударил
                    TemporaryListener<EntityTargetEvent> targetListener = new TemporaryListener<EntityTargetEvent>(
                            EntityTargetEvent.class, EventPriority.HIGH, e -> {
                                if (e.getEntity().equals(golem) && consumer.equals(e.getTarget())) {
                                    e.setCancelled(true);
                                }
                            });

                    new BukkitRunnable() {
                        int ticks = 0;

                        @Override
                        public void run() {
                            ticks++;
                            LivingEntity target = Game.targetsExcept(consumer).stream()
                                    .filter(t -> t.isValid() && t.getWorld().equals(golem.getWorld()))
                                    .min(Comparator.comparingDouble(
                                            t -> t.getLocation().distanceSquared(golem.getLocation())))
                                    .orElse(null);

                            boolean reached = target != null
                                    && target.getLocation().distance(golem.getLocation()) <= TRIGGER_RADIUS;
                            if (!golem.isValid() || ticks > LIFETIME_TICKS || reached) {
                                cancel();
                                targetListener.unregister();
                                if (golem.isValid()) {
                                    explode(golem, consumer);
                                }
                                return;
                            }
                            if (target != null && !target.equals(golem.getTarget())) {
                                golem.setTarget(target);
                            }
                            if (ticks % 10 == 0) {
                                golem.getWorld().spawnParticle(Particle.SMOKE,
                                        golem.getLocation().add(0, .5, 0), 3, .1, .1, .1, .01);
                            }
                        }
                    }.runTaskTimer(Bows.instance, 1L, 1L);
                })
                .setDelay(1)
                .build(player);
    }

    private static void explode(IronGolem golem, Player owner) {
        // взрыв через TNT: урон от TNT проходит фильтр игры и засчитывается владельцу
        TNTPrimed tnt = golem.getWorld().spawn(golem.getLocation(), TNTPrimed.class);
        tnt.setSource(owner);
        tnt.setFuseTicks(0);
        golem.remove();
    }
}
