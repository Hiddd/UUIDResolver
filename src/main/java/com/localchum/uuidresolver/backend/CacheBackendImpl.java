package com.localchum.uuidresolver.backend;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by LocalChum on 12/20/2014.
 */
public class CacheBackendImpl implements ICacheBackend {

    /*
     * byte - Version ID
     * int - # entries
     *
     * {
     *   MSB, LSB longs - UUID
     *   byte - username length
     *   UTF8 encoded string bytes - username
     *   int - System.currentTimeMillis() / 1000 / 86400 last access
     * }
     */

    public static final byte FILE_VERSION_HEADER = 0x01;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Object ioLock = new Object();

    // dual hashmap for quick lookups in both directions
    private final Map<UUID, String> entries = new HashMap<>();
    private final Map<String, UUID> lowerLookup = new HashMap<>();

    private final TObjectIntHashMap<UUID> times = new TObjectIntHashMap<>();

    public int currentExpiryTime() {
        return (int) (System.currentTimeMillis() / 1000 / 86400);
    }

    public void insert(UUID uuid, String username, int time) {
        entries.put(uuid, username);
        lowerLookup.put(username.toLowerCase(), uuid);

    }

    @Override
    public void load(File file) throws Exception {
        synchronized (ioLock) {
            if (!file.exists()) {
                file.createNewFile();
            }

            lock.writeLock().lock();

            DataInputStream in = new DataInputStream(new FileInputStream(file));
            int ver = in.readByte();
            if (ver == -1) {
                return;
            } // EOF

            if (ver != FILE_VERSION_HEADER) {
                throw new IllegalStateException("File version mismatch");
            }

            byte[] userBytes = new byte[16]; // with the characters in usernames, chars --> utf8 bytes is always 1:1
            for (int i = 0; i < in.readInt(); i++) {
                long msb = in.readLong();
                long lsb = in.readLong();
                UUID uuid = new UUID(msb, lsb);

                byte userLen = in.readByte();
                in.read(userBytes);
                String username = new String(userBytes, 0, userLen, StandardCharsets.UTF_8);

                int time = in.readInt();

                insert(uuid, username, time);
            }

            in.close();

            lock.writeLock().unlock();
        }
    }

    @Override
    public void save(File file) throws Exception {
        synchronized (ioLock) {
            if (!file.exists()) {
                file.createNewFile();
            }

            lock.readLock().lock();

            DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
            out.write(FILE_VERSION_HEADER);

            byte[] userBytes;
            for (Map.Entry<UUID, String> e : entries.entrySet()) {
                UUID uuid = e.getKey();
                String username = e.getValue();

                out.writeLong(uuid.getMostSignificantBits());
                out.writeLong(uuid.getLeastSignificantBits());

                userBytes = username.getBytes(StandardCharsets.UTF_8);
                if (userBytes.length > 16) {
                    throw new IllegalStateException("Username bytes > 16? Not a valid username?");
                }
                out.writeByte(userBytes.length);
                out.write(userBytes);

                out.writeInt(times.get(uuid));
            }

            out.flush();
            out.close();

            lock.readLock().unlock();
        }
    }

    @Override
    public UUID getUUID(String name) {
        lock.writeLock().lock();

        UUID id = lowerLookup.get(name.toLowerCase());
        times.put(id, currentExpiryTime());

        lock.writeLock().unlock();
        return id;
    }

    @Override
    public String getUsername(UUID uuid) {
        lock.writeLock().lock();

        String name = entries.get(uuid);
        times.put(uuid, currentExpiryTime());

        lock.writeLock().unlock();
        return name;
    }

    @Override
    public void addEntry(UUID uuid, String name) {
        lock.writeLock().lock();

        insert(uuid, name, currentExpiryTime());

        lock.writeLock().unlock();
    }

    @Override
    public void purge(int days) {
        lock.writeLock().lock();

        Iterator<UUID> it = entries.keySet().iterator();
        int curDays = currentExpiryTime();

        while (it.hasNext()) {
            UUID next = it.next();
            if (curDays - times.get(next) > days) {
                it.remove();
                times.remove(next);
            }
        }

        lock.writeLock().unlock();
    }

    public UUID getConflicting(String username, UUID expected) {
        lock.readLock().lock();

        UUID uuid = lowerLookup.get(username);

        lock.readLock().unlock();

        return !expected.equals(uuid) ? uuid : null;
    }

}