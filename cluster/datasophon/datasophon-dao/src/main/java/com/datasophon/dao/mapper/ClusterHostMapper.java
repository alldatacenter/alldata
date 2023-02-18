package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterHostEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 集群主机表 
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-14 20:32:39
 */
@Mapper
public interface ClusterHostMapper extends BaseMapper<ClusterHostEntity> {

    ClusterHostEntity getClusterHostByHostname(@Param("hostname") String hostname);

    void updateBatchNodeLabel(@Param("hostIds") String hostIds,@Param("nodeLabel") String nodeLabel);
}
