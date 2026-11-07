package com.v4nden.bows.Game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.v4nden.bows.Bows;
import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Boosts.BoostTypes;
import com.v4nden.bows.Boosts.StarterBoosts;
import com.v4nden.bows.Boosts.Types.ElusiveBoost;
import com.v4nden.bows.Boosts.Types.GiantBoost;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Game implements Listener {
    public static boolean isRunning = false;
    // тестовая игра: участвуют только выбранные игроки, смерть не выбивает из игры
    public static boolean isTest = false;
    public static Location gameLocation;
    public static long gameStart;
    // участников на старте игры (для бустов, зависящих от размера лобби)
    public static int startPlayerCount = 0;
    public static ArrayList<Player> gamePlayers = new ArrayList<Player>();

    private static int taskActionTimer;
    private static int taskRenewArrowsTimer;
    private static int taskTabInfos;
    private static int taskBoosts;

    // все, на кого действуют бусты: участники и куклы тестовой игры
    public static List<LivingEntity> targets() {
        List<LivingEntity> targets = new ArrayList<>(gamePlayers);
        targets.addAll(Dummies.alive());
        return targets;
    }

    // цели буста владельца: все, кроме его сокомандников (сам владелец остаётся)
    public static List<LivingEntity> targetsFor(Player owner) {
        List<LivingEntity> targets = targets();
        targets.removeIf(target -> Teams.areTeammates(owner, target));
        return targets;
    }

    // цели кроме владельца буста и его сокомандников
    public static List<LivingEntity> targetsExcept(Player owner) {
        List<LivingEntity> targets = targetsFor(owner);
        targets.remove(owner);
        return targets;
    }

    // цели для бустов, которые выбирают одну случайную или ближайшую цель:
    // владельцы «Неуловимого» в выбор не попадают
    public static List<LivingEntity> selectableTargetsExcept(Player owner) {
        List<LivingEntity> targets = targetsExcept(owner);
        targets.removeIf(target -> target instanceof Player player && ElusiveBoost.has(player));
        return targets;
    }

    public static void startGame(Location location) {
        startGame(location, Bukkit.getOnlinePlayers(), false);
    }

    public static void startGame(Location location, Collection<? extends Player> players, boolean test) {
        startGame(location, players, test, Map.of());
    }

    /** spawns — заранее назначенные (и сгенерированные) точки появления участников. */
    public static void startGame(Location location, Collection<? extends Player> players, boolean test,
            Map<UUID, Location> spawns) {
        if (isRunning)
            stopGame();
        isRunning = true;
        isTest = test;
        gameLocation = location;
        gamePlayers.clear();
        KillAnnouncer.reset();
        GiantBoost.reset();
        StarterChoiceDialog.reset();

        gameStart = System.currentTimeMillis();

        Random random = new Random();

        gamePlayers.addAll(players);
        startPlayerCount = gamePlayers.size();
        gamePlayers.forEach((player) -> {
            resetPlayerState(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.getInventory().setItem(0, new ItemStack(Material.BOW));
            player.getInventory().setItem(1, new ItemStack(Material.ARROW, 20));
            // стартовый буст игрок выбирает сам в окне (или играет без него)
            StarterChoiceDialog.show(player);
            // своя точка, если назначена; clone: add() менял бы саму точку центра
            Location spawn = spawns.getOrDefault(player.getUniqueId(),
                    location.clone().add(random.nextInt(-100, 100), 0, random.nextInt(-100, 100)));
            player.teleport(location.getWorld().getHighestBlockAt(spawn).getLocation().add(.5, 1, .5));
        });
        // кто не попал в участники (зашёл во время подготовки) — наблюдатель, как вошедшие после старта
        if (!test) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!gamePlayers.contains(p)) {
                    p.getInventory().clear();
                    p.setGameMode(GameMode.SPECTATOR);
                    p.teleport(location.getWorld().getHighestBlockAt(location).getLocation().add(.5, 2, .5));
                }
            }
        }
        taskTabInfos = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {

            String center = Bukkit.getWorld("world").getWorldBorder().getCenter().getBlockX() + ", "
                    + Bukkit.getWorld("world").getWorldBorder().getCenter().getBlockZ();
            String message = "\n     Центр: " + center + "     \nРадиус: "
                    + (int) (Bukkit.getWorld("world").getWorldBorder().getSize() / 2) + "\n";

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setPlayerListFooter(message);

            }
        }, 0L, 20L);
        taskActionTimer = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
            long diff = System.currentTimeMillis() - gameStart;

            gamePlayers.forEach((player) -> {

                double xVectorToCenter = player.getLocation().getX() - Game.gameLocation.getX();
                double zVectorToCenter = player.getLocation().getZ() - Game.gameLocation.getZ();

                Vector vectorToCenter = new Vector(xVectorToCenter, 0, zVectorToCenter).normalize();
                Vector directionVector = player.getLocation().getDirection().normalize();

                Double indicator = vectorToCenter.distance(directionVector) / 2;

                player.setExp(indicator.floatValue());

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(
                                "[" + Math.round(diff / 1000) + "]"));
            });

        }, 0L, 2L);
        taskBoosts = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {

            List<String> boostsList = new ArrayList<>();

            for (BoostTypes type : BoostTypes.values()) {
                boostsList.add(type.name());
            }

            BowsUtils.broadcastSystemMessage(Bukkit.getOnlinePlayers(), "Буст!");

            gamePlayers.forEach((player) -> {
                int rnd = new Random().nextInt(boostsList.size());
                player.getInventory()
                        .addItem(BoostTypes.valueOf(boostsList.get(rnd)).getBoost(player).createItem());
            });

        }, 0L, 20 * 30L);
        taskRenewArrowsTimer = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {

            gamePlayers.forEach((player) -> {
                if (player.getInventory().getItem(1) == null) {
                    player.getInventory().setItem(1,
                            new ItemStack(Material.ARROW, 1));

                    return;
                }
                if (player.getInventory().getItem(1).getAmount() < StarterBoosts.maxArrows(player)) {
                    player.getInventory().setItem(1,
                            new ItemStack(Material.ARROW, player.getInventory().getItem(1).getAmount() + 1));
                }
            });
        }, 0L, 60L);

        Bukkit.getWorld("world").getWorldBorder().setSize(1400);
        Bukkit.getWorld("world").getWorldBorder().setCenter(location);
        Bukkit.getWorld("world").getWorldBorder().setSize(5, 7 * 60);
    }

    public static void stopGame() {
        // в тестовой игре не трогаем игроков, которые в ней не участвовали
        Collection<? extends Player> playersToReset = isTest ? new ArrayList<>(gamePlayers)
                : Bukkit.getOnlinePlayers();
        isRunning = false;
        isTest = false;
        Bukkit.getServer().getScheduler().cancelTask(taskActionTimer);
        Bukkit.getServer().getScheduler().cancelTask(taskRenewArrowsTimer);
        Bukkit.getServer().getScheduler().cancelTask(taskTabInfos);
        Bukkit.getServer().getScheduler().cancelTask(taskBoosts);
        for (Player p : playersToReset) {
            p.setGameMode(GameMode.ADVENTURE);
            p.teleport(new Location(Bukkit.getWorld("world"), 0, 100, 0));
            gamePlayers.remove(p);
            p.getInventory().clear();
            p.setPlayerListFooter("");
            resetPlayerState(p);
        }
        gamePlayers.clear();
        Dummies.clear();
        GiantBoost.reset();
        Teams.reset();
        Bukkit.getWorld("world").getWorldBorder().setCenter(0, 0);
        Bukkit.getWorld("world").getWorldBorder().setSize(500);
    }

    // всё, что игра и бусты меняют на игроке, — обратно к ванильному
    private static void resetPlayerState(Player p) {
        StarterBoosts.resetPassive(p); // размер (PlayerSize), спешка, доп. здоровье
        new ArrayList<>(p.getActivePotionEffects()).forEach(effect -> p.removePotionEffect(effect.getType()));
        p.setFireTicks(0);
        p.setFreezeTicks(0);
        p.setFallDistance(0);
        p.setGliding(false);
        if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
            p.setAllowFlight(false);
        }
        p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
        p.setFoodLevel(20);
    }

    public static void eliminatePlayer(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 10, 1);
        BowsUtils.broadcastSystemMessage(Bukkit.getOnlinePlayers(), p.getName() + " убит!");
        gamePlayers.remove(p);
        p.getInventory().clear();
        StarterBoosts.resetPassive(p);
        p.setGameMode(GameMode.SPECTATOR);

    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent e) {
        e.getPlayer().getInventory().clear();
        if (isRunning && !isTest) {
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
            e.getPlayer()
                    .teleport(Bukkit.getWorld("world")
                            .getHighestBlockAt(Bukkit.getWorld("world").getWorldBorder().getCenter()).getLocation()
                            .add(0, 2, 0));
        } else {
            e.getPlayer().teleport(new Location(Bukkit.getWorld("world"), 0, 100, 0));
            e.getPlayer().setGameMode(GameMode.ADVENTURE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventDamageFromOtherSources(EntityDamageByEntityEvent e) {
        // у взрыва TNT с источником (setSource) getDamager() — поджёгший игрок,
        // поэтому смотрим и на прямую сущность урона, и на причину
        Entity direct = e.getDamageSource().getDirectEntity();
        boolean allowed = isAllowedDamager(e.getDamager()) || isAllowedDamager(direct)
                || e.getCause() == DamageCause.ENTITY_EXPLOSION || e.getCause() == DamageCause.BLOCK_EXPLOSION
                || e.getCause() == DamageCause.LIGHTNING;
        if (!allowed) {
            e.setCancelled(true);
        }
    }

    private static final double EXPLOSION_DAMAGE_MULTIPLIER = .5;

    // взрывы по игрокам — половина урона. LOWEST: раньше проверки на смертельный
    // урон, иначе игрок выбывал бы от урона, который после уменьшения не убил бы
    @EventHandler(priority = EventPriority.LOWEST)
    public void halveExplosionDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && (e.getCause() == DamageCause.ENTITY_EXPLOSION
                || e.getCause() == DamageCause.BLOCK_EXPLOSION)) {
            e.setDamage(e.getDamage() * EXPLOSION_DAMAGE_MULTIPLIER);
        }
    }

    private static boolean isAllowedDamager(Entity entity) {
        return entity instanceof Arrow || entity instanceof TNTPrimed || entity instanceof LightningStrike;
    }

    @EventHandler
    public void handlePlayerLeave(PlayerQuitEvent e) {

        if (!isRunning)
            return;

        if (isTest) {
            if (gamePlayers.contains(e.getPlayer()))
                stopGame();
            return;
        }

        eliminatePlayer(e.getPlayer());
        checkWin();
    }

    // победа: последний живой игрок, а в командном режиме — последняя живая команда
    // (победа засчитывается всем её участникам, включая выбывших)
    private static void checkWin() {
        if (!isRunning || isTest) {
            return;
        }
        if (gamePlayers.isEmpty()) {
            stopGame();
            return;
        }
        if (Teams.isActive()) {
            Set<Integer> alive = new HashSet<>();
            gamePlayers.forEach(player -> alive.add(Teams.teamOf(player)));
            if (alive.size() > 1) {
                return;
            }
            int team = alive.iterator().next();
            List<String> names = new ArrayList<>();
            for (UUID member : Teams.membersOf(team)) {
                Player player = Bukkit.getPlayer(member);
                if (player != null) {
                    Stats.increment(player, Stats.WINS);
                    names.add(player.getName());
                }
            }
            BowsUtils.broadcastSystemMessage(Bukkit.getOnlinePlayers(),
                    Teams.colorOf(team) + "Команда " + (team + 1) + ChatColor.RESET + " победила: " + String.join(", ", names));
            stopGame();
            return;
        }
        if (gamePlayers.size() == 1) {
            BowsUtils.broadcastSystemMessage(Bukkit.getOnlinePlayers(), gamePlayers.get(0).getName() + " победил!");
            Stats.increment(gamePlayers.get(0), Stats.WINS);
            stopGame();
        }
    }

    // сокомандники не ранят друг друга (LOWEST — раньше проверки смертельного урона)
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void preventFriendlyFire(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player victim && Teams.areTeammates(victim, Stats.findAttacker(e))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventBowAndArrowsClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null || e.getInventory() == null)
            return;

        if (e.getClick().equals(ClickType.NUMBER_KEY) && (e.getHotbarButton() == 0 || e.getHotbarButton() == 1)) {
            e.setCancelled(true);
            return;
        }
        // у пустого слота getCurrentItem() == null
        if (isBowOrArrow(e.getCurrentItem()) || isBowOrArrow(e.getCursor())) {
            e.setCancelled(true);
        }
    }

    private static boolean isBowOrArrow(ItemStack item) {
        return item != null && (item.getType() == Material.BOW || item.getType() == Material.ARROW);
    }

    // отменённый урон (например, падение под защитой буста) не должен убивать
    @EventHandler(ignoreCancelled = true)
    public void preventBowAndArrowsClick(EntityDamageEvent e) {

        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();

            if (p.getHealth() - e.getDamage() <= 0) {
                e.setCancelled(true);
                if (isRunning && gamePlayers.contains(p) && StarterBoosts.tryRevive(p)) {
                    return;
                }
                if (isRunning && isTest && gamePlayers.contains(p)) {
                    p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
                    BowsUtils.broadcastSystemMessage(p, "Тестовая игра: ты бы умер, здоровье восстановлено");
                    return;
                }
                if (isRunning && gamePlayers.contains(p)) {
                    Stats.increment(p, Stats.DEATHS);
                    Player killer = Stats.findKiller(p, e);
                    if (killer != null) {
                        Stats.increment(killer, Stats.KILLS);
                        KillAnnouncer.onKill(killer, p.getName());
                        GiantBoost.onKill(killer);
                    }
                }
                Stats.forget(p);
                eliminatePlayer(p);
                checkWin();
            }
        }
    }

    // запоминаем, кто ранил игрока, чтобы засчитать убийство при смерти от чего-то ещё
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordDamage(EntityDamageEvent e) {
        if (isRunning) {
            Stats.recordDamage(e);
        }
    }

    @EventHandler
    public void preventSwapNotBoost(PlayerSwapHandItemsEvent e) {
        if (e.getOffHandItem() == null) {
            e.setCancelled(true);
            return;
        }
        if (e.getOffHandItem().getItemMeta() == null) {
            e.setCancelled(true);
            return;
        }
        if (e.getOffHandItem().getItemMeta().getLore() == null) {
            e.setCancelled(true);
            return;
        }
        List<String> lore = e.getOffHandItem().getItemMeta().getLore();
        if (!lore.get(lore.size() - 2).contains("bows")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventBoostSecontHandItemPlace(InventoryClickEvent e) {
        if (e.getSlot() == 40) {
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void preventBoostSecontHandItemDrag(InventoryDragEvent e) {
        if (e.getInventorySlots().contains(40)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventBowAndArrowsClick(PlayerDropItemEvent e) {

        if (e.getItemDrop().getItemStack().getType().equals(Material.BOW)
                || e.getItemDrop().getItemStack().getType().equals(Material.ARROW)) {
            e.setCancelled(true);
        }
    }

}
