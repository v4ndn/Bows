package com.v4nden.bows.Boosts;

import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class BoostGenerator {

    private Material material;
    private int rarity;
    private String id;
    private String name;
    private String description;
    private float delay;
    private Consumer<Player> runnable;

    public BoostGenerator() {

    }

    public BoostGenerator setMaterial(Material material) {
        this.material = material;
        return this;
    }

    public BoostGenerator setRarity(int rarity) {
        this.rarity = rarity;
        return this;
    }

    public BoostGenerator setId(String id) {
        this.id = id;
        return this;
    }

    public BoostGenerator setName(String name) {
        this.name = name;
        return this;
    }

    public BoostGenerator setDescription(String description) {
        this.description = description;
        return this;
    }

    public BoostGenerator setDelay(float delay) {
        this.delay = delay;
        return this;
    }

    public BoostGenerator setRunnable(Consumer<Player> runnable) {
        this.runnable = runnable;
        return this;
    }

    public Boost build(Player owner) {
        this.description += "\n\n";
        this.description += ChatColor.of("#939393") + "⌚: "
                + String.valueOf(this.delay > 0 ? this.delay : this.delay * 1000)
                + (delay > 0 ? "s" : "ms");
        this.description += "\n";

        return new Boost(owner, this.material, this.rarity, this.id, this.name, this.description,
                this.runnable, this.delay);
    }
}
