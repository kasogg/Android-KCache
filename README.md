# KCache

KCache是一个轻量级缓存框架，内设二级缓存，采用MemroyLruCache和DiskLruCache实现，轻便简单易用。

## 特性
* 支持String、Serializable、Bitmap、byte[]写入和读取操作
* 支持自定义缓存路径
* 支持自定义过期时间

## 用法
```java
KCache kCache = new KCache(context, appVersion, MAX_SIZE); // 可自定义appVersion，改变后Disk缓存失效
KCache kCache = new KCache(cacheDir, appVersion, MAX_SIZE); // 可自定义缓存目录

kCache.put("key", "value"); // 写入缓存
kCache.put("key", "value", System.currentTimeMillis() + 5000); // 指定过期时间，5秒后过期

String value = kCache.getAsString("key"); // 获取指定key

long size = kCache.size(); // 获取磁盘缓存大小
kCache.remove("key"); // 移除指定key
kCache.clear(); // 清除缓存
```

更多用法请参考Demo。
