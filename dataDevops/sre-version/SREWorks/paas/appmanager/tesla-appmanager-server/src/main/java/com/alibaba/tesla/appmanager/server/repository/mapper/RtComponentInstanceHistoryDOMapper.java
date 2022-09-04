package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceHistoryDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceHistoryDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RtComponentInstanceHistoryDOMapper {
    long countByExample(RtComponentInstanceHistoryDOExample example);

    int deleteByExample(RtComponentInstanceHistoryDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(RtComponentInstanceHistoryDO record);

    int insertSelective(RtComponentInstanceHistoryDO record);

    List<RtComponentInstanceHistoryDO> selectByExample(RtComponentInstanceHistoryDOExample example);

    RtComponentInstanceHistoryDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") RtComponentInstanceHistoryDO record, @Param("example") RtComponentInstanceHistoryDOExample example);

    int updateByExample(@Param("record") RtComponentInstanceHistoryDO record, @Param("example") RtComponentInstanceHistoryDOExample example);

    int updateByPrimaryKeySelective(RtComponentInstanceHistoryDO record);

    int updateByPrimaryKey(RtComponentInstanceHistoryDO record);

    /**
     * 删除指定组件实例下小于 gmtCreate 创建时间的所有历史记录
     *
     * @param componentInstanceId 组件实例 ID
     * @param instanceKeepDays    保留天数
     * @return 删除数量 (最多 1000 条单次)
     */
    int deleteExpiredRecords(@Param("componentInstanceId") String componentInstanceId, @Param("instanceKeepDays") int instanceKeepDays);
}