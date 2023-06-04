package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterZk;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-09-07 10:04:16
 */
@Mapper
public interface ClusterZkMapper extends BaseMapper<ClusterZk> {

    Integer getMaxMyId(@Param("clusterId") Integer clusterId);
}
