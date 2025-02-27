package com.v4nden.bows.Boosts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.UseCooldownComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.v4nden.bows.Bows;
import com.v4nden.bows.BowsUtils;

import net.md_5.bungee.api.ChatColor;

public class Boost implements Listener {
    public Player owner;
    public Consumer<Player> runnable;

    private Material material;
    private String id;
    private String name;
    private String description;
    private int rarity;
    private float delay;

    private ItemStack item;

    private UUID uid;

    private boolean used = false;

    private int inventoryCheck;

    public Boost(Player owner, Material material, int rarity, String id, String name, String description,
            Consumer<Player> runnable, float delay) {
        this.runnable = runnable;
        this.owner = owner;

        this.material = material;
        this.id = id;
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.delay = delay;

        this.uid = UUID.randomUUID();
        this.item = createItem();

        Bows.instance.getServer().getPluginManager().registerEvents(this, Bows.instance);

    }

    public Boost(Player owner, Material material, int rarity, String id, String name, String description,
            Consumer<Player> runnable) {
        this.runnable = runnable;
        this.owner = owner;

        this.material = material;
        this.id = id;
        this.name = name;
        this.description = description;
        this.rarity = rarity;

        this.uid = UUID.randomUUID();
        this.item = createItem();

        Bows.instance.getServer().getPluginManager().registerEvents(this, Bows.instance);

    }

    public ItemStack getItem() {
        // inventoryCheck =
        // Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bows.instance, ()
        // -> {
        // if (this.used) {
        // Bukkit.getServer().getScheduler().cancelTask(inventoryCheck);
        // }
        // }, 120L, 120L);
        return this.item;
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
        lore.add(ChatColor.of("#dd9e6e") + "bows:" + this.id);
        lore.add(ChatColor.of("#414141") + this.uid.toString());

        meta.setLore(lore);
        meta.setMaxStackSize(1);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    @EventHandler
    public void onActivate(PlayerSwapHandItemsEvent e) {

        if (!e.getPlayer().equals(owner))
            return;
        if (this.used) {
            System.out.println(createLogMessage("UNREGISTER CUZ USED"));
            e.getHandlers().unregister(this);
        }

        System.out.println(createLogMessage("swap event"));
        if (!e.getPlayer().getInventory().contains(this.item)) {
            e.getHandlers().unregister(this);
            return;
        }
        if (owner.getCooldown(e.getMainHandItem()) > 0) {
            e.setCancelled(true);
            return;
        }
        if (owner.getCooldown(this.getItem()) > 0) {
            e.setCancelled(true);
            return;
        }
        if (compareUids(this.item, e.getMainHandItem())) {
            e.setCancelled(true);
            return;
        }
        if (compareUids(this.item, e.getOffHandItem())) {
            System.out.println(createLogMessage("used"));

            List<String> rarityColors = new ArrayList<String>();
            rarityColors.add("#fafafa");
            rarityColors.add("#4aff65");
            rarityColors.add("#4a89ff");
            rarityColors.add("#834aff");
            rarityColors.add("#e97218");

            if (this.delay != 0) {
                System.out.println(createLogMessage("has delay"));
                owner.setCooldown(this.item, Math.round(this.delay * 20));

                for (float i = 0; i <= this.delay; i += 0.25f) {
                    final float fi = Float.valueOf(i);
                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        System.out.println(fi);

                        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 10,
                                Double.valueOf(2.0 * (fi / (this.delay))).floatValue());
                    }, Math.round(i * 20));
                }
                Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                    owner.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                    e.getHandlers().unregister(this);
                    this.used = true;
                    runnable.accept(owner);
                    BowsUtils.broadcastSystemMessage(Bukkit.getOnlinePlayers(),
                            owner.getName()
                                    + " использует "
                                    + name
                                    + ChatColor.of(rarityColors.get(rarity - 1))
                                    + " [" + "★".repeat(rarity) + "]");
                }, Math.round(this.delay * 20));
                return;
            } else {
                this.used = true;
                e.setOffHandItem(new ItemStack(Material.AIR));
                e.getHandlers().unregister(this);
                runnable.accept(owner);
                BowsUtils.broadcastSystemMessage(Bukkit.getOnlinePlayers(),
                        owner.getName()
                                + " использует "
                                + name
                                + ChatColor.of(rarityColors.get(rarity - 1))
                                + " [" + "★".repeat(rarity) + "]");
            }

        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (this.used) {
            e.getHandlers().unregister(this);
        }
        if (e.getItemDrop().getItemStack().equals(this.item)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (this.used) {
            e.getHandlers().unregister(this);
        }
        if (compareUids(e.getItem(), this.item)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(InventoryClickEvent e) {
        if (this.used) {
            e.getHandlers().unregister(this);
        }
        if (e.getClickedInventory() == null || e.getInventory() == null) {
            e.setCancelled(true);
            return;
        }
        if (compareUids(e.getCurrentItem(), this.item) || compareUids(e.getCursor(), this.item)) {
            if (e.getSlot() == 40) {
                e.setCancelled(true);
                return;
            } else if (!e.getInventory().getType().equals(InventoryType.CRAFTING)) {
                e.setCancelled(true);
                return;
            } else {
                e.setCancelled(false);
                return;
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(InventoryDragEvent e) {
        if (this.used) {
            e.getHandlers().unregister(this);
        }
        if (compareUids(e.getOldCursor(), this.item)) {
            if (e.getInventorySlots().contains(40)) {
                e.setCancelled(true);
            }
        }

    }

    private String createLogMessage(String message) {

        return "\033[0;33m" + "[" + this.owner.getName() + "] " + "\033[0;34m" + "[" + this.id + "] " + "\033[0;31m"
                + "[" + this.uid.toString()
                + "] " + "\033[0;37m" + message;
    }

    private static boolean compareUids(ItemStack fromitem, ItemStack toitem) {
        if (fromitem == null || toitem == null)
            return false;
        if (fromitem.getItemMeta() == null || toitem.getItemMeta() == null)
            return false;
        if (fromitem.getItemMeta().getLore() == null || toitem.getItemMeta().getLore() == null)
            return false;
        return fromitem.getItemMeta().getLore().getLast().equals(toitem.getItemMeta().getLore().getLast());
    }

}
