package com.alibaba.tesla.tkgone.server.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.config.ApplicationProperties;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author yangjinghua
 */
@Service
@Log4j
public class RedisHelper {

    @Autowired
    ApplicationProperties applicationProperties;

    private static Map<String, JedisPool> jedisPoolMap = new ConcurrentHashMap<>();

    LoadingCache<String, Set<String>> expireKeysWithStartCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(1, TimeUnit.MINUTES).maximumSize(10000L).build(new CacheLoader<String, Set<String>>() {
                @Override
                public Set<String> load(String s) throws Exception {
                    return keysCache(s);
                }
            });
    LoadingCache<String, String> expireGetCache = CacheBuilder.newBuilder().refreshAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000L).build(new CacheLoader<String, String>() {
                @Override
                public String load(String s) throws Exception {
                    return getCache(s);
                }
            });
    LoadingCache<String, Set<String>> expireZrangeCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(1, TimeUnit.MINUTES).maximumSize(10000L).build(new CacheLoader<String, Set<String>>() {
                @Override
                public Set<String> load(String s) throws Exception {
                    return zrangeCache(s);
                }
            });

    public static JedisPool getJedisPool(String host, int port, String password, int database) {
        String key = String.format("%s:%s/%s?password=%s", host, port, database, password);
        JedisPool jedisPool = jedisPoolMap.get(key);
        if (jedisPool == null) {
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxTotal(1024);
            jedisPoolConfig.setMaxIdle(128);
            jedisPoolConfig.setMaxWaitMillis(30 * 1000);
            jedisPool = new JedisPool(jedisPoolConfig, host, port, 0, password, database);
            jedisPoolMap.put(key, jedisPool);
        }
        return jedisPool;
    }

    static Jedis getJedis(String host, int port, String password, int database) {
        return getJedisPool(host, port, password, database).getResource();
    }

    public Jedis getJedis() {
        return getJedis(applicationProperties.getRedisHost(), applicationProperties.getRedisPort(),
                applicationProperties.getRedisPwd(), applicationProperties.getRedisDb());
    }

    public Long lpush(String key, JSONObject jsonObject) {
        return lpush(key, JSONObject.toJSONString(jsonObject));
    }

    public Long lpush(String key, String value) {
        Jedis jedis = getJedis();
        Long ret = jedis.lpush(key, value);
        jedis.close();
        return ret;
    }

    public String rpop(String key) {
        Jedis jedis = getJedis();
        String ret = jedis.rpop(key);
        jedis.close();
        return ret;
    }

    public Long del(String key) {
        Jedis jedis = getJedis();
        Long ret = jedis.del(key);
        jedis.close();
        return ret;
    }

    public Long delAll(String key) {
        Jedis jedis = getJedis();
        key = key + "::::*";
        Set<String> keys = jedis.keys(key);
        Long ret = 0L;
        if (!keys.isEmpty()) {
            ret = jedis.del(keys.toArray(new String[0]));
        }
        jedis.close();
        return ret;
    }

    public Long expire(String key, int timeout) {
        Jedis jedis = getJedis();
        Long ret = jedis.expire(key, timeout);
        jedis.close();
        return ret;
    }

    public Set<String> hkeys(String key) {
        Jedis jedis = getJedis();
        Set<String> value = jedis.hkeys(key);
        jedis.close();
        return value == null ? new HashSet<>() : value;
    }

    private Set<String> keysCache(String key) {
        Jedis jedis = getJedis();
        Set<String> value = jedis.keys(key);
        jedis.close();
        return value == null ? new HashSet<>() : value;
    }

    public Set<String> keys(String key) {
        try {
            return expireKeysWithStartCache.get(key);
        } catch (ExecutionException e) {
            log.error("keys caught an execution exception", e);
            return new HashSet<>();
        }
    }

    public String set(String key, String value, int timeout) {
        Jedis jedis = getJedis();
        String ret = jedis.set(key, value);
        jedis.expire(key, timeout);
        jedis.close();
        return ret;
    }

    private String getCache(String key) {
        Jedis jedis = getJedis();
        String ret = jedis.get(key);
        jedis.close();
        return ret == null ? "" : ret;
    }

    public String get(String key) {
        try {
            return expireGetCache.get(key);
        } catch (ExecutionException e) {
            log.error("get caught an execution exception", e);
            return "";
        }
    }

    public String hset(String key, String field, String value, int timeout) {
        key = key + "::::" + field;
        return set(key, value, timeout);
    }

    public String hget(String key, String field) {
        key = key + "::::" + field;
        return get(key);
    }

    public Map<String, String> hgetAll(String key) {
        Map<String, String> retMap = new HashMap<>();
        Set<String> keyFields = keys(key + "::::*");
        for (String keyField : keyFields) {
            String field = keyField.split("::::")[1];
            String value = hget(key, field);
            retMap.put(field, value);
        }
        return retMap;
    }

    public JSONArray hgetJSONArray(String key, String field) {
        String value = hget(key, field);
        JSONArray retArray;
        try {
            retArray = JSONArray.parseArray(value);
        } catch (Exception ignored) {
            retArray = new JSONArray();
        }
        return retArray == null ? new JSONArray() : retArray;
    }

    public <T> List<T> hgetList(String key, String field, Class<T> clazz) {
        String value = hget(key, field);
        if (value.equals("")) {
            return new ArrayList<>();
        }
        List<T> ret;
        try {
            ret = JSONArray.parseArray(value).toJavaList(clazz);
        } catch (Exception e) {
            ret = new ArrayList<>();
            log.error("hgetList 错误: ", e);
            log.error("value: " + value);
        }

        return ret == null ? new ArrayList<>() : ret;
    }

    public <T> Map<String, List<T>> hgetAllList(String key, Class<T> clazz) {
        Map<String, String> value = hgetAll(key);
        Map<String, List<T>> ret = new HashMap<>(0);
        for (String field : value.keySet()) {
            ret.put(field, JSONArray.parseArray(value.get(field)).toJavaList(clazz));
        }
        return ret;
    }

    public Long zadd(String key, String member) {
        Jedis jedis = getJedis();
        Long ret = jedis.zadd(key, Tools.currentTimestamp(), member);
        jedis.close();
        return ret;
    }

    private Set<String> zrangeCache(String key) {
        Jedis jedis = getJedis();
        Set<String> value = jedis.zrangeByScore(key, Tools.currentTimestamp() - 5000, Double.MAX_VALUE);
        jedis.close();
        return value == null ? new HashSet<>() : value;
    }

    public Set<String> zrange(String key) {
        try {
            return expireZrangeCache.get(key);
        } catch (ExecutionException e) {
            log.error("zrange caught an execution exception", e);
            return new HashSet<>();
        }
    }

    public Long zremove(String key, Long timeout) {
        Jedis jedis = getJedis();
        Long ret = jedis.zremrangeByScore(key, Double.MIN_VALUE, Tools.currentTimestamp() - timeout);
        jedis.close();
        return ret;
    }
}
