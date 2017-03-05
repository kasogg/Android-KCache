# KCache

KCache是一个轻量级缓存，内部采用LruCache，实现了内存和磁盘二级缓存管理，提高缓存获取效率，支持自定义缓存路径。

## 用法

```java
KCache kCache = new KCache(context, appVersion, MAX_SIZE); // 可自定义appVersion，改变后Disk缓存失效
KCache kCache = new KCache(cacheDir, appVersion, MAX_SIZE); // 可自定义缓存目录

kCache.put("key", "value"); // 写入缓存
kCache.put("key", "value", System.currentTimeMillis() + 5000); // 指定过期时间，5秒后过期

String value = kCache.getAsString("key"); // 获取指定key
```

KCache支持String、Serializable、Bitmap、byte[]写入和读取操作。
