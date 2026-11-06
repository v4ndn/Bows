package com.v4nden.bows.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HexFormat;
import java.util.Properties;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.packs.ResourcePack;

import com.v4nden.bows.Bows;

/**
 * Актуальный ресурспак без рестарта сервера.
 *
 * Сервер раздаёт пак, ссылку и sha1 которого прочитал из server.properties при
 * своём старте. Ссылка обновляется при каждом пуше пака, поэтому плагин при
 * включении перечитывает server.properties с диска и:
 * - если пак изменился с прошлой рассылки — предлагает его всем онлайн;
 * - при входе игрока, если актуальный пак отличается от стартового, — заменяет
 *   им стартовый (а не добавляет поверх).
 */
public class ResourcePackSync implements Listener {
    // одинаковый id: новая версия пака заменяет нашу предыдущую у клиента
    private static final UUID PACK_ID = UUID.nameUUIDFromBytes("bows-resourcepack".getBytes(StandardCharsets.UTF_8));
    private static final String PROMPT = "Ресурспак Bows обновился";
    private static final String LAST_SENT_FILE = "last-resourcepack-sha1.txt";

    private static String url;
    private static String sha1;

    public static void sync() {
        Properties properties = new Properties();
        File file = new File(Bukkit.getWorldContainer(), "server.properties");
        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            Bows.instance.getLogger().warning("Не удалось прочитать server.properties: " + e.getMessage());
            return;
        }
        url = properties.getProperty("resource-pack", "").strip();
        sha1 = properties.getProperty("resource-pack-sha1", "").strip().toLowerCase();
        if (url.isEmpty() || sha1.length() != 40) {
            url = null;
            return;
        }

        // при перезагрузке плагина — только если пак правда изменился
        if (!sha1.equals(readLastSent())) {
            Bukkit.getOnlinePlayers().forEach(ResourcePackSync::send);
            writeLastSent(sha1);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (url == null || isStartupPack()) {
            return;
        }
        send(e.getPlayer());
    }

    private static void send(Player player) {
        ResourcePack startup = Bukkit.getServer().getServerResourcePack();
        // устаревший стартовый пак сервера убираем, чтобы старые иконки не легли поверх
        if (startup != null && !isStartupPack()) {
            player.removeResourcePack(startup.getId());
        }
        player.setResourcePack(PACK_ID, url, HexFormat.of().parseHex(sha1), PROMPT, false);
    }

    private static boolean isStartupPack() {
        ResourcePack startup = Bukkit.getServer().getServerResourcePack();
        return startup != null && sha1.equalsIgnoreCase(startup.getHash());
    }

    private static String readLastSent() {
        File file = new File(Bows.instance.getDataFolder(), LAST_SENT_FILE);
        try {
            return file.exists() ? Files.readString(file.toPath()).strip() : "";
        } catch (IOException e) {
            return "";
        }
    }

    private static void writeLastSent(String hash) {
        File folder = Bows.instance.getDataFolder();
        try {
            Files.createDirectories(folder.toPath());
            Files.writeString(new File(folder, LAST_SENT_FILE).toPath(), hash);
        } catch (IOException e) {
            Bows.instance.getLogger().warning("Не удалось сохранить " + LAST_SENT_FILE + ": " + e.getMessage());
        }
    }
}
