package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 集群信息表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@Mapper
public interface ClusterInfoMapper extends BaseMapper<ClusterInfoEntity> {

    ClusterInfoEntity getClusterByClusterCode(@Param("clusterCode") String clusterCode);
}
