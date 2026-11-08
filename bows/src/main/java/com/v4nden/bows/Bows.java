package com.v4nden.bows;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostCommand.BoostCommand;
import com.v4nden.bows.Boosts.BoostCommand.BoostCommandTab;
import com.v4nden.bows.Boosts.Types.DoubleJumpBoost;
import com.v4nden.bows.Boosts.Types.GathererBoost;
import com.v4nden.bows.Game.Dummies;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Game.GameWorlds;
import com.v4nden.bows.Game.StarterChoiceDialog;
import com.v4nden.bows.Game.GameStarter;
import com.v4nden.bows.Game.PeacefulExplosions;
import com.v4nden.bows.Game.RadiusHighlight;
import com.v4nden.bows.Utils.ResourcePackSync;
import com.v4nden.bows.Game.TabArrows;
import com.v4nden.bows.Game.GameCommand.DummyCommand;
import com.v4nden.bows.Game.GameCommand.HudTextCommand;
import com.v4nden.bows.Game.GameCommand.InfoCommand;
import com.v4nden.bows.Game.GameCommand.WorldSettingsCommand;
import com.v4nden.bows.Game.GameCommand.StartGame;
import com.v4nden.bows.Game.GameCommand.StopGame;

public class Bows extends JavaPlugin {

    public static JavaPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        this.getCommand("startgame").setExecutor(new StartGame());
        this.getCommand("stopgame").setExecutor(new StopGame());
        DummyCommand dummyCommand = new DummyCommand();
        this.getCommand("dummy").setExecutor(dummyCommand);
        this.getCommand("dummy").setTabCompleter(dummyCommand);
        this.getCommand("info").setExecutor(new InfoCommand());
        this.getCommand("worldsettings").setExecutor(new WorldSettingsCommand());
        HudTextCommand hudTextCommand = new HudTextCommand();
        this.getCommand("hudtext").setExecutor(hudTextCommand);
        this.getCommand("hudtext").setTabCompleter(hudTextCommand);
        this.getCommand("boost").setExecutor(new BoostCommand());
        this.getCommand("boost").setTabCompleter(new BoostCommandTab());

        Bukkit.getServer().getPluginManager().registerEvents(new Game(), this);
        // миры игр, оставшиеся после падения или перезагрузки
        GameWorlds.cleanupLeftovers();
        Bukkit.getServer().getPluginManager().registerEvents(new Dummies(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PeacefulExplosions(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new DoubleJumpBoost.Handler(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new GathererBoost.Handler(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new StarterChoiceDialog.Handler(), this);
        TabArrows.start();
        RadiusHighlight.start();
        Bukkit.getServer().getPluginManager().registerEvents(new ResourcePackSync(), this);
        ResourcePackSync.sync();

        getLogger().info("All settled");

    }

    @Override
    public void onDisable() {
        GameStarter.cancel();
        Game.stopGame();
        TabArrows.stop();
        RadiusHighlight.stop();

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