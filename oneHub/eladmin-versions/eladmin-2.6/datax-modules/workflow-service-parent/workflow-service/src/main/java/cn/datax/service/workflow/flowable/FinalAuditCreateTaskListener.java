package cn.datax.service.workflow.flowable;

import cn.datax.common.utils.SpringContextHolder;
import cn.datax.service.workflow.api.enums.VariablesEnum;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;

import java.util.Map;

@Slf4j
public class FinalAuditCreateTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        log.info("进入终审节点用户任务启动监听器");
        Map<String, Object> variables = delegateTask.getVariables();
        log.info("Variables：{}", delegateTask.getVariables());
        log.info("任务执行人：{}", delegateTask.getAssignee());
        log.info("任务配置ID: {}", delegateTask.getTaskDefinitionKey());
        TaskService taskService = SpringContextHolder.getBean(TaskService.class);
        String businessAuditGroup = (String) variables.get(VariablesEnum.businessAuditGroup.toString());
        taskService.addCandidateGroup(delegateTask.getId(), businessAuditGroup);
//        taskService.setAssignee(delegateTask.getId(), "1214835832967581698");
//        taskService.addCandidateUser(delegateTask.getId(), "");
//        taskService.addCandidateGroup(delegateTask.getId(), "");
        log.info("退出终审节点用户任务启动监听器");
    }
}
