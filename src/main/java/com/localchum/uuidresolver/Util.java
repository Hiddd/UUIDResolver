package com.localchum.uuidresolver;

import java.util.UUID;

/**
 * Created by LocalChum on 2/2/2015.
 */
public class Util {

    public static String toWebUuid(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    public static UUID fromWebUuid(String uuid) {
        return UUID.fromString(uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20));
    }

    public static void trimAll(String[] split) {
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].trim();
        }
    }
}
