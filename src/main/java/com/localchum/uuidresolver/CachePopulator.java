package com.localchum.uuidresolver;

import java.util.UUID;

/**
 * Created by LocalChum on 2/2/2015.
 */
public class CachePopulator {

    public final ResolverAPI api;

    public CachePopulator(ResolverAPI api) {
        this.api = api;
    }

    /**
     * Called when a player logs in. Must be called from a thread where blocking for an external request is tolerable.
     *
     * @param uuid     The correct Mojang UUID.
     * @param username The current username associated with the UUID.
     */
    public void playerLogin(final UUID uuid, final String username) {
        UUID conflict = api.cache.getConflicting(username, uuid);
        if (conflict != null) {
            // look up correct name for the other player

            api.online.lookupPreviousUsernames(api.cache, conflict, new Callback<MojangProfile[]>() {
                @Override
                public void run(MojangProfile[] obj) {
                    MojangProfile profile = obj[obj.length - 1];
                    api.cache.addEntry(profile.uuid, profile.username);
                }
            }).run(); // resolve synchronously
        }

        api.cache.addEntry(uuid, username);
    }

}
