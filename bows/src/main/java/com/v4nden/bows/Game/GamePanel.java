package com.v4nden.bows.Game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostIcons;
import com.v4nden.bows.Boosts.StarterBoosts;
import com.v4nden.bows.Utils.PlayerHeads;

import io.papermc.paper.scoreboard.numbers.NumberFormat;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Панель участников в левом верхнем углу во время игры: по строке на каждого
 * участника, кроме смотрящего, — голова скина, иконка стартового буста, радар
 * с направлением на участника, ник, хп и высота.
 *
 * Это сайдбар личного скорборда смотрящего (у каждого свой радар). Пакетами
 * через PacketEvents его не отправить: PacketEvents не умеет головы в тексте,
 * поэтому — скорборд Paper. Команды ников над головой TabArrows копирует и в него. Шейдеры ресурспака переносят его из правого края
 * в левый верхний угол:
 *  - rendertype_text.vsh — текст строк, по цвету-метке (254, вид, число строк);
 *  - gui.vsh — фон сайдбара, по его геометрии.
 * Для этого все строки дополняются до одной ширины WIDTH: тогда левый край
 * сайдбара известен шейдеру (ширина экрана − WIDTH − 3). Без ресурспака панель
 * останется обычным сайдбаром справа.
 *
 * Сайдбар рисует текст без тени, поэтому строка рисуется дважды: сначала копия
 * с меткой тени (красный 253) — шейдер сдвигает её на 1px и затемняет, — затем
 * возврат на ширину строки и сам текст. Ванильный фон сайдбара прячет gui.vsh.
 */
public class GamePanel {
    // должны совпадать с PANEL_WIDTH и метками в шейдерах
    private static final int WIDTH = 210;
    private static final int MARKER_RED = 254;
    private static final int SHADOW_MARKER_RED = 253;
    private static final int KIND_WHITE = 3;
    private static final int KIND_RADAR = 5;
    private static final int KIND_GRAY = 6;
    // 16 + код цвета (0–15): ник в цвете команды
    private static final int KIND_LEGACY = 16;
    // клиент показывает в сайдбаре не больше 15 строк
    private static final int MAX_LINES = 15;

    private static final String OBJECTIVE = "bows_hud";
    // владельцы строк — не ники: иначе клиент добавил бы к строке суффикс команды игрока.
    // Без «#» в начале: такие владельцы клиент прячет из сайдбара
    private static final String LINE_OWNER = "bows_hud_";

    private static final Key RADAR_FONT = Key.key("bows", "radar");
    private static final int RADAR_FIRST_CODEPOINT = 0xE200;
    private static final int RADAR_WIDTH = 8;
    private static final int HEAD_WIDTH = 8;
    private static final int SPACE_WIDTH = 4;
    // пробел на −WIDTH из шрифта bows:hud (resourcepack/gen_hud.py)
    private static final Key HUD_FONT = Key.key("bows", "hud");
    private static final String BACK_TO_LINE_START = "\uE351";

    // смотрящий -> его скорборд с панелью и выставленные строки (ключи для сравнения)
    private static final Map<UUID, Scoreboard> boards = new HashMap<>();
    private static final Map<UUID, List<String>> sent = new HashMap<>();
    private static int task = -1;

