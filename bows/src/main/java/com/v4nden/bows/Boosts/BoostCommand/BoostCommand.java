package com.v4nden.bows.Boosts.BoostCommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.v4nden.bows.Boosts.BoostTypes;
import com.v4nden.bows.Boosts.StarterBoostTypes;
import com.v4nden.bows.Boosts.StarterBoosts;

public class BoostCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length < 1)
                return false;
            if (args[0].equals("ALL")) {
                for (BoostTypes type : BoostTypes.values()) {
                    player.getInventory().addItem(type.getBoost(player).createItem());
                }
                return false;
            }
            for (StarterBoostTypes type : StarterBoostTypes.values()) {
                if (type.name().equalsIgnoreCase(args[0])) {
                    StarterBoosts.give(player, type);
                    return true;
                }
            }
            for (BoostTypes type : BoostTypes.values()) {
                if (type.name().equalsIgnoreCase(args[0])) {
                    player.getInventory().addItem(type.getBoost(player).createItem());
                    return true;
                }
            }
            return false;

        }

        return false;
    }

}
