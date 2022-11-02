package com.alibaba.tdata.aisp.server.repository.mapper;

import com.alibaba.tdata.aisp.server.repository.domain.SceneConfigDO;
import com.alibaba.tdata.aisp.server.repository.domain.SceneConfigDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface SceneConfigDOMapper {
    long countByExample(SceneConfigDOExample example);

    int deleteByExample(SceneConfigDOExample example);

    int deleteByPrimaryKey(String sceneCode);

    int insert(SceneConfigDO record);

    int insertSelective(SceneConfigDO record);

    List<SceneConfigDO> selectByExampleWithBLOBsWithRowbounds(SceneConfigDOExample example, RowBounds rowBounds);

    List<SceneConfigDO> selectByExampleWithBLOBs(SceneConfigDOExample example);

    List<SceneConfigDO> selectByExampleWithRowbounds(SceneConfigDOExample example, RowBounds rowBounds);

    List<SceneConfigDO> selectByExample(SceneConfigDOExample example);

    SceneConfigDO selectByPrimaryKey(String sceneCode);

    int updateByExampleSelective(@Param("record") SceneConfigDO record, @Param("example") SceneConfigDOExample example);

    int updateByExampleWithBLOBs(@Param("record") SceneConfigDO record, @Param("example") SceneConfigDOExample example);

    int updateByExample(@Param("record") SceneConfigDO record, @Param("example") SceneConfigDOExample example);

    int updateByPrimaryKeySelective(SceneConfigDO record);

    int updateByPrimaryKeyWithBLOBs(SceneConfigDO record);

    int updateByPrimaryKey(SceneConfigDO record);

    int batchInsert(@Param("list") List<SceneConfigDO> list);

    int batchInsertSelective(@Param("list") List<SceneConfigDO> list, @Param("selective") SceneConfigDO.Column ... selective);

    int upsert(SceneConfigDO record);

    int upsertSelective(SceneConfigDO record);

    int upsertWithBLOBs(SceneConfigDO record);
}