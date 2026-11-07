package com.v4nden.bows.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo;
import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostIcons;
import com.v4nden.bows.Boosts.StarterBoosts;

import io.github.retrooper.packetevents.adventure.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;

/**
 * Таб и ник над головой.
 *
 * Ник — ванильный: суффикс команды на основном скорборде с хп участника.
 * Таб — разный для каждого смотрящего: имя в табе отправляется пакетом каждому
 * игроку отдельно (иконка стартового буста, радар с направлением на участника
 * относительно взгляда смотрящего, ник, хп, высота и статистика). Имя в табе
 * перекрывает суффикс команды, поэтому в табе суффикс не дублируется.
 */
public class TabArrows {
    // радар — символ шрифта bows:radar из ресурспака (resourcepack/gen_icon_font.py),
    // по символу на каждое из 12 направлений; у всех одинаковая ширина
    private static final Key RADAR_FONT = Key.key("bows", "radar");
    private static final int RADAR_FIRST_CODEPOINT = 0xE200;
    private static final int RADAR_SECTORS = 12;
    // ширина символа радара: 7px + 1px после символа
    private static final int RADAR_WIDTH = 8;

    private static final String TEAM_PREFIX = "bows_";

    // колонки статистики: иконка, цвет, ключ
    private static final String[] STAT_ICONS = { "✦", "⚔", "☠" };
    private static final String[] STAT_COLORS = { "#f2c14e", "#e05b4b", "#a6a6a6" };
    private static final NamespacedKey[] STAT_KEYS = { Stats.WINS, Stats.KILLS, Stats.DEATHS };
    // отступы в пикселях шрифта; меньше 12 точно набрать нельзя (см. pad)
    private static final int COLUMN_GAP = 12;
    private static final int DIGIT_WIDTH = 6;
    private static final int SPACE_WIDTH = 4;
    // раз в столько обновлений имена шлются заново, даже если не менялись:
    // сервер сам перезаписывает имя в табе, например при входе игрока
    private static final int FULL_RESEND_EVERY = 10;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    // смотрящий -> (цель -> последнее отправленное имя)
    private static final Map<UUID, Map<UUID, String>> sent = new HashMap<>();
    private static int task = -1;
    private static int updates = 0;

