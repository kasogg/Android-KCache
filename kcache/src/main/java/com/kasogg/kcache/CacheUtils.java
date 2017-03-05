package com.kasogg.kcache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * CacheUtils
 * <p>
 * Created by KasoGG on 2017/2/17.
 */
class CacheUtils {
    private static final String TAG = CacheUtils.class.getSimpleName();

    static String hashKey(String key) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(key.getBytes("UTF-8"));
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            return bigInt.toString(16);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return String.valueOf(key.hashCode());
        }
    }

    static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
    }

    static void abortQuietly(DiskLruCache.Editor editor) {
        if (editor != null) {
            try {
                editor.abort();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
    }

    static boolean isExpired(KCache.CacheEntity cacheEntity) {
        return cacheEntity != null && cacheEntity.expiration != 0 && System.currentTimeMillis() > cacheEntity.expiration;
    }

    static int calculateEntitySize(KCache.CacheEntity entity) {
        if (entity == null) {
            return 1;
        }
        return entity.bytes != null ? entity.bytes.length + 8 : 8;
    }

    static byte[] bitmapToBytes(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bytes = baos.toByteArray();
        closeQuietly(baos);
        return bytes;
    }

    static Bitmap bytesToBitmap(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    static byte[] objectToBytes(Object obj) {
        byte[] bytes = null;
        ObjectOutputStream oos = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            bytes = baos.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "", e);
        } finally {
            closeQuietly(oos);
        }
        return bytes;
    }

    static Object bytesToObject(byte[] bytes) {
        Object obj = null;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            obj = ois.readObject();
        } catch (Exception e) {
            Log.e(TAG, "", e);
        } finally {
            closeQuietly(ois);
        }
        return obj;
    }

    static File getCacheChildDir(Context context, String childDirName) {
        File childDir = new File(getCacheDir(context), childDirName);
        if (!childDir.exists()) {
            childDir.mkdirs();
        }
        return childDir;
    }

    static File getCacheDir(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            return context.getExternalCacheDir();
        } else {
            return context.getCacheDir();
        }
    }

    static long getSizeByPath(File path) {
        long size = 0;
        File files[];
        if (path == null || (files = path.listFiles()) == null) {
            return size;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                size += getSizeByPath(file);
            } else {
                size += file.length();
            }
        }
        return size;
    }

    static void clearByPath(File path) {
        File files[];
        if (path == null || (files = path.listFiles()) == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                clearByPath(file);
            } else {
                file.delete();
            }
        }
    }

}
