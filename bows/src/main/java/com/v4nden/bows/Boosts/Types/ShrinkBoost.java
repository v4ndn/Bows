package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.PlayerSize;

import net.md_5.bungee.api.ChatColor;

public class ShrinkBoost implements IBoostType {
    private static final double SCALE = 0.5;
    private static final long DURATION_TICKS = 20 * 15L;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.RABBIT_FOOT).setRarity(2).setId("shrink").setName("Уменьшение")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Уменьшает владельца буста\n вдвое на 15 секунд")
                .setRunnable(consumer -> {
                    // от базового игрового размера: с «Малышом» это 0.85 * 0.5
                    int version = PlayerSize.setTemporary(consumer, SCALE);

                    consumer.getWorld().spawnParticle(Particle.POOF, consumer.getLocation().add(0, .5, 0), 30,
                            .3, .5, .3, .02);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_OUT,
                                SoundCategory.MASTER, 1,
                                1.5f);
                    }

                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance,
                            () -> PlayerSize.clearTemporary(consumer, version), DURATION_TICKS);
                }).setDelay(1).build(player);
    }

}