    public static void start() {
        stop();
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, TabArrows::update, 0L, 2L);
    }

    public static void stop() {
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task);
            task = -1;
        }
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : new HashSet<>(board.getTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.unregister();
            }
        }
        // вернуть всем обычные имена в табе
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            List<PlayerInfo> infos = new ArrayList<>();
            for (Player target : Bukkit.getOnlinePlayers()) {
                PlayerInfo info = new PlayerInfo(target.getUniqueId());
                info.setDisplayName(null);
                infos.add(info);
            }
            sendDisplayNames(viewer, infos);
        }
        sent.clear();
    }

    private static void update() {
        updateNameTags();

        boolean fullResend = updates++ % FULL_RESEND_EVERY == 0;

        // левая часть (ник, хп, высота) и статистика одинаковы для всех смотрящих;
        // статистика выравнивается колонками по самой широкой строке
        Map<UUID, String> rows = new HashMap<>();
        Map<UUID, Integer> widths = new HashMap<>();
        int maxWidth = 0;
        int[] maxDigits = new int[STAT_KEYS.length];
        for (Player target : Bukkit.getOnlinePlayers()) {
            String left = leftPart(target);
            int width = textWidth(ChatColor.stripColor(left));
            rows.put(target.getUniqueId(), left);
            widths.put(target.getUniqueId(), width);
            maxWidth = Math.max(maxWidth, width);
            for (int i = 0; i < STAT_KEYS.length; i++) {
                maxDigits[i] = Math.max(maxDigits[i], String.valueOf(Stats.get(target, STAT_KEYS[i])).length());
            }
        }
        for (Player target : Bukkit.getOnlinePlayers()) {
            StringBuilder row = new StringBuilder(rows.get(target.getUniqueId()));
            row.append(pad(maxWidth - widths.get(target.getUniqueId()) + COLUMN_GAP));
            for (int i = 0; i < STAT_KEYS.length; i++) {
                String value = String.valueOf(Stats.get(target, STAT_KEYS[i]));
                row.append(ChatColor.of(STAT_COLORS[i])).append(STAT_ICONS[i]).append(value);
                if (i < STAT_KEYS.length - 1) {
                    row.append(pad((maxDigits[i] - value.length()) * DIGIT_WIDTH + COLUMN_GAP));
                }
            }
            rows.put(target.getUniqueId(), row.toString());
        }

        Set<UUID> online = new HashSet<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            online.add(viewer.getUniqueId());
            Map<UUID, String> sentToViewer = sent.computeIfAbsent(viewer.getUniqueId(), uuid -> new HashMap<>());

            List<PlayerInfo> infos = new ArrayList<>();
            for (Player target : Bukkit.getOnlinePlayers()) {
                Boost starter = StarterBoosts.current(target);
                String name = rows.get(target.getUniqueId());
                int sector = radarSector(viewer, target);
                String key = (starter == null ? "" : starter.getId()) + "|" + sector + "|" + name;
                // обновляем только при изменении, чтобы не слать лишние пакеты
                if (fullResend || !key.equals(sentToViewer.get(target.getUniqueId()))) {
                    sentToViewer.put(target.getUniqueId(), key);
                    PlayerInfo info = new PlayerInfo(target.getUniqueId());
                    // иконка и текст — соседи под корнем без шрифта: дочерние компоненты
                    // наследуют шрифт родителя, и текст после иконки стал бы квадратиками
                    info.setDisplayName(Component.text()
                            .append(starterPart(starter))
                            .append(radarPart(sector))
                            .append(LEGACY.deserialize(name))
                            .build());
                    infos.add(info);
                }
            }
            sendDisplayNames(viewer, infos);
        }
        sent.keySet().retainAll(online);
    }

    // слева от ника: иконка стартового буста и пробел; если он использован — отступ
    // той же ширины, чтобы ники и колонки не съезжали
    private static Component starterPart(Boost starter) {
        if (starter == null) {
            return LEGACY.deserialize(pad(BoostIcons.SMALL_WIDTH + SPACE_WIDTH));
        }
        return Component.text()
                .append(BoostIcons.icon(starter.getId(), BoostIcons.SMALL_FONT))
                .append(Component.text(" "))
                .build();
    }

    private static String leftPart(Player target) {
        // в командном режиме ник в цвете команды
        org.bukkit.ChatColor teamColor = Teams.colorOf(target);
        String name = (teamColor != null ? teamColor.toString() : "") + target.getName() + ChatColor.RESET;
        if (Game.isRunning && Game.gamePlayers.contains(target)) {
            name += ChatColor.of("#a6a6a6") + " [" + (int) target.getHealth() + "] ["
                    + target.getLocation().getBlockY() + "]";
        }
        return name;
    }

    // радар между иконкой стартового буста и ником; без радара — отступ той же ширины
    private static Component radarPart(int sector) {
        if (sector < 0) {
            return LEGACY.deserialize(pad(RADAR_WIDTH + SPACE_WIDTH));
        }
        return Component.text()
                .append(Component.text(String.valueOf((char) (RADAR_FIRST_CODEPOINT + sector)))
                        .font(RADAR_FONT)
                        .color(TextColor.fromHexString("#e97218"))
                        .shadowColor(ShadowColor.none()))
                .append(Component.text(" "))
                .build();
    }

    // направление на цель относительно взгляда смотрящего: 0 — вперёд, по часовой;
    // -1 — радара нет (это сам смотрящий, цель не в игре или в другом мире)
    private static int radarSector(Player viewer, Player target) {
        if (!Game.isRunning || !Game.gamePlayers.contains(target) || target.equals(viewer)) {
            return -1;
        }
        Location from = viewer.getLocation();
        Location to = target.getLocation();
        if (!from.getWorld().equals(to.getWorld())) {
            return -1;
        }
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        // yaw в Minecraft: 0 = +Z, растёт по часовой стрелке (поворот направо)
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = ((targetYaw - from.getYaw()) % 360 + 360) % 360;
        return (int) Math.round(relative / (360.0 / RADAR_SECTORS)) % RADAR_SECTORS;
    }

    // отступ ровно в pixels пикселей: обычный пробел 4px, жирный 5px,
    // их сочетаниями набирается любая ширина от 12px
    private static String pad(int pixels) {
        for (int bold = 0; bold < 4; bold++) {
            int rest = pixels - bold * 5;
            if (rest >= 0 && rest % 4 == 0) {
                return " ".repeat(rest / 4) + (bold > 0 ? "§l" + " ".repeat(bold) + "§r" : "");
            }
        }
        return " ".repeat(Math.max(0, Math.round(pixels / 4f)));
    }

    // ширина ASCII-текста в стандартном шрифте Minecraft, с промежутком 1px
    private static int textWidth(String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            width += charWidth(c);
        }
        return width;
    }

    private static int charWidth(char c) {
        switch (c) {
            case '!': case '\'': case ',': case '.': case ':': case ';': case '|': case 'i':
                return 2;
            case '`': case 'l':
                return 3;
            case ' ': case '"': case '(': case ')': case '*': case 'I': case '[': case ']': case 't':
            case '{': case '}':
                return 4;
            case '<': case '>': case 'f': case 'k':
                return 5;
            case '@': case '~':
                return 7;
            default:
                return 6;
        }
    }

    // ванильный ник над головой: только хп, и только у участников игры.
    // Команда есть у каждого игрока и названа по нику: клиент сортирует таб по
    // порядку в списке, затем наблюдателей вниз, затем по названию команды и нику —
    // так таб идёт по алфавиту, а наблюдатели внизу
    private static void updateNameTags() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Set<String> activeTeams = new HashSet<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getPlayerListOrder() != 0) {
                target.setPlayerListOrder(0);
            }
            // команда скорборда по команде игры и нику: таб группирует команды подряд
            int gameTeam = Teams.teamOf(target);
            String teamName = TEAM_PREFIX + (gameTeam >= 0 ? String.format("%02d_", gameTeam) : "zz_")
                    + target.getName().toLowerCase();
            activeTeams.add(teamName);

            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }
            // цвет ника над головой — цвет команды
            org.bukkit.ChatColor color = gameTeam >= 0 ? Teams.colorOf(gameTeam) : org.bukkit.ChatColor.WHITE;
            if (team.getColor() != color) {
                team.setColor(color);
            }
            String suffix = Game.isRunning && Game.gamePlayers.contains(target)
                    ? ChatColor.of("#e97218") + " [" + (int) target.getHealth() + "]"
                    : "";
            if (!suffix.equals(team.getSuffix())) {
                team.setSuffix(suffix);
            }
        }
        for (Team team : new HashSet<>(board.getTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX) && !activeTeams.contains(team.getName())) {
                team.unregister();
            }
        }
    }

    private static void sendDisplayNames(Player viewer, List<PlayerInfo> infos) {
        if (infos.isEmpty()) {
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerPlayerInfoUpdate(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME,
                        infos));
    }
}
