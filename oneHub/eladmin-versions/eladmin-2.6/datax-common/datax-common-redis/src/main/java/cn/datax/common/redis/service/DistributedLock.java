package cn.datax.common.redis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * redis分布式锁
 */
@Slf4j
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 加锁，无阻塞
     * @param lock
     * @param key
     * @param expireTime    锁过期时间单位秒
     * @return
     */
    public boolean tryLock(String lock, String key, Long expireTime) {
        return this.tryLock(lock, key, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 加锁，无阻塞
     * @param lock
     * @param key
     * @param expireTime    锁过期时间
     * @param timeUnit
     * @return
     */
    public boolean tryLock(String lock, String key, Long expireTime, TimeUnit timeUnit) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lock, key, expireTime, timeUnit);
        if (success == null || !success) {
            log.info("申请锁(" + lock + "," + key + ")失败");
            return false;
        }
        log.error("申请锁(" + lock + "," + key + ")成功");
        return true;
    }

    public void unlock(String lock, String key) {
        String script = "if redis.call('get', KEYS[1]) == KEYS[2] then return redis.call('del', KEYS[1]) else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(redisScript, Arrays.asList(lock, key));
        if (result == null || result == 0) {
            log.info("释放锁(" + lock + "," + key + ")失败,该锁不存在或锁已经过期");
        } else {
            log.info("释放锁(" + lock + "," + key + ")成功");
        }
    }
}
