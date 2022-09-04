package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceTaskDOExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by Mybatis Generator 2020/09/29
 */
public interface AddonInstanceTaskMapper {
    long countByExample(AddonInstanceTaskDOExample example);

    int deleteByExample(AddonInstanceTaskDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(AddonInstanceTaskDO record);

    int insertOrUpdate(AddonInstanceTaskDO record);

    int insertOrUpdateSelective(AddonInstanceTaskDO record);

    int insertSelective(AddonInstanceTaskDO record);

    List<AddonInstanceTaskDO> selectByExample(AddonInstanceTaskDOExample example);

    AddonInstanceTaskDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") AddonInstanceTaskDO record, @Param("example") AddonInstanceTaskDOExample example);

    int updateByExample(@Param("record") AddonInstanceTaskDO record, @Param("example") AddonInstanceTaskDOExample example);

    int updateByPrimaryKeySelective(AddonInstanceTaskDO record);

    int updateByPrimaryKey(AddonInstanceTaskDO record);
}