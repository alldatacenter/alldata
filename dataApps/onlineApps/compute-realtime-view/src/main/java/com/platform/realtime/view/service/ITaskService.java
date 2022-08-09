package com.platform.realtime.view.service;

import com.platform.realtime.view.module.*;
import com.platform.realtime.view.common.base.IBaseService;
import com.platform.realtime.view.module.*;

import java.util.List;
import java.util.Map;

public interface ITaskService extends IBaseService<Task> {
    public List<Task> findAllTasks();

    List<Map<String, Object>> getConvertRate(String taskName);

    List<AreaTop3Product> getAreaTop3Product(String taskName);

    SessionAggrStat getSessionAggrStat(String taskName);

    List<Top10Category> getTop10Category(String taskName);

    List<Top10Session> getTop10Session(String taskName);

    boolean runAdSparkTask();

    boolean updateMockData(String ifMockData);
}
