package com.localchum.uuidresolver.backend;

import java.io.File;
import java.util.UUID;

/**
 * Created by LocalChum on 12/20/2014.
 */
public interface ICacheBackend {

    public void load(File file) throws Exception;

    public void save(File file) throws Exception;

    public UUID getUUID(String name);

    public String getUsername(UUID uuid);

    public void addEntry(UUID uuid, String name);

    public void purge(int days);

    public UUID getConflicting(String username, UUID expected);

}
