package com.localchum.uuidresolver;

import java.util.UUID;

/**
 * Created by LocalChum on 2/2/2015.
 */
public class MojangProfile {

    public final UUID uuid;
    public final String username;

    public MojangProfile(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

}
