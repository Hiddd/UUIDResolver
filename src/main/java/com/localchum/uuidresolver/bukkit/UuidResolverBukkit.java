package com.localchum.uuidresolver.bukkit;

import com.localchum.uuidresolver.ResolverAPI;
import com.localchum.uuidresolver.Util;
import com.localchum.uuidresolver.UuidResolver;
import com.localchum.uuidresolver.UuidResolverConfig;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.UUID;

/**
 * Created by LocalChum on 2/3/2015.
 */
public class UuidResolverBukkit extends JavaPlugin implements Listener {

    private UuidResolver instance;

    @Override
    public void onEnable() {
        try {
            /*
             * Load Config
             */
            File configFile = new File(getDataFolder() + File.separator + "config.json");
            if (!configFile.exists()) {
                configFile.createNewFile();
            }

            FileReader fr = new FileReader(configFile);
            UuidResolverConfig config = UuidResolver.gson.fromJson(fr, UuidResolverConfig.class);
            fr.close();

            config.verify();

            /*
             * Initialize UuidResolver
             */

            instance = new UuidResolver(config, new File(getDataFolder() + File.separator + config.saveFile));
            ResolverAPI api = UuidResolver.get();

            /*
             * Import legacy UUIDResolver format
             */

            File legacy = new File(getDataFolder() + File.separator + "resolver-cache.yml");
            if (legacy.exists()) {
                File legacyRename = new File(getDataFolder() + File.separator + "resolver-cache.yml.imported");
                BufferedReader br = new BufferedReader(new FileReader(legacy));
                String line;

                while ((line = br.readLine()) != null) {
                    String[] split = line.split(":");
                    Util.trimAll(split);

                    UUID uuid = Util.fromWebUuid(split[0]);
                    String username = split[1];

                    api.unsafeCache().addEntry(uuid, username);
                }

                br.close();

                legacy.renameTo(legacyRename);
            }

            /*
             * Autosave Scheduling
             */

            if (config.autoSaveIntervalMinutes != -1) {
                Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
                    @Override
                    public void run() {
                        instance.save();
                    }
                }, config.autoSaveIntervalMinutes * 20 * 60, config.autoSaveIntervalMinutes * 20 * 60);
            }

            /*
             * Listener
             */

            Bukkit.getPluginManager().registerEvents(this, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        if (instance != null) {
            instance.save();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        instance.onLoginAllowBlocking(event.getUniqueId(), event.getName());
    }
}
