package steyn91.blockparty.commands;

import steyn91.blockparty.Blockparty;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class bpAdminCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (command.getName().equalsIgnoreCase("bpadmin")){

            if (args.length < 1 || sender instanceof ConsoleCommandSender || sender instanceof CommandBlock){
                return false;
            }

            Player p = (Player) sender;
            if (!p.hasPermission("blockparty.admin")){
                p.sendMessage("Нет прав");
            }

            if (args[0].equalsIgnoreCase("reload")){
                Blockparty.LoadArenasFromConfig();
            }

            // TODO all other commands for arena creation and configuration

        }

        return false;
    }
}
