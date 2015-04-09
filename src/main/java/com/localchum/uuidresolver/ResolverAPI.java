package com.localchum.uuidresolver;

import com.localchum.uuidresolver.backend.ICacheBackend;
import com.localchum.uuidresolver.backend.IOnlineBackend;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by LocalChum on 12/20/2014.
 */
public class ResolverAPI {

    private final ExecutorService lookupPool = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("UUID/Username Online Processing Thread");
            t.setDaemon(true);
            return t;
        }
    });

    final ICacheBackend cache;
    final IOnlineBackend online;

    public ResolverAPI(String cacheClass, String onlineClass) {
        try {
            cache = (ICacheBackend) Class.forName(cacheClass).newInstance();
            online = (IOnlineBackend) Class.forName(onlineClass).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Sync Lookups
     */

    public UUID getUUIDSync(String username) {
        if (username == null) {
            return null;
        }

        return cache.getUUID(username);
    }

    public String getUsernameSync(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        return cache.getUsername(uuid);
    }

    /*
     * Async Lookups
    */

    public void getUUIDAsync(String username, Callback<MojangProfile> callback) {
        lookupPool.submit(online.lookupUUID(cache, username, callback));
    }

    public void getUsernameAsync(UUID uuid, final Callback<MojangProfile> callback) {
        lookupPool.submit(online.lookupPreviousUsernames(cache, uuid, new Callback<MojangProfile[]>() {
            @Override
            public void run(MojangProfile[] obj) {
                callback.run(obj[obj.length - 1]);
            }
        }));
    }

    public void getPreviousUsernamesAsync(UUID uuid, final Callback<MojangProfile[]> callback) {
        lookupPool.submit(online.lookupPreviousUsernames(cache, uuid, callback));
    }

    /*
     * Unsafe Access
     */

    public ICacheBackend unsafeCache() {
        return cache;
    }

    public IOnlineBackend unsafeOnline() {
        return online;
    }

}
