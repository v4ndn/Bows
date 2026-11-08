package com.v4nden.bows.Game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Difficulty;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.v4nden.bows.Bows;
import com.v4nden.bows.BowsUtils;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;

/**
 * Настройки мира для новых игр, как на ванильном экране создания мира: тип мира,
 * сид, строения, сложность. Хранятся в config.yml плагина, меняются /worldsettings.
 */
public class WorldSettings {
    private static final String TYPE = "world.type";
    private static final String SEED = "world.seed";
    private static final String STRUCTURES = "world.structures";
    private static final String DIFFICULTY = "world.difficulty";

    private static final List<String> TYPES = List.of("NORMAL", "FLAT", "LARGE_BIOMES", "AMPLIFIED");
    private static final List<String> TYPE_NAMES = List.of("Обычный", "Плоский", "Большие биомы", "Усилённый");
    private static final List<String> DIFFICULTIES = List.of("PEACEFUL", "EASY", "NORMAL", "HARD");
    private static final List<String> DIFFICULTY_NAMES = List.of("Мирная", "Лёгкая", "Нормальная", "Сложная");

    private static FileConfiguration config() {
        return Bows.instance.getConfig();
    }

    @SuppressWarnings("deprecation")
    public static WorldType type() {
        String type = config().getString(TYPE, "NORMAL");
        return TYPES.contains(type) ? WorldType.valueOf(type) : WorldType.NORMAL;
    }

    /** Сид как ввели: пусто — случайный. */
    public static String seedText() {
        return config().getString(SEED, "");
    }

    /** Сид мира: число как есть, текст — как в ванилле, через hashCode, пусто — случайный. */
    public static long seed() {
        String text = seedText().strip();
        if (text.isEmpty()) {
            return new java.util.Random().nextLong();
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return text.hashCode();
        }
    }

    public static boolean structures() {
        return config().getBoolean(STRUCTURES, true);
    }

    public static Difficulty difficulty() {
        String difficulty = config().getString(DIFFICULTY, "PEACEFUL");
        return DIFFICULTIES.contains(difficulty) ? Difficulty.valueOf(difficulty) : Difficulty.PEACEFUL;
    }

    public static void open(Player player) {
        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(10)).build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("Настройки мира"))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .inputs(List.of(
                                DialogInput.singleOption("type", Component.text("Тип мира"),
                                        options(TYPES, TYPE_NAMES, config().getString(TYPE, "NORMAL"))).width(200).build(),
                                DialogInput.text("seed", Component.text("Сид (пусто — случайный)"))
                                        .initial(seedText()).maxLength(64).width(200).build(),
                                DialogInput.bool("structures", Component.text("Строения"))
                                        .initial(structures()).build(),
                                DialogInput.singleOption("difficulty", Component.text("Сложность"),
                                        options(DIFFICULTIES, DIFFICULTY_NAMES, difficulty().name())).width(200).build()))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Сохранить")).width(100)
                                .action(DialogAction.customClick((response, audience) -> {
                                    config().set(TYPE, response.getText("type"));
                                    config().set(SEED, response.getText("seed"));
                                    config().set(STRUCTURES, response.getBoolean("structures"));
                                    config().set(DIFFICULTY, response.getText("difficulty"));
                                    Bows.instance.saveConfig();
                                    if (audience instanceof Player p) {
                                        BowsUtils.broadcastSystemMessage(p, "Настройки мира сохранены, они будут в следующей игре");
                                    }
                                }, once))
                                .build(),
                        ActionButton.builder(Component.text("Отмена")).width(100).build())));
        player.showDialog(dialog);
    }

    private static List<OptionEntry> options(List<String> ids, List<String> names, String selected) {
        List<OptionEntry> options = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            options.add(OptionEntry.create(ids.get(i), Component.text(names.get(i)), ids.get(i).equals(selected)));
        }
        return options;
    }
}
