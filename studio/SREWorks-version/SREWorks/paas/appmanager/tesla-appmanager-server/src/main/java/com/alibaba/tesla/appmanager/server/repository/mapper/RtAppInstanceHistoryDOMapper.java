package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceHistoryDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceHistoryDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RtAppInstanceHistoryDOMapper {
    long countByExample(RtAppInstanceHistoryDOExample example);

    int deleteByExample(RtAppInstanceHistoryDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(RtAppInstanceHistoryDO record);

    int insertSelective(RtAppInstanceHistoryDO record);

    List<RtAppInstanceHistoryDO> selectByExample(RtAppInstanceHistoryDOExample example);

    RtAppInstanceHistoryDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") RtAppInstanceHistoryDO record, @Param("example") RtAppInstanceHistoryDOExample example);

    int updateByExample(@Param("record") RtAppInstanceHistoryDO record, @Param("example") RtAppInstanceHistoryDOExample example);

    int updateByPrimaryKeySelective(RtAppInstanceHistoryDO record);

    int updateByPrimaryKey(RtAppInstanceHistoryDO record);

    /**
     * 删除指定组件实例下小于 gmtCreate 创建时间的所有历史记录
     *
     * @param appInstanceId    组件实例 ID
     * @param instanceKeepDays 保留天数
     * @return 删除数量 (最多 1000 条单次)
     */
    int deleteExpiredRecords(@Param("appInstanceId") String appInstanceId, @Param("instanceKeepDays") int instanceKeepDays);
}