package dev.localchum.uuidresolver;

import com.localchum.uuidresolver.*;

import java.util.UUID;

/**
 * Legacy Bridge
 *
 * For the current, clean API, please look at ResolverAPI.
 */
public class UUIDResolver {

    public static ResolutionInfo performSyncQuery(final ResolutionInfo info) {
        ResolverAPI api = UuidResolver.get();

        switch (info.getDirection()) {
            case ID_TO_NAME:
                String username = api.getUsernameSync(asUUID(info.uuid));
                return new ResolutionInfo(username != null, username, info.uuid);
            case NAME_TO_ID:
                UUID uuid = api.getUUIDSync(info.username);
                String name = api.getUsernameSync(uuid);
                return new ResolutionInfo(uuid != null && name != null, name, uuid != null ? consistentFormat(uuid.toString()) : null);
        }

        throw new IllegalStateException();
    }

    private static ResolutionInfo nameToIdForce(ResolutionInfo info) {
        ResolverAPI api = UuidResolver.get();

        UUID uuid = api.getUUIDSync(info.username);
        if (uuid != null) {
            return new ResolutionInfo(true, api.getUsernameSync(uuid), consistentFormat(uuid.toString()));
        } else {
            final ResolutionInfo ret = new ResolutionInfo(false, null, null);
            api.unsafeOnline().lookupUUID(api.unsafeCache(), info.username, new Callback<MojangProfile>() {
                @Override
                public void run(MojangProfile obj) {
                    ret.uuid = obj.uuid != null ? consistentFormat(obj.uuid.toString()) : null;
                    ret.username = obj.username;
                }
            }).run();

            ret.success = ret.username != null && ret.uuid != null;
            return ret;
        }
    }

    private static ResolutionInfo idToNameForce(ResolutionInfo info) {
        ResolverAPI api = UuidResolver.get();

        UUID uuid = asUUID(info.uuid);
        String username = api.getUsernameSync(asUUID(info.uuid));
        if (username != null) {
            return new ResolutionInfo(true, username, info.uuid);
        } else {
            final ResolutionInfo ret = new ResolutionInfo(false, null, info.uuid);
            api.unsafeOnline().lookupUsername(api.unsafeCache(), uuid, new Callback<MojangProfile>() {
                @Override
                public void run(MojangProfile obj) {
                    ret.username = obj.username;
                }
            }).run();

            ret.success = ret.username != null;
            return ret;
        }
    }

    public static ResolutionInfo performForceQuery(final ResolutionInfo info) {
        switch (info.getDirection()) {
            case ID_TO_NAME:
                return idToNameForce(info);
            case NAME_TO_ID:
                return nameToIdForce(info);
        }

        throw new IllegalStateException();
    }

    public static UUID asUUID(String s) {
        return Util.fromWebUuid(s.replace("-", ""));
    }

    public static String consistentFormat(String uuid) {
        return uuid.replace("-", "");
    }

}