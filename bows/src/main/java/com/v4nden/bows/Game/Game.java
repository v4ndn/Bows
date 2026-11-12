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
import org.bukkit.World;
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

import com.v4nden.bows.Bows;
import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Boosts.BoostTypes;
import com.v4nden.bows.Boosts.StarterBoosts;
import com.v4nden.bows.Boosts.Types.ElusiveBoost;
import com.v4nden.bows.Boosts.Types.GiantBoost;


public class Game implements Listener {
    public static boolean isRunning = false;
    // тестовая игра: участвуют только выбранные игроки, смерть не выбивает из игры
    public static boolean isTest = false;
    public static Location gameLocation;
    public static long gameStart;
    // участников на старте игры (для бустов, зависящих от размера лобби)
    public static int startPlayerCount = 0;
    public static ArrayList<Player> gamePlayers = new ArrayList<Player>();

    // предел обычных бустов в инвентаре (стартовый не считается): сверх него буст не выдаётся
    public static final int MAX_BOOSTS = 10;

    private static final long BOOST_PERIOD_TICKS = 20 * 30L;
    // барьер сужается до ширины BORDER_MIN_SIZE за BORDER_SHRINK_SECONDS, затем финал:
    // до ширины BORDER_FINAL_SIZE за BORDER_FINAL_SECONDS, бусты — раз в FINAL_BOOST_PERIOD_TICKS
    private static final double BORDER_START_SIZE = 1400;
    private static final double BORDER_MIN_SIZE = 5;
    private static final long BORDER_SHRINK_SECONDS = 7 * 60;
    private static final double BORDER_FINAL_SIZE = 2;
    private static final long BORDER_FINAL_SECONDS = 3 * 60;
    private static final long FINAL_BOOST_PERIOD_TICKS = 20 * 10L;
    private static boolean isFinal = false;
    // когда выпадет следующий буст и раз в сколько они выпадают сейчас (для StatusBar)
    private static long nextBoostAt = 0;
    private static long boostPeriodMillis = BOOST_PERIOD_TICKS * 50;

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
        ScreenEffect.reset();
        GiantBoost.reset();
        StarterChoiceDialog.reset();

        gameStart = System.currentTimeMillis();
        isFinal = false;
        boostPeriodMillis = BOOST_PERIOD_TICKS * 50;

        Random random = new Random();

