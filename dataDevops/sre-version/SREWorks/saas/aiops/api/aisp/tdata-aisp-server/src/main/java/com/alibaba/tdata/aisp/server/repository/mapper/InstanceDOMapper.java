package com.alibaba.tdata.aisp.server.repository.mapper;

import com.alibaba.tdata.aisp.server.repository.domain.InstanceDO;
import com.alibaba.tdata.aisp.server.repository.domain.InstanceDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface InstanceDOMapper {
    long countByExample(InstanceDOExample example);

    int deleteByExample(InstanceDOExample example);

    int deleteByPrimaryKey(String instanceCode);

    int insert(InstanceDO record);

    int insertSelective(InstanceDO record);

    List<InstanceDO> selectByExampleWithBLOBsWithRowbounds(InstanceDOExample example, RowBounds rowBounds);

    List<InstanceDO> selectByExampleWithBLOBs(InstanceDOExample example);

    List<InstanceDO> selectByExampleWithRowbounds(InstanceDOExample example, RowBounds rowBounds);

    List<InstanceDO> selectByExample(InstanceDOExample example);

    InstanceDO selectByPrimaryKey(String instanceCode);

    int updateByExampleSelective(@Param("record") InstanceDO record, @Param("example") InstanceDOExample example);

    int updateByExampleWithBLOBs(@Param("record") InstanceDO record, @Param("example") InstanceDOExample example);

    int updateByExample(@Param("record") InstanceDO record, @Param("example") InstanceDOExample example);

    int updateByPrimaryKeySelective(InstanceDO record);

    int updateByPrimaryKeyWithBLOBs(InstanceDO record);

    int updateByPrimaryKey(InstanceDO record);

    int batchInsert(@Param("list") List<InstanceDO> list);

    int batchInsertSelective(@Param("list") List<InstanceDO> list, @Param("selective") InstanceDO.Column ... selective);

    int upsert(InstanceDO record);

    int upsertSelective(InstanceDO record);

    int upsertWithBLOBs(InstanceDO record);
}