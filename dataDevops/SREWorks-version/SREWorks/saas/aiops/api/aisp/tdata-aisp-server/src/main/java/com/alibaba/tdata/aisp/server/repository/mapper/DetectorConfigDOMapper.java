package com.alibaba.tdata.aisp.server.repository.mapper;

import com.alibaba.tdata.aisp.server.repository.domain.DetectorConfigDO;
import com.alibaba.tdata.aisp.server.repository.domain.DetectorConfigDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface DetectorConfigDOMapper {
    long countByExample(DetectorConfigDOExample example);

    int deleteByExample(DetectorConfigDOExample example);

    int deleteByPrimaryKey(String detectorCode);

    int insert(DetectorConfigDO record);

    int insertSelective(DetectorConfigDO record);

    List<DetectorConfigDO> selectByExampleWithBLOBsWithRowbounds(DetectorConfigDOExample example, RowBounds rowBounds);

    List<DetectorConfigDO> selectByExampleWithBLOBs(DetectorConfigDOExample example);

    List<DetectorConfigDO> selectByExampleWithRowbounds(DetectorConfigDOExample example, RowBounds rowBounds);

    List<DetectorConfigDO> selectByExample(DetectorConfigDOExample example);

    DetectorConfigDO selectByPrimaryKey(String detectorCode);

    int updateByExampleSelective(@Param("record") DetectorConfigDO record, @Param("example") DetectorConfigDOExample example);

    int updateByExampleWithBLOBs(@Param("record") DetectorConfigDO record, @Param("example") DetectorConfigDOExample example);

    int updateByExample(@Param("record") DetectorConfigDO record, @Param("example") DetectorConfigDOExample example);

    int updateByPrimaryKeySelective(DetectorConfigDO record);

    int updateByPrimaryKeyWithBLOBs(DetectorConfigDO record);

    int updateByPrimaryKey(DetectorConfigDO record);

    int batchInsert(@Param("list") List<DetectorConfigDO> list);

    int batchInsertSelective(@Param("list") List<DetectorConfigDO> list, @Param("selective") DetectorConfigDO.Column ... selective);

    int upsert(DetectorConfigDO record);

    int upsertSelective(DetectorConfigDO record);

    int upsertWithBLOBs(DetectorConfigDO record);
}