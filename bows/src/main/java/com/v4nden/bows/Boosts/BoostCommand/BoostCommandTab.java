package com.v4nden.bows.Boosts.BoostCommand;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.v4nden.bows.Boosts.BoostTypes;

public class BoostCommandTab implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> tabcomplete = new ArrayList<>();
        for (BoostTypes type : BoostTypes.values()) {
            tabcomplete.add(type.id.toUpperCase());
        }
        tabcomplete.add("ALL");
        return tabcomplete;
    }

}
