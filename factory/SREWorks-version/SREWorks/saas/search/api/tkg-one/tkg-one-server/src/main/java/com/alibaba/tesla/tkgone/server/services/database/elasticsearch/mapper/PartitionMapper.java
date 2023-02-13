package com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存ES中indices和partitions的关系
 *
 * @author xueyong.zxy
 */
@Component
public class PartitionMapper {
    private ConcurrentHashMap<String, List<String>> partitions = new ConcurrentHashMap<>();

    /**
     * 设置aliasIndex的partitions值
     * @param aliasIndex
     * @param partitions
     */
    public void set(String aliasIndex, List<String> partitions) {
        this.partitions.put(aliasIndex, partitions);
    }

    /**
     * 查询aliasIndex的partitions值
     * @param aliasIndex
     * @return
     */
    public List<String> get(String aliasIndex) {
        return this.partitions.getOrDefault(aliasIndex, Collections.emptyList());
    }
}
