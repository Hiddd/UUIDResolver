package com.localchum.uuidresolver;

import com.google.gson.Gson;

import java.io.File;
import java.util.UUID;

/**
 * Created by LocalChum on 2/3/2015.
 */
public class UuidResolver {

    public static final Gson gson = new Gson();

    private static volatile ResolverAPI api = null;

    public static ResolverAPI get() {
        return api;
    }

    private final CachePopulator populator;
    private final UuidResolverConfig config;
    private final File file;

    public UuidResolver(UuidResolverConfig config, File file) {
        api = new ResolverAPI(config.cacheClass, config.onlineClass);
        this.config = config;
        this.file = file;
        this.populator = new CachePopulator(api);

        load();
    }

    public void onLoginAllowBlocking(UUID uuid, String username) {
        populator.playerLogin(uuid, username);
    }

    public void load() {
        try {
            api.unsafeCache().load(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            api.unsafeCache().save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
