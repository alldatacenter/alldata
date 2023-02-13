package com.alibaba.tesla.appmanager.server.action.impl.componentpackage;

import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskStateEnum;
import com.alibaba.tesla.appmanager.server.action.ComponentPackageTaskStateAction;
import com.alibaba.tesla.appmanager.server.event.loader.ComponentPackageTaskStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Component Package State 处理 Action - SKIP
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service("SkipComponentPackageTaskStateAction")
public class SkipComponentPackageTaskStateAction implements ComponentPackageTaskStateAction, ApplicationRunner {

    private static final String LOG_PRE = String.format("action=action.%s|message=",
            SkipComponentPackageTaskStateAction.class.getSimpleName());

    private static final ComponentPackageTaskStateEnum STATE = ComponentPackageTaskStateEnum.SKIP;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private ComponentPackageTaskRepository componentPackageTaskRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        publisher.publishEvent(new ComponentPackageTaskStateActionLoadedEvent(
                this, STATE.toString(), this.getClass().getSimpleName()));
    }

    /**
     * 自身逻辑处理
     *
     * @param task 部署工单
     */
    @Override
    public void run(ComponentPackageTaskDO task) {
        updateComponentTaskStatus(task);
        log.info("reached skip state|componentPackageTaskId={}", task.getComponentPackageId());
    }

    private void updateComponentTaskStatus(ComponentPackageTaskDO taskDO) {
        String oldStautus = taskDO.getTaskStatus();
        // 状态转移
        taskDO.setTaskStatus(STATE.toString());
        componentPackageTaskRepository.updateByCondition(taskDO, ComponentPackageTaskQueryCondition.builder().id(taskDO.getId()).build());
        log.info("actionName=RunningComponentPackageTask||status transitioned from {} to {}", oldStautus, STATE.toString());
    }
}
