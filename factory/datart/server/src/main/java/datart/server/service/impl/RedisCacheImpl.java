package datart.server.service.impl;

import datart.core.common.Cache;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class RedisCacheImpl implements Cache {

    private final RedisTemplate redisTemplate;

    public RedisCacheImpl(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void put(String key, Object object) {
        redisTemplate.opsForValue().set(key, object);
    }

    @Override
    public void put(String key, Object object, int ttl) {
        redisTemplate.opsForValue().set(key, object, ttl, TimeUnit.SECONDS);
    }

    @Override
    public boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    @Override
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }
}
