package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 集群服务角色实例表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@Mapper
public interface ClusterServiceRoleInstanceMapper extends BaseMapper<ClusterServiceRoleInstanceEntity> {

    void updateToNeedRestart(@Param("roleGroupId") Integer roleGroupId);
}
