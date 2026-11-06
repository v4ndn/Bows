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
    private String icon;
    private boolean passive;
    private double previewRadius;
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

    // радиус вокруг владельца: пока буст в руке, цели в нём подсвечиваются (RadiusHighlight)
    public BoostGenerator setPreviewRadius(double radius) {
        this.previewRadius = radius;
        return this;
    }

    // пассивный: действует, пока лежит у игрока; через F не активируется
    public BoostGenerator setPassive(boolean passive) {
        this.passive = passive;
        return this;
    }

    // модель из ресурспака: bows:<icon>
    public BoostGenerator setIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public BoostGenerator setRunnable(Consumer<Player> runnable) {
        this.runnable = runnable;
        return this;
    }

    public Boost build(Player owner) {
        this.description += "\n\n";
        this.description += ChatColor.of("#939393") + "⌚: "
                + (this.passive ? "Пассивный"
                        : this.delay == 0 ? "Моментально"
                                : this.delay < 1 ? Math.round(this.delay * 1000) + "ms" : this.delay + "s");
        this.description += "\n";

        Boost boost = new Boost(owner, this.material, this.rarity, this.id, this.name, this.description,
                this.runnable, this.delay, this.icon);
        boost.setPassive(this.passive);
        boost.setPreviewRadius(this.previewRadius);
        return boost;
    }
}
