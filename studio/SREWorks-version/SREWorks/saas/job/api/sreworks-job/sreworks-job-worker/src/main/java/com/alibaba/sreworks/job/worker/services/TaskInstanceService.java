package com.alibaba.sreworks.job.worker.services;

import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstance;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobs;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobsRepository;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.sreworks.job.utils.SleepUtil;
import com.alibaba.sreworks.job.worker.taskhandlers.AbstractTaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class TaskInstanceService {

    @Autowired
    TaskHandlerService taskHandlerService;

    @Autowired
    ElasticTaskInstanceWithBlobsRepository taskInstanceWithBlobsRepository;

    @Autowired
    ElasticTaskInstanceService elasticTaskInstanceService;

    public ThreadPoolTaskExecutor runningExecutor = new ThreadPoolTaskExecutor();

    public Map<String, Future<Object>> futureMap = new Hashtable<>();

    public Map<String, AbstractTaskHandler> taskHandlerMap = new Hashtable<>();

    @PostConstruct
    public void postConstruct() {
        runningExecutor.initialize();
    }

    @PreDestroy
    public void preDestroy() {
        runningExecutor.shutdown();
    }

    private boolean shouldRetry(ElasticTaskInstance taskInstance) {
        return taskInstance.getExecRetryTimes() >= taskInstance.getExecTimes();
    }

    private void retry(ElasticTaskInstance taskInstance) {
        elasticTaskInstanceService.appendString(taskInstance.getId(), "stdout", "\n#####RETRY#####\n");
        elasticTaskInstanceService.appendString(taskInstance.getId(), "stderr", "\n#####RETRY#####\n");
        elasticTaskInstanceService.toWaitRetry(taskInstance);
        SleepUtil.sleep(taskInstance.getExecRetryInterval());
        start(taskInstance.getId());
    }

    public void start(String id) {

        ElasticTaskInstanceWithBlobs taskInstance = taskInstanceWithBlobsRepository.findFirstById(id);
        elasticTaskInstanceService.addExecTimes(taskInstance);
        try {
            Future<Object> future;
            elasticTaskInstanceService.toRunning(taskInstance);
            AbstractTaskHandler taskHandler = taskHandlerService.newInstance(taskInstance);

            taskHandlerMap.put(id, taskHandler);
            future = Executors.newSingleThreadExecutor().submit(() -> {
                taskHandler.execute();
                return null;
            });
            futureMap.put(id, future);
        } catch (Exception e) {
            elasticTaskInstanceService.toException(taskInstance, e);
        }

    }

    public void stop(String id) {
        futureMap.get(id).cancel(true);
        taskHandlerMap.get(id).setCancelByStop(true);
        elasticTaskInstanceService.update(id, JsonUtil.map(
            "gmtStop", System.currentTimeMillis()
        ));
    }

    @Scheduled(fixedRate = 1000)
    private void scheduleTimeout() {

        new ArrayList<>(futureMap.entrySet()).stream()
            .filter(x -> !x.getValue().isDone())
            .forEach(entry -> {

                String id = entry.getKey();
                Future<Object> future = entry.getValue();

                AbstractTaskHandler taskHandler = taskHandlerMap.get(id);
                ElasticTaskInstanceWithBlobs taskInstance = taskHandler.getTaskInstance();

                if (System.currentTimeMillis() - taskInstance.getGmtExecute() > taskInstance.getExecTimeout() * 1000) {
                    future.cancel(true);
                    taskHandler.setCancelByTimeout(true);
                }

            });

    }

    @Scheduled(fixedRate = 1000)
    public void scheduleDone() {

        new ArrayList<>(futureMap.entrySet()).stream()
            .filter(x -> x.getValue().isDone())
            .forEach(entry -> {

                boolean shouldRetry = false;
                String id = entry.getKey();
                Future<Object> future = entry.getValue();

                AbstractTaskHandler taskHandler = taskHandlerMap.get(id);
                ElasticTaskInstanceWithBlobs taskInstance = taskHandler.getTaskInstance();

                if (future.isCancelled()) {
                    if (taskHandler.isCancelByStop()) {
                        elasticTaskInstanceService.toStopped(taskInstance);
                    } else if (taskHandler.isCancelByTimeout()) {
                        shouldRetry = shouldRetry(taskInstance);
                        if (!shouldRetry) {
                            elasticTaskInstanceService.toTimeout(taskInstance);
                        }
                    } else {
                        shouldRetry = shouldRetry(taskInstance);
                        if (!shouldRetry) {
                            elasticTaskInstanceService.toCancelled(taskInstance);
                        }
                    }
                } else {
                    try {
                        future.get();
                        elasticTaskInstanceService.toSuccess(taskInstance, taskInstance.getOutVarConf());
                    } catch (Exception e) {
                        shouldRetry = shouldRetry(taskInstance);
                        if (!shouldRetry) {
                            elasticTaskInstanceService.toException(taskInstance, e);
                        }
                    }
                }
                taskHandler.destroy();
                taskHandlerMap.remove(id);
                futureMap.remove(id);
                if (shouldRetry) {
                    retry(taskInstance);
                }
            });

    }

}
