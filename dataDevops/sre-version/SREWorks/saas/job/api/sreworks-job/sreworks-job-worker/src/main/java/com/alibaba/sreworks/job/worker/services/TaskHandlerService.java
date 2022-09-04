package com.alibaba.sreworks.job.worker.services;

import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobs;
import com.alibaba.sreworks.job.worker.taskhandlers.AbstractTaskHandler;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

@Data
@Service
public class TaskHandlerService {

    Map<String, Class<? extends AbstractTaskHandler>> taskHandlerMap = new Hashtable<>();

    @PostConstruct
    public void initTaskHandlerMap() throws NoSuchFieldException, IllegalAccessException {
        Reflections reflections = new Reflections("com.alibaba.sreworks.job.worker.taskhandlers");
        Set<Class<? extends AbstractTaskHandler>> taskHandlers = reflections.getSubTypesOf(AbstractTaskHandler.class);
        for (Class<? extends AbstractTaskHandler> taskHandler : taskHandlers) {
            String execType = (String)taskHandler.getField("execType").get(taskHandler);
            if (StringUtils.isNotEmpty(execType)) {
                taskHandlerMap.put(execType, taskHandler);
            }
        }
    }

    public AbstractTaskHandler newInstance(ElasticTaskInstanceWithBlobs taskInstance)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        AbstractTaskHandler taskHandler = taskHandlerMap.get(taskInstance.getExecType())
            .getDeclaredConstructor().newInstance();
        taskHandler.setTaskInstance(taskInstance);
        return taskHandler;

    }

}
