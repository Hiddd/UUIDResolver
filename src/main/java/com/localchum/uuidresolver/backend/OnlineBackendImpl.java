package com.localchum.uuidresolver.backend;

import com.localchum.uuidresolver.Callback;
import com.localchum.uuidresolver.MojangProfile;
import com.localchum.uuidresolver.Util;
import com.localchum.uuidresolver.UuidResolver;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.util.*;

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
    public Runnable lookupPreviousUsernames(final ICacheBackend cache, final UUID uuid, final Callback<MojangProfile[]> callback) {
        return new Runnable() {
            @Override
            public void run() {
                String result = performHttpsGet(String.format(USERNAME_URL, Util.toWebUuid(uuid)));
                if (result == null) {
                    if (callback != null) {
                        callback.run(null);
                    }
                    return;
                }

                Map<String, Object>[] nameHistory = UuidResolver.gson.fromJson(result, Map[].class);
                if (nameHistory.length == 0){
                    if (callback != null){ callback.run(null); }
                    return;
                }

                List<MojangProfile> profileHistory = new ArrayList<>();

                for (Map<String, Object> o : nameHistory) {
                    Object cta = o.get("changedToAt");
                    String name = (String) o.get("name");
                    long changedAsLong = (cta != null && cta instanceof Double ? ((Double) cta).longValue() : 0);

                    profileHistory.add(
                            new MojangProfile(uuid, name, changedAsLong)
                    );
                }

                Collections.sort(profileHistory, new Comparator<MojangProfile>() {
                    @Override
                    public int compare(MojangProfile o1, MojangProfile o2) {
                        return Long.compare(o1.usernameLastChangedAt, o2.usernameLastChangedAt);
                    }
                });

                cache.addEntry(uuid, profileHistory.get(0).username);

                if (callback != null) {
                    callback.run(profileHistory.toArray(new MojangProfile[0]));
                }
            }
        };
    }

}
