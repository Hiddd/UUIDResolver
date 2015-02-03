package com.localchum.uuidresolver;

import com.localchum.uuidresolver.backend.CacheBackendImpl;
import com.localchum.uuidresolver.backend.OnlineBackendImpl;

/**
 * Created by LocalChum on 2/3/2015.
 */
public class UuidResolverConfig {

    public String cacheClass;
    public String onlineClass;
    public String saveFile;
    public int autoSaveIntervalMinutes;

    public void verify() {
        if (cacheClass == null) {
            cacheClass = CacheBackendImpl.class.getName();
        }
        if (onlineClass == null) {
            onlineClass = OnlineBackendImpl.class.getName();
        }
        if (saveFile == null) {
            saveFile = "cache.dat";
        }
        if (autoSaveIntervalMinutes == 0) {
            autoSaveIntervalMinutes = -1;
        }
    }
}