        gamePlayers.addAll(players);
        startPlayerCount = gamePlayers.size();
        gamePlayers.forEach((player) -> {
            resetPlayerState(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.getInventory().setItem(0, new ItemStack(Material.BOW));
            player.getInventory().setItem(1, new ItemStack(Material.ARROW, 20));
            // стартовый буст игрок выбирает сам в окне (или играет без него); окно — после загрузки мира игры
            StarterChoiceDialog.showWhenLoaded(player);
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

            String center = gameLocation.getWorld().getWorldBorder().getCenter().getBlockX() + ", "
                    + gameLocation.getWorld().getWorldBorder().getCenter().getBlockZ();
            String message = "\n     Центр: " + center + "     \nРадиус: "
                    + (int) (gameLocation.getWorld().getWorldBorder().getSize() / 2) + "\n";

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setPlayerListFooter(message);

            }
            // по размеру, а не по таймеру: так финал не сдвинется, если барьер замирал
            if (!isFinal && gameLocation.getWorld().getWorldBorder().getSize() <= BORDER_MIN_SIZE + .01) {
                startFinal();
            }
        }, 0L, 20L);
        taskActionTimer = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
            gamePlayers.forEach((player) -> {
                // полоска опыта скрыта ресурспаком, на её месте — StatusBar;
                // уровень обнуляем, чтобы над ним не висела цифра
                if (player.getLevel() != 0 || player.getExp() != 0) {
                    player.setLevel(0);
                    player.setExp(0);
                }
                StatusBar.send(player);
            });
        }, 0L, 2L);
        taskBoosts = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bows.instance, Game::giveBoosts,
                0L, BOOST_PERIOD_TICKS);
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

        // граница — в мире игры (у каждой игры свой мир, см. GameWorlds)
        location.getWorld().getWorldBorder().setSize(BORDER_START_SIZE);
        location.getWorld().getWorldBorder().setCenter(location);
        location.getWorld().getWorldBorder().setSize(BORDER_MIN_SIZE, BORDER_SHRINK_SECONDS);
    }

    private static void startFinal() {
        isFinal = true;
        gameLocation.getWorld().getWorldBorder().setSize(BORDER_FINAL_SIZE, BORDER_FINAL_SECONDS);
        Bukkit.getServer().getScheduler().cancelTask(taskBoosts);
        boostPeriodMillis = FINAL_BOOST_PERIOD_TICKS * 50;
        nextBoostAt = System.currentTimeMillis() + boostPeriodMillis;
        taskBoosts = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bows.instance, Game::giveBoosts,
                FINAL_BOOST_PERIOD_TICKS, FINAL_BOOST_PERIOD_TICKS);
    }

    /** Доля пройденного времени до следующего буста: 0 — только что выпал, 1 — вот-вот. */
    public static double boostProgress() {
        double left = (nextBoostAt - System.currentTimeMillis()) / (double) boostPeriodMillis;
        return Math.max(0, Math.min(1, 1 - left));
    }

    /** Прогресс финала: −1 — финала нет, 0 — только начался, 1 — барьер финального размера. */
    public static double finalProgress() {
        if (!isFinal) {
            return -1;
        }
        double size = gameLocation.getWorld().getWorldBorder().getSize();
        return Math.max(0, Math.min(1, (BORDER_MIN_SIZE - size) / (BORDER_MIN_SIZE - BORDER_FINAL_SIZE)));
    }

    /** Насколько барьер сузился к финальному размеру: 0 — старт, 1 — финальный размер. */
    public static double borderProgress() {
        double size = gameLocation.getWorld().getWorldBorder().getSize();
        return Math.max(0, Math.min(1, (BORDER_START_SIZE - size) / (BORDER_START_SIZE - BORDER_FINAL_SIZE)));
    }

    private static void giveBoosts() {
        nextBoostAt = System.currentTimeMillis() + boostPeriodMillis;
        List<String> boostsList = new ArrayList<>();

        for (BoostTypes type : BoostTypes.values()) {
            boostsList.add(type.name());
        }

        BowsUtils.broadcastSystemMessage(Bukkit.getOnlinePlayers(), "Буст!");

        gamePlayers.forEach((player) -> {
            if (StarterBoosts.regularBoostSlots(player).size() >= MAX_BOOSTS) {
                return;
            }
            int rnd = new Random().nextInt(boostsList.size());
            player.getInventory()
                    .addItem(BoostTypes.valueOf(boostsList.get(rnd)).getBoost(player).createItem());
        });
    }

    public static void stopGame() {
        // в тестовой игре не трогаем игроков, которые в ней не участвовали
        Collection<? extends Player> playersToReset = isTest ? new ArrayList<>(gamePlayers)
                : Bukkit.getOnlinePlayers();
        if (isRunning) {
            KillAnnouncer.announceTop();
        }
        isRunning = false;
        isTest = false;
        Bukkit.getServer().getScheduler().cancelTask(taskActionTimer);
        Bukkit.getServer().getScheduler().cancelTask(taskRenewArrowsTimer);
        Bukkit.getServer().getScheduler().cancelTask(taskTabInfos);
        Bukkit.getServer().getScheduler().cancelTask(taskBoosts);
        isFinal = false;
        for (Player p : playersToReset) {
            p.setGameMode(GameMode.ADVENTURE);
            p.teleport(GameWorlds.lobbySpawn());
            gamePlayers.remove(p);
            p.getInventory().clear();
            p.setPlayerListFooter("");
            resetPlayerState(p);
        }
        gamePlayers.clear();
        Dummies.clear();
        GiantBoost.reset();
        Teams.reset();
        GameWorlds.lobby().getWorldBorder().setCenter(0, 0);
        GameWorlds.lobby().getWorldBorder().setSize(500);
        // мир игры больше не нужен: выгрузить и удалить. Следующим тиком: stopGame часто
        // вызывается из события внутри этого мира, а выгружать мир посреди его тика нельзя
        if (gameLocation != null) {
            World world = gameLocation.getWorld();
            if (Bows.instance.isEnabled()) {
                Bukkit.getScheduler().runTask(Bows.instance, () -> GameWorlds.delete(world));
            } else {
                GameWorlds.delete(world);
            }
        }
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
                    .teleport(gameLocation.getWorld()
                            .getHighestBlockAt(gameLocation.getWorld().getWorldBorder().getCenter()).getLocation()
                            .add(0, 2, 0));
        } else {
            e.getPlayer().teleport(GameWorlds.lobbySpawn());
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
