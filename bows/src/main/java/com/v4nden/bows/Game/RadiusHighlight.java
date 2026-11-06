package com.v4nden.bows.Game;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;

/**
 * Пока игрок держит в руке буст с радиусом, ему — и только ему — подсвечиваются
 * цели, которых этот буст сейчас задел бы.
 *
 * Подсветка — флаг «светится» в метаданных сущности, отправленный одному игроку.
 * Сервер сам переотправляет метаданные (присел, побежал, загорелся), поэтому
 * исходящие метаданные подсвеченных целей перехватываются и флаг дописывается.
 */
public class RadiusHighlight {
    private static final long UPDATE_TICKS = 5L;
    private static final int FLAGS_INDEX = 0;
    private static final byte ON_FIRE = 0x01;
    private static final byte SNEAKING = 0x02;
    private static final byte SPRINTING = 0x08;
    private static final byte SWIMMING = 0x10;
    private static final byte INVISIBLE = 0x20;
    private static final byte GLOWING = 0x40;
    private static final byte GLIDING = (byte) 0x80;

    // смотрящий -> id сущностей, подсвеченных только для него (читается из потока сети)
    private static final Map<UUID, Set<Integer>> highlighted = new ConcurrentHashMap<>();
    private static int task = -1;
    private static PacketListenerCommon listener;

    public static void start() {
        stop();
        listener = PacketEvents.getAPI().getEventManager().registerListener(new MetadataListener());
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, RadiusHighlight::update, 0L,
                UPDATE_TICKS);
    }

    public static void stop() {
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task);
            task = -1;
        }
        if (listener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            listener = null;
        }
        for (UUID viewerId : highlighted.keySet()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null) {
                highlighted.get(viewerId).forEach(id -> sendFlags(viewer, id, false));
            }
        }
        highlighted.clear();
    }

    private static void update() {
        highlighted.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Set<Integer> targets = targetsFor(viewer);
            Set<Integer> current = highlighted.computeIfAbsent(viewer.getUniqueId(),
                    id -> ConcurrentHashMap.newKeySet());

            for (Integer id : new HashSet<>(current)) {
                if (!targets.contains(id)) {
                    current.remove(id);
                    sendFlags(viewer, id, false);
                }
            }
            for (Integer id : targets) {
                if (current.add(id)) {
                    sendFlags(viewer, id, true);
                }
            }
        }
    }

    // цели буста в руке: те же условия, что у самих бустов — в радиусе, кроме владельца
    private static Set<Integer> targetsFor(Player viewer) {
        Set<Integer> targets = new HashSet<>();
        Boost boost = Boost.fromItem(viewer.getInventory().getItemInMainHand());
        if (boost == null || !viewer.equals(boost.owner) || boost.getPreviewRadius() <= 0) {
            return targets;
        }
        double radius = boost.getPreviewRadius();
        for (LivingEntity target : Game.targetsExcept(viewer)) {
            if (target.getWorld().equals(viewer.getWorld())
                    && target.getLocation().distance(viewer.getLocation()) < radius) {
                targets.add(target.getEntityId());
            }
        }
        return targets;
    }

    private static void sendFlags(Player viewer, int entityId, boolean glow) {
        Entity entity = findEntity(viewer, entityId);
        if (entity == null) {
            return;
        }
        byte flags = flagsOf(entity);
        if (glow) {
            flags |= GLOWING;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, new WrapperPlayServerEntityMetadata(entityId,
                List.of(new EntityData<>(FLAGS_INDEX, EntityDataTypes.BYTE, flags))));
    }

    private static Entity findEntity(Player viewer, int entityId) {
        for (Entity entity : viewer.getWorld().getEntities()) {
            if (entity.getEntityId() == entityId) {
                return entity;
            }
        }
        return null;
    }

    // настоящие флаги сущности, чтобы не сбить огонь, присед, невидимость и т.д.
    private static byte flagsOf(Entity entity) {
        byte flags = 0;
        if (entity.getFireTicks() > 0) {
            flags |= ON_FIRE;
        }
        if (entity.isGlowing()) {
            flags |= GLOWING;
        }
        if (entity instanceof LivingEntity living) {
            if (living.isInvisible()) {
                flags |= INVISIBLE;
            }
            if (living.isSwimming()) {
                flags |= SWIMMING;
            }
            if (living.isGliding()) {
                flags |= GLIDING;
            }
        }
        if (entity instanceof Player player) {
            if (player.isSneaking()) {
                flags |= SNEAKING;
            }
            if (player.isSprinting()) {
                flags |= SPRINTING;
            }
        }
        return flags;
    }

    private static class MetadataListener extends PacketListenerAbstract {
        @Override
        public void onPacketSend(PacketSendEvent event) {
            if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA || event.getUser().getUUID() == null) {
                return;
            }
            Set<Integer> ids = highlighted.get(event.getUser().getUUID());
            if (ids == null || ids.isEmpty()) {
                return;
            }
            WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(event);
            if (!ids.contains(metadata.getEntityId())) {
                return;
            }
            for (EntityData<?> data : metadata.getEntityMetadata()) {
                if (data.getIndex() == FLAGS_INDEX && data.getValue() instanceof Byte flags) {
                    @SuppressWarnings("unchecked")
                    EntityData<Byte> byteData = (EntityData<Byte>) data;
                    byteData.setValue((byte) (flags | GLOWING));
                    event.markForReEncode(true);
                }
            }
        }
    }
}