    public static void start() {
        stop();
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, GamePanel::update, 0L, 2L);
    }

    public static void stop() {
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task);
            task = -1;
        }
        for (UUID uuid : new ArrayList<>(boards.keySet())) {
            remove(uuid);
        }
    }

    /** Личные скорборды панели: в них тоже нужны команды ников над головой. */
    public static Collection<Scoreboard> boards() {
        return boards.values();
    }

    private static void update() {
        Set<UUID> online = new HashSet<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            online.add(viewer.getUniqueId());
            List<Player> targets = new ArrayList<>();
            if (Game.isRunning) {
                for (Player target : Game.gamePlayers) {
                    if (!target.equals(viewer) && target.isOnline()) {
                        targets.add(target);
                    }
                }
            }
            targets.sort(Comparator.comparing(target -> target.getName().toLowerCase()));
            if (targets.size() > MAX_LINES) {
                targets = targets.subList(0, MAX_LINES);
            }
            // пустой сайдбар шейдер не узнал бы по ширине — убираем его совсем
            if (targets.isEmpty()) {
                remove(viewer.getUniqueId());
                continue;
            }
            show(viewer, targets);
        }
        for (UUID uuid : new ArrayList<>(boards.keySet())) {
            if (!online.contains(uuid)) {
                remove(uuid);
            }
        }
    }

    private static void show(Player viewer, List<Player> targets) {
        Scoreboard board = boards.get(viewer.getUniqueId());
        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = board.registerNewObjective(OBJECTIVE, Criteria.DUMMY, Component.empty());
            objective.numberFormat(NumberFormat.blank());
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            boards.put(viewer.getUniqueId(), board);
        }
        if (viewer.getScoreboard() != board) {
            viewer.setScoreboard(board);
        }
        Objective objective = board.getObjective(OBJECTIVE);
        List<String> previous = sent.getOrDefault(viewer.getUniqueId(), List.of());
        int lines = targets.size();
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < lines; i++) {
            Player target = targets.get(i);
            Boost starter = StarterBoosts.current(target);
            int sector = TabArrows.radarSector(viewer, target);
            org.bukkit.ChatColor teamColor = Teams.colorOf(target);
            // число строк — часть метки, поэтому при его изменении меняются все строки
            String key = lines + "|" + target.getUniqueId() + "|" + (starter == null ? "" : starter.getId()) + "|"
                    + sector + "|" + teamColor + "|" + stats(target);
            keys.add(key);
            if (i < previous.size() && key.equals(previous.get(i))) {
                continue;
            }
            // сайдбар сортирует по убыванию очков: первая строка — с наибольшим
            Score score = objective.getScore(LINE_OWNER + i);
            score.setScore(lines - i);
            score.customName(line(target, starter, sector, teamColor, lines));
        }
        for (int i = lines; i < previous.size(); i++) {
            board.resetScores(LINE_OWNER + i);
        }
        sent.put(viewer.getUniqueId(), keys);
    }

    // вернуть игроку основной скорборд
    private static void remove(UUID uuid) {
        Scoreboard board = boards.remove(uuid);
        sent.remove(uuid);
        Player viewer = Bukkit.getPlayer(uuid);
        if (board != null && viewer != null && viewer.getScoreboard() == board) {
            viewer.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    // голова, иконка стартового буста, радар, ник и отступ до общей ширины.
    // Все части — соседи под корнем без шрифта: иначе ник унаследовал бы шрифт иконки
    private static Component line(Player target, Boost starter, int sector, org.bukkit.ChatColor teamColor,
            int lines) {
        return Component.text()
                .append(content(target, starter, sector, teamColor, lines, true))
                .append(Component.text(BACK_TO_LINE_START).font(HUD_FONT))
                .append(content(target, starter, sector, teamColor, lines, false))
                .build();
    }

    private static Component content(Player target, Boost starter, int sector, org.bukkit.ChatColor teamColor,
            int lines, boolean shadow) {
        int red = shadow ? SHADOW_MARKER_RED : MARKER_RED;
        TextColor white = marker(red, KIND_WHITE, lines);
        TextComponent.Builder line = Component.text();
        line.append(PlayerHeads.head(target).color(white));
        line.append(Component.text(" ").color(white));
        int width = HEAD_WIDTH + SPACE_WIDTH;

        if (starter != null) {
            line.append(BoostIcons.icon(starter.getId(), BoostIcons.SMALL_FONT).color(white));
            line.append(Component.text(" ").color(white));
        } else {
            line.append(pad(BoostIcons.SMALL_WIDTH + SPACE_WIDTH, white));
        }
        width += BoostIcons.SMALL_WIDTH + SPACE_WIDTH;

        if (sector >= 0) {
            line.append(Component.text(String.valueOf((char) (RADAR_FIRST_CODEPOINT + sector)))
                    .font(RADAR_FONT)
                    .color(marker(red, KIND_RADAR, lines))
                    .shadowColor(ShadowColor.none()));
            line.append(Component.text(" ").color(white));
        } else {
            line.append(pad(RADAR_WIDTH + SPACE_WIDTH, white));
        }
        width += RADAR_WIDTH + SPACE_WIDTH;

        TextColor nameColor = teamColor != null && teamColor.isColor()
                ? marker(red, KIND_LEGACY + teamColor.ordinal(), lines)
                : white;
        line.append(Component.text(target.getName()).color(nameColor));
        width += TabArrows.textWidth(target.getName());

        String stats = stats(target);
        line.append(Component.text(stats).color(marker(red, KIND_GRAY, lines)));
        width += TabArrows.textWidth(stats);

        line.append(pad(WIDTH - width, white));
        return line.build();
    }

    // хп и высота после ника
    private static String stats(Player target) {
        return " [" + (int) target.getHealth() + "] [" + target.getLocation().getBlockY() + "]";
    }

    private static TextColor marker(int red, int kind, int lines) {
        return TextColor.color(red, kind, lines);
    }

    // отступ ровно в pixels пикселей: пробел 4px, жирный пробел 5px (от 12px — любая ширина)
    private static Component pad(int pixels, TextColor color) {
        for (int bold = 0; bold < 4; bold++) {
            int rest = pixels - bold * 5;
            if (rest >= 0 && rest % 4 == 0) {
                return Component.text()
                        .append(Component.text(" ".repeat(rest / 4)).color(color))
                        .append(Component.text(" ".repeat(bold)).color(color).decorate(TextDecoration.BOLD))
                        .build();
            }
        }
        return Component.text(" ".repeat(Math.max(0, Math.round(pixels / 4f)))).color(color);
    }
}
