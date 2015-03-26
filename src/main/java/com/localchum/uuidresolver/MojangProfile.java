package com.localchum.uuidresolver;

import java.util.UUID;

/**
 * Created by LocalChum on 2/2/2015.
 */
public class MojangProfile {

    public final UUID uuid;
    public final String username;

    public final long usernameLastChangedAt;

    public MojangProfile(UUID uuid, String username, long lastChanged) {
        this.uuid = uuid;
        this.username = username;
        this.usernameLastChangedAt = lastChanged;
    }

    public MojangProfile(UUID uuid, String username) {
        this(uuid, username, 0);
    }

}
