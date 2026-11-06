package com.v4nden.bows.Boosts.Types;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class FreezeBoost implements IBoostType {
    private static final double RADIUS = 15;
    private static final int DURATION_TICKS = 20 * 2;
    // иней на экране; меньше 140, иначе мороз начнёт наносить урон
    private static final int FROST_TICKS = 120;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.PRISMARINE_SHARD)
                .setRarity(3)
                .setId("freeze")
                .setPreviewRadius(RADIUS)
                .setName("Заморозка")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Замораживает на месте\n на 2 секунды всех игроков\n в радиусе 15 блоков.\n Эффект не действует\n на владельца буста")
                .setRunnable(consumer -> {
                    Location center = consumer.getLocation();
                    Set<UUID> frozen = new HashSet<>();
                    for (LivingEntity target : Game.targetsExcept(consumer)) {
                        if (!target.getWorld().equals(center.getWorld())
                                || target.getLocation().distance(center) >= RADIUS) {
                            continue;
                        }
                        frozen.add(target.getUniqueId());
                        target.setVelocity(target.getVelocity().zero());
                        target.setFreezeTicks(FROST_TICKS);
                        // для кукол (у них нет события движения) и чтобы не прыгали
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, DURATION_TICKS, 255,
                                false, false));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, DURATION_TICKS, 250,
                                false, false));
                        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 40,
                                .4, .8, .4, .02);
                    }

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(center, Sound.BLOCK_GLASS_BREAK, SoundCategory.MASTER, 1, .6f);
                        p.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.MASTER, 1, 1);
                    }
                    if (frozen.isEmpty()) {
                        return;
                    }

                    // игроков держим на месте, но они могут крутить головой
                    TemporaryListener<PlayerMoveEvent> moveListener = new TemporaryListener<PlayerMoveEvent>(
                            PlayerMoveEvent.class, EventPriority.HIGH, e -> {
                                if (!frozen.contains(e.getPlayer().getUniqueId()) || e.getTo() == null) {
                                    return;
                                }
                                Location from = e.getFrom();
                                if (from.getX() != e.getTo().getX() || from.getY() != e.getTo().getY()
                                        || from.getZ() != e.getTo().getZ()) {
                                    Location stay = from.clone();
                                    stay.setYaw(e.getTo().getYaw());
                                    stay.setPitch(e.getTo().getPitch());
                                    e.setTo(stay);
                                }
                            });

                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> moveListener.unregister(),
                            DURATION_TICKS);
                })
                .setDelay(1)
                .build(player);
    }
}
