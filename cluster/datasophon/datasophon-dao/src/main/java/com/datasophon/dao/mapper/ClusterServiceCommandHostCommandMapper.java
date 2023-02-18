package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 集群服务操作指令主机指令表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
@Mapper
public interface ClusterServiceCommandHostCommandMapper extends BaseMapper<ClusterServiceCommandHostCommandEntity> {

    Integer getHostCommandTotalProgressByHostnameAndCommandHostId(@Param("hostname") String hostname,@Param("commandHostId") String commandHostId);
}
