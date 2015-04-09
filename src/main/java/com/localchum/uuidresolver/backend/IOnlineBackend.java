package com.localchum.uuidresolver.backend;

import com.localchum.uuidresolver.Callback;
import com.localchum.uuidresolver.MojangProfile;

import java.util.UUID;

/**
 * Created by LocalChum on 12/20/2014.
 */
public interface IOnlineBackend {

    public Runnable lookupUUID(ICacheBackend cache, String usernames, Callback<MojangProfile> callback);

    public Runnable lookupPreviousUsernames(ICacheBackend cache, UUID uuids, Callback<MojangProfile[]> callback);
}
