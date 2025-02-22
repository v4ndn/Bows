package com.v4nden.bows.Boosts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.v4nden.bows.Bows;

import net.md_5.bungee.api.ChatColor;

public class Boost implements Listener {
    public Player owner;
    public Consumer<Player> runnable;

    private Material material;
    private String id;
    private String name;
    private String description;
    private int rarity;

    private ItemStack item;

    public Boost(Player owner, Material material, int rarity, String id, String name, String description,
            Consumer<Player> runnable) {
        this.runnable = runnable;
        this.owner = owner;

        this.material = material;
        this.id = id;
        this.name = name;
        this.description = description;
        this.rarity = rarity;

        item = createItem();

        Bows.instance.getServer().getPluginManager().registerEvents(this, Bows.instance);

    }

    public ItemStack getItem() {
        return item;
    }

    public ItemStack createItem() {
        List<String> rarityColors = new ArrayList<String>();
        rarityColors.add("#fafafa");
        rarityColors.add("#4aff65");
        rarityColors.add("#4a89ff");
        rarityColors.add("#834aff");
        rarityColors.add("#e97218");

        // ------------
        ItemStack itemStack = new ItemStack(material, 1);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(ChatColor.of("#e97218") + "[🏹] " + ChatColor.of("#f4ebe5") + name
                + ChatColor.of(rarityColors.get(rarity - 1))
                + " [" + "★".repeat(rarity) + "]");

        ArrayList<String> lore = new ArrayList<>();
        for (String line : description.split("\n")) {
            lore.add(ChatColor.of("#e6b794") + line);
        }

        lore.add("\n");
        lore.add(ChatColor.of("#dd9e6e") + "bows:" + id);

        meta.setLore(lore);
        meta.setMaxStackSize(1);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    @EventHandler
    public void onActivate(PlayerSwapHandItemsEvent e) {
        if (!e.getPlayer().equals(owner))
            return;
        if (!e.getPlayer().getInventory().contains(this.item)) {
            e.getHandlers().unregister(this);
            return;
        }
        Bows.instance.getLogger().info("Log from " + id);
        if (e.getOffHandItem().equals(item)) {
            Bows.instance.getLogger().info("Boost(" + id + ") used by " + e.getPlayer().getName());

            List<String> rarityColors = new ArrayList<String>();
            rarityColors.add("#fafafa");
            rarityColors.add("#4aff65");
            rarityColors.add("#4a89ff");
            rarityColors.add("#834aff");
            rarityColors.add("#e97218");

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(
                        "" + ChatColor.of("#e97218") + "[🏹] " + ChatColor.of("#f4ebe5") + p.getName() + " использует "
                                + name
                                + ChatColor.of(rarityColors.get(rarity - 1))
                                + " [" + "★".repeat(rarity) + "]");
            }
            runnable.accept(owner);
            e.getOffHandItem().setType(Material.AIR);
            e.getHandlers().unregister(this);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (e.getItemDrop().getItemStack().equals(item)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(InventoryClickEvent e) {
        if (e.getClickedInventory() == null || e.getInventory() == null)
            return;
        if (e.getCurrentItem().equals(item) || e.getCursor().equals(item)) {
            if (!e.getInventory().getType().equals(InventoryType.CRAFTING)) {
                e.setCancelled(true);
                return;
            } else {
                e.setCancelled(false);
                return;
            }
        }

    }
}
