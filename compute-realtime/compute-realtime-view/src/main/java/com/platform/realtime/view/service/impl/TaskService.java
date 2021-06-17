package com.platform.realtime.view.service.impl;

import com.platform.realtime.view.module.*;
import com.platform.realtime.view.service.ITaskService;
import com.platform.realtime.view.common.base.BaseService;
import com.platform.realtime.view.common.base.IBaseMapper;
import com.platform.realtime.view.dao.TaskMapper;
import com.platform.realtime.view.module.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("taskService")
public class TaskService extends BaseService<Task> implements ITaskService {

    @Autowired
    private TaskMapper taskMapper;


    @Override
    public IBaseMapper<Task> getBaseMapper() {
        return taskMapper;
    }

    @Override
    public List<Task> findAllTasks() {
        List<Task> tasks = taskMapper.selectAllTask();
        return tasks;
    }

    @Override
    public List<Map<String, Object>> getConvertRate(String taskName) {
        List<Task> tasks = taskMapper.selectByName(taskName);
        Long taskid = tasks.get(0).getTaskid();

        //TODO:??
        List<Map<String, Object>> pageAndRageMapList = new ArrayList<Map<String, Object>>();
        String convertRate = taskMapper.selectConvertRateById(taskid);
        String[] converRates = convertRate.split("\\|");
        for (String item : converRates) {
            String[] pageAndRate = item.split("=");
            Map<String, Object> pageAndRateMap = new HashMap<>();
            pageAndRateMap.put("key", pageAndRate[0]);
            pageAndRateMap.put("value", Double.valueOf(pageAndRate[1]) * 100);
            pageAndRageMapList.add(pageAndRateMap);
        }
        return pageAndRageMapList;
    }

    @Override
    public List<AreaTop3Product> getAreaTop3Product(String taskName) {
        List<Task> tasks = taskMapper.selectByName(taskName);
        Long taskid = tasks.get(0).getTaskid();

        //TODO:??
        List<AreaTop3Product> pageAndRageMapList = taskMapper.selectAreaProductTop3(taskid);
        return pageAndRageMapList;
    }

    @Override
    public SessionAggrStat getSessionAggrStat(String taskName) {
        List<Task> tasks = taskMapper.selectByName(taskName);
        Long taskid = tasks.get(0).getTaskid();

        //TODO:??
        List<SessionAggrStat> sessionAggrStatList = taskMapper.selectSessionAggrStat(taskid);

        SessionAggrStat sessionAggrStat = sessionAggrStatList.get(0);
        return sessionAggrStat;
    }

    @Override
    public List<Top10Category> getTop10Category(String taskName) {
        List<Task> tasks = taskMapper.selectByName(taskName);
        Long taskid = tasks.get(0).getTaskid();

        //TODO:??
        List<Top10Category> top10Categories = taskMapper.selectTop10Category(taskid);
        return top10Categories;
    }

    @Override
    public List<Top10Session> getTop10Session(String taskName) {
        List<Task> tasks = taskMapper.selectByName(taskName);
        Long taskid = tasks.get(0).getTaskid();

        //TODO:??
        List<Top10Session> top10Sessions = taskMapper.selectTop10Session(taskid);
        return top10Sessions;
    }

    @Override
    public boolean runAdSparkTask() {
        String ifMockData = taskMapper.getMockDataState();
        if ("false".equals(ifMockData)) {
            return false;
        }

        Runtime rt = Runtime.getRuntime();
        //生成mock data
        System.out.println("mock ad data begin");
        String command = "java -classpath /usr/local/spark/compute-realtime-view/compute-realtime-view-jar-with-dependencies.jar " +
                "com.platform.realtime.view.project.test.MockRealTimeData &";
        System.out.println("mock ad data end");
        Process proc = null;
        try {
            System.out.println("广告点击流量模拟数据开始！");
            proc = rt.exec(command);// 执行Linux命令
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        while ("true".equals(ifMockData)) {
            ifMockData = taskMapper.getMockDataState();
        }
        proc.destroy();
        System.out.println("广告点击流量模拟数据结束！");
        return true;
    }

    @Override
    public boolean updateMockData(String ifMockData) {
        try {
            synchronized (this){
                taskMapper.setMockDataState(ifMockData);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }



}
