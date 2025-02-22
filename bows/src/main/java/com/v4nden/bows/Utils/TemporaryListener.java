package com.v4nden.bows.Utils;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import com.v4nden.bows.Bows;

public class TemporaryListener<E extends Event> implements Listener {

    private boolean isUnregister = false;

    public TemporaryListener(Class<? extends Event> cz, EventPriority priority, TemporaryEvent<E> event) {
        Bukkit.getServer().getPluginManager().registerEvent(cz, this, priority,
                (ignored, ev) -> {
                    try {
                        event.use((E) ev);
                    } catch (ClassCastException e) {
                    }
                },

                Bows.instance);
    }

    public TemporaryListener(Class<? extends Event> cz, EventPriority priority, TemporaryEventAutoUnregister<E> event) {
        Bukkit.getServer().getPluginManager().registerEvent(cz, this, priority,
                (ignored, ev) -> {
                    try {
                        unregister(event.use((E) ev));
                    } catch (ClassCastException e) {
                    }
                }, Bows.instance);
    }

    public void unregister(boolean bool) {
        if (bool)
            unregister();
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        isUnregister = true;
    }

    public boolean isUnregister() {
        return isUnregister;
    }

    public interface TemporaryEvent<E extends Event> {

        void use(E event);

    }

    public interface TemporaryEventAutoUnregister<E extends Event> {

        /**
         * Return true if you want to remove the listener
         */
        boolean use(E event);

    }

}