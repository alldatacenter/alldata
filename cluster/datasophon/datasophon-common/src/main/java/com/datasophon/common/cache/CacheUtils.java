package com.datasophon.common.cache;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache工具类
 */
public class CacheUtils {

    private static Logger logger = LoggerFactory.getLogger(CacheUtils.class);
    private static Cache<String, Object> cache = CacheUtil.newLRUCache(4096);

   public static Object get(String key){
       Object data = cache.get(key);
       return data;
   };

   public static void put(String key,Object value){
       cache.put(key,value);
   }

   public static boolean constainsKey(String key){
       return cache.containsKey(key);
   }

   public static void  removeKey(String key){
       cache.remove(key);
   }


    public static Integer getInteger(String key) {
        Object data = cache.get(key);
        return (Integer)data;
    }

    public static Boolean getBoolean(String key) {
        Object data = cache.get(key);
        return (Boolean) data;
    }

    public static String getString(String key) {
        Object data = cache.get(key);
        return (String) data;
    }

}