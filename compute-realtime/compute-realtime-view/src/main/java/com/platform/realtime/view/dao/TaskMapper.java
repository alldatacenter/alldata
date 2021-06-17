package com.platform.realtime.view.dao;

import com.platform.realtime.view.module.*;
import com.platform.realtime.view.common.base.IBaseMapper;
import com.platform.realtime.view.module.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TaskMapper extends IBaseMapper<Task> {
    List<Task> selectAllTask();

    List<Task>  selectByName(@Param("taskName") String taskName);

    String selectConvertRateById(@Param("taskid") Long taskid);

    List<AreaTop3Product> selectAreaProductTop3(@Param("taskid") Long taskid);

    List<SessionAggrStat> selectSessionAggrStat(@Param("taskid") Long taskid);

    List<Top10Category> selectTop10Category(@Param("taskid") Long taskid);

    List<Top10Session> selectTop10Session(@Param("taskid") Long taskid);

    void setMockDataState(@Param("ifMockData") String ifMockData);

    String getMockDataState();
}