package com.localchum.uuidresolver.backend;

import com.localchum.uuidresolver.Callback;
import com.localchum.uuidresolver.MojangProfile;
import com.localchum.uuidresolver.Util;
import com.localchum.uuidresolver.UuidResolver;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

/**
 * Created by LocalChum on 12/22/2014.
 */
public class OnlineBackendImpl implements IOnlineBackend {

    public static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
    public static final String USERNAME_URL = "https://api.mojang.com/user/profiles/%s/names";

    private static final ThreadLocal<byte[]> reusableBuffers = new ThreadLocal<byte[]>() {
        @Override
        public byte[] initialValue() {
            return new byte[0x1000];
        }
    };

    public static String performHttpsGet(String url) {
        try {
            HttpsURLConnection c = (HttpsURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            c.setDoInput(true);
            if (c.getResponseCode() != 200) {
                return null;
            }

            byte[] buf = reusableBuffers.get();
            StringBuilder builder = new StringBuilder();

            int amount;
            while ((amount = c.getInputStream().read(buf)) > 0) {
                builder.append(new String(buf, 0, amount));
            }

            return builder.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Runnable lookupUUID(final ICacheBackend cache, final String username, final Callback<MojangProfile> callback) {
        return new Runnable() {
            @Override
            public void run() {
                String result = performHttpsGet(String.format(UUID_URL, username));
                if (result == null) {
                    if (callback != null) {
                        callback.run(null);
                    }
                    return;
                }

                Map<String, String> map = UuidResolver.gson.fromJson(result, Map.class);
                if (map == null) {
                    if (callback != null) {
                        callback.run(null);
                    }
                    return;
                }

                UUID uuid = Util.fromWebUuid(map.get("id"));
                String name = map.get("name");

                cache.addEntry(uuid, name);
                if (callback != null) {
                    callback.run(new MojangProfile(Util.fromWebUuid(map.get("id")), map.get("name")));
                }
            }

        };
    }

    @Override
    public Runnable lookupUsername(final ICacheBackend cache, final UUID uuid, final Callback<MojangProfile> callback) {
        return new Runnable() {
            @Override
            public void run() {
                String result = performHttpsGet(String.format(USERNAME_URL, uuid));
                if (result == null) {
                    if (callback != null) {
                        callback.run(null);
                    }
                    return;
                }

                Map<String, Object>[] obj = UuidResolver.gson.fromJson(result, Map[].class);

                System.out.println(result);
                System.out.println(obj);
                if (obj.length > 0) {
                    String name = (String) obj[0].get("name");

                    cache.addEntry(uuid, name);
                    if (callback != null) {
                        callback.run(new MojangProfile(uuid, name));
                    }
                }

            }
        };
    }

}
