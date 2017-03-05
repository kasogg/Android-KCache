package com.kasogg.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * KCache
 * <p>
 * Usage：
 * <p>
 * {@link #put(String, String)}
 * {@link #put(String, String, long)}
 * {@link #put(String, byte[])}
 * {@link #put(String, byte[], long)}
 * {@link #put(String, Serializable)}
 * {@link #put(String, Serializable, long)}
 * {@link #put(String, Bitmap)}
 * {@link #put(String, Bitmap, long)}
 * <p>
 * {@link #getAsString(String)}
 * {@link #getAsBytes(String)}
 * {@link #getAsSerializable(String)}
 * {@link #getAsBitmap(String)}
 * <p>
 * {@link #remove(String)}
 * {@link #clear()}
 * {@link #size()}
 * <p>
 * Created by KasoGG on 2017/2/17.
 */
public class KCache {
    private static final int INDEX = 0;
    private static final String CACHE_DIR_NAME = "KCache";

    private DiskLruCache diskLruCache;
    private LruCache<String, CacheEntity> memoryLruCache;
    private int appVersion;
    private File cacheDir;

    public KCache(Context context, int appVersion, long maxSize) {
        this.appVersion = appVersion;
        this.cacheDir = CacheUtils.getCacheChildDir(context, CACHE_DIR_NAME);
        initDiskLruCache(cacheDir, maxSize);
        initMemoryLruCache();
    }

    public KCache(File dir, int appVersion, long maxSize) {
        this.appVersion = appVersion;
        this.cacheDir = dir;
        initDiskLruCache(dir, maxSize);
        initMemoryLruCache();
    }

    private synchronized void initDiskLruCache(File dir, long maxSize) {
        if (diskLruCache == null) {
            try {
                diskLruCache = DiskLruCache.open(dir, appVersion, 1, maxSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void initMemoryLruCache() {
        if (memoryLruCache == null) {
            int maxSize = (int) (Runtime.getRuntime().maxMemory() / 8);
            memoryLruCache = new LruCache<String, CacheEntity>(maxSize) {
                @Override
                protected int sizeOf(String key, CacheEntity entity) {
                    return CacheUtils.calculateEntitySize(entity);
                }
            };
        }
    }

    public void put(String key, String value) {
        put(key, value, CacheEntity.NO_EXPIRATION);
    }

    public void put(String key, String value, long expiration) {
        put(key, value.getBytes(), expiration);
    }

    public void put(String key, Bitmap bitmap) {
        put(key, bitmap, CacheEntity.NO_EXPIRATION);
    }

    public void put(String key, Bitmap bitmap, long expiration) {
        put(key, CacheUtils.bitmapToBytes(bitmap), expiration);
    }

    public void put(String key, Serializable serializable) {
        put(key, serializable, CacheEntity.NO_EXPIRATION);
    }

    public void put(String key, Serializable serializable, long expiration) {
        put(key, CacheUtils.objectToBytes(serializable), expiration);
    }

    public void put(String key, byte[] bytes) {
        put(key, bytes, CacheEntity.NO_EXPIRATION);
    }

    public void put(String key, byte[] bytes, long expiration) {
        if (bytes == null) {
            return;
        }
        CacheEntity cacheEntity = new CacheEntity(bytes, expiration);
        put(key, cacheEntity);
    }

    private void put(String key, CacheEntity cacheEntity) {
        String hashedKey = CacheUtils.hashKey(key);
        DiskLruCache.Editor editor = null;
        ObjectOutputStream oos = null;
        try {
            editor = diskLruCache.edit(hashedKey);
            if (editor == null) {
                return;
            }
            oos = new ObjectOutputStream(editor.newOutputStream(INDEX));
            oos.writeObject(cacheEntity);
            editor.commit();
        } catch (IOException e) {
            e.printStackTrace();
            CacheUtils.abortQuietly(editor);
        } finally {
            CacheUtils.closeQuietly(oos);
        }
    }

    public String getAsString(String key) {
        byte[] bytes = getAsBytes(key);
        return bytes != null ? new String(bytes) : null;
    }

    public Serializable getAsSerializable(String key) {
        byte[] bytes = getAsBytes(key);
        return bytes != null ? (Serializable) CacheUtils.bytesToObject(bytes) : null;
    }

    public Bitmap getAsBitmap(String key) {
        byte[] bytes = getAsBytes(key);
        return bytes != null ? CacheUtils.bytesToBitmap(bytes) : null;
    }

    public byte[] getAsBytes(String key) {
        CacheEntity cacheEntity = getAsCacheEntity(key);
        return cacheEntity != null ? cacheEntity.bytes : null;
    }

    private CacheEntity getAsCacheEntity(String key) {
        String hashedKey = CacheUtils.hashKey(key);
        CacheEntity cacheEntity = memoryLruCache.get(hashedKey);
        boolean notInMemory = cacheEntity == null;
        if (notInMemory) {
            ObjectInputStream ois = null;
            try {
                DiskLruCache.Snapshot snapshot = diskLruCache.get(hashedKey);
                if (snapshot == null) {
                    return null;
                }
                ois = new ObjectInputStream(snapshot.getInputStream(INDEX));
                cacheEntity = (CacheEntity) ois.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                CacheUtils.closeQuietly(ois);
            }
        }
        if (CacheUtils.isExpired(cacheEntity)) {
            try {
                memoryLruCache.remove(hashedKey);
                diskLruCache.remove(hashedKey);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        } else if (cacheEntity != null && notInMemory) {
            memoryLruCache.put(hashedKey, cacheEntity);
        }
        return cacheEntity;
    }

    public synchronized void remove(String key) {
        memoryLruCache.remove(key);
        try {
            diskLruCache.remove(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void clear() {
        memoryLruCache.evictAll();
        File dir = diskLruCache.getDirectory();
        long maxSize = diskLruCache.maxSize();
        try {
            diskLruCache.delete();
            diskLruCache = DiskLruCache.open(dir, appVersion, 1, maxSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long size() {
        return CacheUtils.getSizeByPath(cacheDir);
    }

    static class CacheEntity implements Serializable {
        private static final long serialVersionUID = -5137353261556372905L;
        private static final int NO_EXPIRATION = 0;

        byte[] bytes;
        long expiration;

        private CacheEntity(byte[] bytes, long expiration) {
            this.bytes = bytes;
            this.expiration = expiration;
        }
    }
}
