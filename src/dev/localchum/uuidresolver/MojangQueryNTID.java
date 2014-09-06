package dev.localchum.uuidresolver;
import org.bukkit.Bukkit;
import dev.localchum.com.mojang.api.profiles.HttpProfileRepository;
import dev.localchum.com.mojang.api.profiles.Profile;
import dev.localchum.com.mojang.api.profiles.ProfileCriteria;

public class MojangQueryNTID {

	public static HttpProfileRepository repo = new HttpProfileRepository();
	public static final String AGENT = "minecraft";

	public static ResolutionInfo resolveAsync(ResolutionInfo info){
		ResolutionInfo ret = null;
		
		if (info.username != null){
		 	Profile[] profiles = repo.findProfilesByCriteria(new ProfileCriteria(info.username, AGENT));
			if (profiles.length > 1){ UUIDResolver.LOGGER.severe("Unexpected length of " + profiles.length + " in online resolution. Selecting first profile."); }
			Profile prof = profiles.length > 0 ? profiles[0] : null;
			
			if (prof != null){
				String uuid = UUIDResolver.consistentFormat(prof.getId());
				String username = prof.getName();
				UUIDResolver.resolutionCache.put(uuid, username);
				UUIDResolver.resolutionCacheLower.put(uuid, username.toLowerCase());
				if (UUIDResolver.failedUsernameResolutions.contains(username.toLowerCase())){
					UUIDResolver.failedUsernameResolutions.remove(username.toLowerCase());
				}
			}
			
			ret = new ResolutionInfo(prof != null, prof != null ? prof.getName() : null, prof != null ? prof.getId() : null);
			ret.syncCompletionCallback = info.syncCompletionCallback;
			ret.asyncCompletionCallback = info.asyncCompletionCallback;
		} else {
			ret = new ResolutionInfo(false, null, null);
		}
		
		if (ret.syncCompletionCallback != null){
			ret.syncCompletionCallback.resolutionInfo = ret;
			Bukkit.getScheduler().scheduleSyncDelayedTask(UUIDResolver.i, ret.syncCompletionCallback);
		}
		
		if (ret.asyncCompletionCallback != null){
			ret.asyncCompletionCallback.resolutionInfo = ret;
			ret.asyncCompletionCallback.run(); // We're already in a different thread.
		}
		
		return ret;
	}
	
}
