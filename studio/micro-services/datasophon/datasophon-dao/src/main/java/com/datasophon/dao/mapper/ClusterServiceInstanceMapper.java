package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 集群服务表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@Mapper
public interface ClusterServiceInstanceMapper extends BaseMapper<ClusterServiceInstanceEntity> {

    String getServiceConfigByClusterIdAndServiceName(@Param("clusterId") Integer clusterId, @Param("serviceName") String serviceName);
}
