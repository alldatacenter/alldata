package com.platform.schedule.entity;

import redis.clients.jedis.Jedis;

import java.util.List;

public class RedisClient {
    public static Jedis jedis;

    //静态代码块初始化 redis
    static {
        jedis = new Jedis(Property.getStrValue("redis.host"), Property.getIntegerValue("redis.port"));
        jedis.auth(Property.getStrValue("redis.password"));
        jedis.select(Property.getIntegerValue("redis.db"));
    }

    public static String getData(String s) {
        return jedis.get(s);
    }

    public static boolean putData(String key, String value) {
        return jedis.append(key, value) != null;
    }

    public static boolean rpush(String key, List<String> value) {
        for(int i = 0; i < value.size(); i++) {
            jedis.rpush(key, value.get(i));
        }
        return true;
    }

    public static List<String> lrange(String key) {
        return jedis.lrange(key, 0, -1);
    }
}
