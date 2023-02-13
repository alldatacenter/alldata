package cn.datax.service.workflow.flowable;

import cn.datax.common.core.DataConstant;
import cn.datax.common.rabbitmq.config.RabbitMqConstant;
import cn.datax.common.utils.SpringContextHolder;
import cn.datax.service.workflow.api.enums.VariablesEnum;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SubmitCompleteTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        log.info("进入提交节点用户任务完成监听器");
        log.info("Variables：{}", delegateTask.getVariables());
        log.info("任务执行人：{}", delegateTask.getAssignee());
        log.info("任务配置ID: {}", delegateTask.getTaskDefinitionKey());
        // 业务状态变更操作
        Map<String, Object> variables = delegateTask.getVariables();
        // 发送消息队列
        String businessKey = (String) variables.get(VariablesEnum.businessKey.toString());
        String businessCode = (String) variables.get(VariablesEnum.businessCode.toString());
        log.info("业务审核中：{},{}", businessKey, businessCode);
        log.info("业务审核中状态：{}", DataConstant.AuditState.AUDIT.getKey());
        log.info("退出提交节点用户任务完成监听器");
        RabbitTemplate rabbitTemplate = SpringContextHolder.getBean(RabbitTemplate.class);
        Map<String, Object> map = new HashMap<>(4);
        map.put(VariablesEnum.businessKey.toString(), businessKey);
        map.put(VariablesEnum.businessCode.toString(), businessCode);
        map.put("flowStatus", DataConstant.AuditState.AUDIT.getKey());
        rabbitTemplate.convertAndSend(RabbitMqConstant.TOPIC_EXCHANGE_WORKFLOW, RabbitMqConstant.TOPIC_WORKFLOW_KEY + businessCode, map);
    }
}
