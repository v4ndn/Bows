package com.v4nden.bows;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostCommand.BoostCommand;
import com.v4nden.bows.Boosts.BoostCommand.BoostCommandTab;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Game.GameCommand.StartGame;

public class Bows extends JavaPlugin {

    public static JavaPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        this.getCommand("startgame").setExecutor(new StartGame());
        this.getCommand("boost").setExecutor(new BoostCommand());
        this.getCommand("boost").setTabCompleter(new BoostCommandTab());

        Bukkit.getServer().getPluginManager().registerEvents(new Game(), this);

        getLogger().info("All settled");

    }

    @Override
    public void onDisable() {
        Game.stopGame();

        Game.isRunning = false;
        Game.gameLocation = null;
        Game.gamePlayers = new ArrayList<Player>();

        getLogger().info("See you again, SpigotMC!");
    }

    public FileConfiguration loadConfig() {
        FileConfiguration config = this.getConfig();
        return config;
    }

}