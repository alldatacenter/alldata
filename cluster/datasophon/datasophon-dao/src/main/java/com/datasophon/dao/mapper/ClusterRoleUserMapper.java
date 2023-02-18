package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterRoleUserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datasophon.dao.entity.UserInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 集群角色用户中间表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@Mapper
public interface ClusterRoleUserMapper extends BaseMapper<ClusterRoleUserEntity> {

    List<UserInfoEntity> getAllClusterManagerByClusterId(@Param("clusterId") Integer clusterId);
}
