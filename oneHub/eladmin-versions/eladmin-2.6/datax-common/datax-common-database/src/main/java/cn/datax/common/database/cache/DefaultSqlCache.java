package cn.datax.common.database.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultSqlCache extends LinkedHashMap<String, DefaultSqlCache.ExpireNode<Object>> implements SqlCache {

    private int capacity;

    private long expire;

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public DefaultSqlCache(int capacity, long expire) {
        super((int) Math.ceil(capacity / 0.75) + 1, 0.75f, true);
        // 容量
        this.capacity = capacity;
        // 固定过期时间
        this.expire = expire;
    }

    @Override
    public void put(String key, Object value, long ttl) {
        long expireTime = Long.MAX_VALUE;
        if (ttl >= 0) {
            expireTime = System.currentTimeMillis() + (ttl == 0 ? this.expire : ttl);
        }
        lock.writeLock().lock();
        try {
            // 封装成过期时间节点
            put(key, new ExpireNode<>(expireTime, value));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Object get(String key) {
        lock.readLock().lock();
        ExpireNode<Object> expireNode;
        try {
            expireNode = super.get(key);
        } finally {
            lock.readLock().unlock();
        }
        if (expireNode == null) {
            return null;
        }
        // 惰性删除过期的
        if (this.expire > -1L && expireNode.expire < System.currentTimeMillis()) {
            try {
                lock.writeLock().lock();
                super.remove(key);
            } finally {
                lock.writeLock().unlock();
            }
            return null;
        }
        return expireNode.value;
    }

    @Override
    public void delete(String key) {
        try {
            lock.writeLock().lock();
            Iterator<Map.Entry<String, ExpireNode<Object>>> iterator = super.entrySet().iterator();
            // 清除key的缓存
            while (iterator.hasNext()) {
                Map.Entry<String, ExpireNode<Object>> entry = iterator.next();
                if (entry.getKey().equals(key)) {
                    iterator.remove();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    protected boolean removeEldestEntry(Map.Entry<String, ExpireNode<Object>> eldest) {
        if (this.expire > -1L && size() > capacity) {
            clean();
        }
        // lru淘汰
        return size() > this.capacity;
    }

    /**
     * 清理已过期的数据
     */
    private void clean() {
        try {
            lock.writeLock().lock();
            Iterator<Map.Entry<String, ExpireNode<Object>>> iterator = super.entrySet().iterator();
            long now = System.currentTimeMillis();
            while (iterator.hasNext()) {
                Map.Entry<String, ExpireNode<Object>> next = iterator.next();
                // 判断是否过期
                if (next.getValue().expire < now) {
                    iterator.remove();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    /**
     * 过期时间节点
     */
    static class ExpireNode<V> {
        long expire;
        Object value;
        public ExpireNode(long expire, Object value) {
            this.expire = expire;
            this.value = value;
        }
    }
}
