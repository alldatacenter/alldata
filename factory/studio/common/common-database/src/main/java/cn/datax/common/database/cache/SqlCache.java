package cn.datax.common.database.cache;

import cn.datax.common.database.utils.MD5Util;

import java.util.Arrays;

/**
 * SQL缓存接口
 */
public interface SqlCache {

    /**
     * 计算key
     */
    default String buildSqlCacheKey(String sql, Object[] args) {
        return MD5Util.encrypt(sql + ":" + Arrays.toString(args));
    }

    /**
     * 存入缓存
     * @param key   key
     * @param value 值
     */
    void put(String key, Object value, long ttl);

    /**
     * 获取缓存
     * @param key   key
     * @return
     */
    <T> T get(String key);

    /**
     * 删除缓存
     * @param key  key
     */
    void delete(String key);
}
