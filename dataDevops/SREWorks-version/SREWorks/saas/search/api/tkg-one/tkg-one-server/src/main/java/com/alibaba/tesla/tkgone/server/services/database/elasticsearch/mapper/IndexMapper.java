package com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 使用内存保存索引真实名称与别名间的映射
 * 自动过滤<em>.</em>开头的索引和别名
 *
 * @author feiquan
 */
@Component
public class IndexMapper {
    /**
     * 缓存的真实索引对应别名表
     */
    private Map<String, Set<String>> indexes = new HashMap<>();
    private Set<String> aliasIndexes = new HashSet<>();

    /**
     * 返回当前所有索引别名
     * @return
     */
    public Set<String> getAliasIndexes() {
        return new HashSet<>(aliasIndexes);
    }

    /**
     * 更新最新的索引数据
     * @param indexes
     */
    public void reset(Map<String, Set<String>> indexes) {
        Set<String> allowedAliases = new HashSet<>();
        Map<String, Set<String>> allowedIndexes = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : indexes.entrySet()) {
            if (!entry.getKey().startsWith(".")) {
                Set<String> allowedItems = entry.getValue().stream().filter(item -> !item.startsWith(".")).collect(Collectors.toSet());
                allowedIndexes.put(entry.getKey(), allowedItems);
                allowedAliases.addAll(allowedItems);
            }
        }
        this.indexes = allowedIndexes;
        this.aliasIndexes = allowedAliases;
    }

    /**
     * 索引是否包含
     * @param index
     * @return
     */
    public boolean isRealContains(String index) {
        return indexes.containsKey(index);
    }

    /**
     * 别名是否包含
     * @param index
     * @return
     */
    public boolean isAliasContains(String index) {
        return aliasIndexes.contains(index);
    }

    /**
     * 添加新的索引
     * @param index
     */
    public void add(String index, String alias) {
        if (index.startsWith(".") || alias.startsWith(".")) {
            return;
        }
        Map<String, Set<String>> indexes = new HashMap<>(this.indexes);
        Set<String> aliases = indexes.computeIfAbsent(index, k -> new HashSet<>());
        aliases.add(alias);
        reset(indexes);
    }

    /**
     * 根据索引别名获取名称
     * @param alias
     * @return
     */
    public Set<String> getRealByAlias(String alias) {
        Set<String> matchIndexes = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : indexes.entrySet()) {
            if (entry.getValue().contains(alias)) {
                matchIndexes.add(entry.getKey());
            }
        }
        return matchIndexes;
    }

    /**
     * 返回当前所有的真实索引名称
     * @return
     */
    public Set<String> getRealIndexes() {
        return new HashSet<>(indexes.keySet());
    }
}
