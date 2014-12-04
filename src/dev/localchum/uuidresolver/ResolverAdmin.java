package dev.localchum.uuidresolver;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResolverAdmin implements CommandExecutor {
	
	public String color(String s){
		return ChatColor.translateAlternateColorCodes('&', s);
	}
	
	public void sendInvalidUsage(CommandSender sender, Command cmd){
		sender.sendMessage(color("&cInvalid syntax. Usage: " + cmd.getUsage()));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission("uuidresolver.admin")){
			sender.sendMessage(color("&cYou do not have permission to perform this command."));
			return true;
		}
		
		if (args.length == 0){
			sendInvalidUsage(sender, cmd);
			return true;
		}
		
		switch(args[0]){
		case "stats":
			sender.sendMessage(color("&eTotal Entries: " + UUIDResolver.resolutionCache.size() + "/" + UUIDResolver.resolutionCacheLower.size()));
			sender.sendMessage(color("&eFailed Sync NTID Resolutions: "));
			String syncRes = "&e";
			for (String fr: UUIDResolver.failedUsernameResolutions){
				syncRes += fr + " ";
			}
			sender.sendMessage(color(syncRes));
			sender.sendMessage(color("&eFailed Sync IDTN Resolutions: "));
			String syncRes2 = "&e";
			for (String fr: UUIDResolver.failedUUIDResolutions){
				syncRes2 += fr + " ";
			}
			sender.sendMessage(color(syncRes2));
			break;
		case "repair-ntid":
			sender.sendMessage(color("&eStarting repair thread [NTID]. Do not use this command again."));
			UUIDResolver.performRepairNTID();
			break;
		case "repair-idtn":
			sender.sendMessage(color("&eStarting repair thread [IDTN]. Do not use this command again."));
			UUIDResolver.performRepairIDTN();
			break;
		case "uuid":
			if (args.length != 2){ sendInvalidUsage(sender, cmd); return true; }
			ResolutionInfo riuuid = UUIDResolver.performSyncQuery(new ResolutionInfo(false, args[1], null));
			sender.sendMessage(riuuid.success ? color("&eUUID: " + riuuid.uuid) : color("&cResolution failed."));
			break;
		case "username":
			if (args.length != 2){ sendInvalidUsage(sender, cmd); return true; }
			ResolutionInfo riusername = UUIDResolver.performSyncQuery(new ResolutionInfo(false, null, args[1]));
			sender.sendMessage(riusername.success ? color("&eUsername: " + riusername.username) : color("&cResolution failed."));
			break;
		case "bench":
			Player[] ps = Bukkit.getOnlinePlayers();
			if (ps.length == 0){
				sender.sendMessage(color("&cNo players are currently online."));
				return true;
			}
			Long start = System.currentTimeMillis();
			for (Player p: ps){
				UUIDResolver.performSyncQuery(new ResolutionInfo(false, p.getName().toLowerCase(), null));
			}
			sender.sendMessage(color("&eLookup time for all currently logged in players (username): " + (System.currentTimeMillis() - start) + "/" + ((System.currentTimeMillis() - start) /ps.length )));
			start = System.currentTimeMillis();
			for (Player p: ps){
				UUIDResolver.performSyncQuery(new ResolutionInfo(false, null, UUIDResolver.consistentFormat(p.getUniqueId().toString())));
			}
			sender.sendMessage(color("&eLookup time for all currently logged in players (UUID): " + (System.currentTimeMillis() - start) + "/" + ((System.currentTimeMillis() - start) / ps.length)));
			break;
		case "async-name":
			if (args.length != 2){ sendInvalidUsage(sender, cmd); return true; }
			UUIDResolver.performAsyncNTIDQuery(new ResolutionInfo(false, args[1], null));
			sender.sendMessage(color("&eLookup started for name."));
			break;
		case "async-id":
			if (args.length != 2){ sendInvalidUsage(sender, cmd); return true; }
			UUIDResolver.performAsyncIDTNQuery(new ResolutionInfo(false, null, args[1]));
			sender.sendMessage(color("&eLookup started for UUID."));
			break;
		default:
			sendInvalidUsage(sender, cmd);
			break;
		}
		return true;
	}

}
