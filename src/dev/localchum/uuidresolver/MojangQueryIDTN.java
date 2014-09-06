package dev.localchum.uuidresolver;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.Bukkit;

import dev.localchum.com.google.gson.Gson;

public class MojangQueryIDTN {
	
	public static Gson gson = new Gson();

	public static ResolutionInfo resolveAsync(ResolutionInfo info){
		ResolutionInfo ret = null;
		
		if (info.uuid != null){
			// TODO: No UUID validation is in place at the moment.
			try {
				HttpsURLConnection con = (HttpsURLConnection) new URL("https://api.mojang.com/user/profiles/" + info.uuid + "/names").openConnection();
				con.setDoInput(true);
				con.setUseCaches(false);
				
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				StringBuffer r = new StringBuffer();
				
				String l;
				while ((l = br.readLine()) != null){
					r.append(l);
					r.append("\n");
				}
				
				String[] names = gson.fromJson(r.toString(), String[].class);
				// TODO: !!! WARNING !!! ASSUMING FIRST ENTRY IS THEIR CURRENT NAME
				// TODO: POLISH AND CHECKS!
				
				UUIDResolver.resolutionCache.put(info.uuid, names[0]);
				UUIDResolver.resolutionCacheLower.put(info.uuid, names[0].toLowerCase());
				if (UUIDResolver.failedUUIDResolutions.contains(info.uuid)){
					UUIDResolver.failedUUIDResolutions.remove(info.uuid);
				}
				
				ret = new ResolutionInfo(true, names[0], info.uuid);
			} catch (Exception e){
				e.printStackTrace();
				ret = new ResolutionInfo(false, null, null);
			}
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
