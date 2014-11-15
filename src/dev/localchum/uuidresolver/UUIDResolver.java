package dev.localchum.uuidresolver;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import dev.localchum.uuidresolver.ResolutionInfo.Direction;

public class UUIDResolver extends JavaPlugin implements Listener {
	
	public static final Pattern userPattern = Pattern.compile("^[a-z0-9A-Z_]+");
	
	/*public static boolean validUsername(String user){
		return userPattern.matcher(user).matches();
	}*/
	
	// TODO Username verification a way that works well.
	public static void performAsyncNTIDQuery(final ResolutionInfo info){
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				MojangQueryNTID.resolveAsync(info);
			}
		}, "UUIDResolver - Online Resolution NTID");
		t.start();
	}
	
	public static void performAsyncIDTNQuery(final ResolutionInfo info){
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				MojangQueryIDTN.resolveAsync(info);
			}
		}, "UUIDResolver - Online Resolution IDTN");
		t.start();
	}
	
	public static ResolutionInfo performSyncQuery(final ResolutionInfo info){
		ArrayList<Entry<String, String>> ret = new ArrayList<Entry<String, String>>();
		
		String user = info.username != null ? info.username.toLowerCase() : null;
		for (Entry<String, String> entry: resolutionCacheLower.entrySet()){
			if ((user != null ? user.equals(entry.getValue()) : false) || (info.uuid != null ? info.uuid.equals(entry.getKey()) : false)){
				if (info.username == null){
					// UUIDs are 100% unique whereas names are not
					return new ResolutionInfo(true, resolutionCache.get(entry.getKey()), entry.getKey());
				}
				
				ret.add(entry);
			}
		}
		
		if (ret.size() > 1){
			LOGGER.severe("UUID conflict, deleting entries");
			for (Entry<String, String> u: ret){
				resolutionCache.remove(u.getKey());
				resolutionCacheLower.remove(u.getKey());
				failedUsernameResolutions.add(u.getValue());
				LOGGER.severe("Deleted: " + u.getKey() + ":" + u.getValue());
			}
			return new ResolutionInfo(false, null, null); // Prevent duplicate entries
		}
		
		if (ret.size() == 1){
			Entry<String, String> r = ret.get(0);
			String uuid = r.getKey();
			String username = resolutionCache.get(uuid);
			return new ResolutionInfo(true, username, uuid);
		}
		
		if (ret.size() == 0 && info.getDirection() == Direction.ID_TO_NAME){
			failedUUIDResolutions.add(info.uuid);
		}
		
		return new ResolutionInfo(false, null, null);
	}
	
	public static ResolutionInfo performForceQuery(final ResolutionInfo info){
		ResolutionInfo syncTry = performSyncQuery(info);
		if (syncTry.success){ return syncTry; }
		
		ResolutionInfo blockingTry = MojangQueryNTID.resolveAsync(info);
		return blockingTry;
	}
	
	public static UUID asUUID(String s){
		return UUID.fromString(s.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5"));
	}
	
	// ===================== //
	
	public static Logger LOGGER = null;
	public static UUIDResolver i = null;
	public static File savedCacheLocation = null;
	public static FileConfiguration savedCache = null;
	public static ConcurrentHashMap<String, String> resolutionCache = new ConcurrentHashMap<String, String>();
	public static ConcurrentHashMap<String, String> resolutionCacheLower = new ConcurrentHashMap<String, String>();
	public static Set<String> failedUsernameResolutions = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> failedUUIDResolutions = Collections.synchronizedSet(new HashSet<String>());
	
	public void onEnable(){
		i = this;
		LOGGER = getLogger();
		
		savedCacheLocation = new File(getDataFolder() + File.separator + "resolver-cache.yml");
		if (savedCacheLocation.exists()){
			try {
				savedCache = YamlConfiguration.loadConfiguration(savedCacheLocation);
			} catch (Exception e) { }
			
			if (savedCache != null){
				for (String uuid: savedCache.getKeys(false)){
					String u = savedCache.getString(uuid);
					resolutionCache.put(uuid, u); // Format is UUID: Username
					resolutionCacheLower.put(uuid, u.toLowerCase());
				}
			}
		} else {
			try {
				savedCacheLocation.getParentFile().mkdirs();
				savedCacheLocation.createNewFile();
			} catch (Exception e) {
				LOGGER.severe("Could not create UUID resolution cache file: " + e.toString());
			}
		}
		
		try {
			savedCache = YamlConfiguration.loadConfiguration(savedCacheLocation);
			// Empty
		} catch (Exception e) { }
		
		Bukkit.getPluginManager().registerEvents(this, this);
		
		getCommand("uuidresolver").setExecutor(new ResolverAdmin());
	}
	
	public void onDisable(){
		attemptSave();
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerLogin(PlayerLoginEvent evt){
		if (!getServer().getOnlineMode()){ return; }
		
		String uuid = consistentFormat(evt.getPlayer().getUniqueId().toString());
		String user = evt.getPlayer().getName();
		resolutionCache.put(uuid, user);
		resolutionCacheLower.put(uuid, user.toLowerCase());
	}
	
	public static String consistentFormat(String uuid){
		return uuid.replace("-", "").toLowerCase();
	}
	
	public void attemptSave(){
		long start = System.currentTimeMillis();
		
		for (String s: savedCache.getKeys(false)){
			savedCache.set(s, null);
		}
		
		for (Entry<String, String> entry: resolutionCache.entrySet()){
			savedCache.set(entry.getKey(), entry.getValue());
		}
		
		try {
			savedCache.save(savedCacheLocation);
		} catch (Exception e) {
			LOGGER.severe("Could not save UUID resolution cache to disk: " + e.toString());
		}
		
		LOGGER.info("Saved in " + (System.currentTimeMillis() - start));
	}
	
	public static void performRepairNTID(){
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				int failed = 0;
				List<String> cpy = new ArrayList<String>(failedUsernameResolutions);
				Iterator<String> it = cpy.iterator();
				while (it.hasNext()){
					String u = it.next();
					ResolutionInfo ri = MojangQueryNTID.resolveAsync(new ResolutionInfo(false, u, null));
					if (ri.success){
						//it.remove();
						continue;
					} else {
						failed++;
					}
				}
				
				LOGGER.info(failed + " repair attempts have failed");
			}
		}, "UUIDResolver - Cache NTID Repair");
		t.start();
	}
	
	public static void performRepairIDTN(){
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				int failed = 0;
				List<String> cpy = new ArrayList<String>(failedUUIDResolutions);
				Iterator<String> it = cpy.iterator();
				while (it.hasNext()){
					String u = it.next();
					ResolutionInfo ri = MojangQueryIDTN.resolveAsync(new ResolutionInfo(false, null, u));
					if (ri.success){
						//it.remove();
						continue;
					} else {
						failed++;
					}
				}
				
				LOGGER.info(failed + " repair attempts have failed");
			}
		}, "UUIDResolver - Cache IDTN Repair");
		t.start();
	}
}
