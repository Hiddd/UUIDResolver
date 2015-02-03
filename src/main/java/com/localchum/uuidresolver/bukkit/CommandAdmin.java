package com.localchum.uuidresolver.bukkit;

import com.localchum.uuidresolver.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Created by localchum on 2/3/15.
 */
public class CommandAdmin implements CommandExecutor {

    private void sendUsage(CommandSender sender, Command command){
        sender.sendMessage(ChatColor.RED + command.getUsage());
    }

    private UUID isUuid(String string){
        if (string.length() == 32) {
            string = Util.fromWebUuid(string).toString();
        }

        if (string.length() == 36){
            try {
                return UUID.fromString(string);
            } catch (Exception e){
                return null;
            }
        }

        return null;
    }

    private String isUsername(String string){
        if (string.length() > 16){ return null; }

        for (int i = 0; i < string.length(); i++){
            char c = string.charAt(i);
            if (!(Character.isDigit(c) || Character.isAlphabetic(c) || c == '_')){
                return null;
            }
        }

        return string;
    }

    private void determineAndLookup(final CommandSender sender, final String input, boolean async){
        /*
         * UUID
         */

        final UUID uuidToUsername = isUuid(input);
        if (uuidToUsername != null) {
            if (async){
                final long start = System.currentTimeMillis();
                UuidResolver.get().getUsernameAsync(uuidToUsername, new Callback<MojangProfile>() {
                    @Override
                    public void run(MojangProfile obj) {
                        sender.sendMessage(ChatColor.YELLOW + "[?] " + uuidToUsername.toString() + " = " + obj.username + " (" + (System.currentTimeMillis() - start) + "ms)");
                    }
                });
            } else {
                long start = System.nanoTime();
                String username = UuidResolver.get().getUsernameSync(uuidToUsername);
                long diff = System.nanoTime() - start;
                sender.sendMessage(ChatColor.YELLOW + "[SYNC] " + uuidToUsername.toString() + " = " + username + " (" + diff + "ns)");
            }

            return;
        }

        /*
         * Username
         */
        if (async){
            final long start = System.currentTimeMillis();
            UuidResolver.get().getUUIDAsync(input, new Callback<MojangProfile>() {
                @Override
                public void run(MojangProfile obj) {
                    sender.sendMessage(ChatColor.YELLOW + "[?] " + obj.username + " = " + obj.uuid + " (" + (System.currentTimeMillis() - start) + "ms)");
                }
            });
        } else {
            long start = System.nanoTime();
            UUID uuid = UuidResolver.get().getUUIDSync(input);
            String username = UuidResolver.get().getUsernameSync(uuid);
            long diff = System.nanoTime() - start;
            sender.sendMessage(ChatColor.YELLOW + "[SYNC] " + username + " = " + uuid + " (" + diff + "ns)");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("uuidresolver.admin")){ sender.sendMessage(ChatColor.RED + "You do not have permission to perform this command."); return true; }
        if (args.length < 1){ sendUsage(sender, command); return true; }

        switch (args[0].toLowerCase()){
            case "lookup-any":
            case "lookup-sync":
                if (args.length < 2){ sendUsage(sender, command); return true; }
                String input = args[1];
                if (isUuid(input) != null || isUsername(input) != null){
                    determineAndLookup(sender, input, args[0].equalsIgnoreCase("lookup-any"));
                }
                break;
            case "bench":
                Player[] players = Bukkit.getOnlinePlayers();
                ResolverAPI api = UuidResolver.get();
                long startUsernames = System.nanoTime();
                for (Player p: players){
                    api.getUsernameSync(p.getUniqueId());
                }
                long diffUsernames = System.nanoTime() - startUsernames;
                long startUuids = System.nanoTime();
                for (Player p: players){
                    api.getUUIDSync(p.getName());
                }
                long diffUuids = System.nanoTime() - startUuids;

                sender.sendMessage(ChatColor.YELLOW + "[BENCH] IDTN=" + diffUsernames + "ns, NTID=" + diffUuids + "ns");
                break;
            default:
                sendUsage(sender, command);
        }
        return true;
    }

}
