package dev.localchum.uuidresolver;

/**
 * Legacy Bridge
 */
public class ResolutionInfo {
    public boolean success = false;
    public String username = null;
    public String uuid = null;
    // Async only
    public ResolverRunnable syncCompletionCallback = null;
    public ResolverRunnable asyncCompletionCallback = null;

    public static enum Direction {
        NAME_TO_ID,
        ID_TO_NAME,
        UNKNOWN
    }

    public ResolutionInfo(boolean s, String name, String id) {
        success = s;
        username = name;
        uuid = id;
    }

    public String toString() {
        return success + ":" + username + ":" + uuid;
    }

    public Direction getDirection() {
        if (uuid == null && username != null) {
            return Direction.NAME_TO_ID;
        }

        if (uuid != null && username == null) {
            return Direction.ID_TO_NAME;
        }

        return Direction.UNKNOWN;
    }
}
