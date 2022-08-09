package com.test.datasync;

ClickhouseQueryService:
        package com.test.datasync;

        import java.util.Map;

/**
 * 根据集群名称查找可以使用的机器
 */
public interface ClickhouseQueryService {
    /**
     * 根据集群名称查找可以使用的机器
     * @param clusterName 集群名词
     * @return Map: shardNum -> random . 集群的分片可以处于[1, n]之间
     */
    Map<Integer, String> findActiveServersForEachShard(String clusterName);
}