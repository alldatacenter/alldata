package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 集群服务操作指令主机表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
@Mapper
public interface ClusterServiceCommandHostMapper extends BaseMapper<ClusterServiceCommandHostEntity> {

    Integer getCommandHostTotalProgressByCommandId(@Param("commandId") String commandId);
}
