package cn.datax.service.workflow.flowable;

import cn.datax.common.core.DataConstant;
import cn.datax.common.rabbitmq.config.RabbitMqConstant;
import cn.datax.common.utils.SpringContextHolder;
import cn.datax.service.workflow.api.enums.VariablesEnum;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class InitialAuditCompleteTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        log.info("进入初审节点用户任务完成监听器");
        log.info("Variables：{}", delegateTask.getVariables());
        log.info("任务执行人：{}", delegateTask.getAssignee());
        log.info("任务配置ID: {}", delegateTask.getTaskDefinitionKey());
        Map<String, Object> variables = delegateTask.getVariables();
        Boolean approved = (Boolean) Optional.ofNullable(variables).map(s -> s.get(VariablesEnum.approved.toString())).orElse(true);
        log.info("审核结果: {}", approved);
        // 退回操作
        if (!approved) {
            // 发送消息队列
            String businessKey = (String) variables.get(VariablesEnum.businessKey.toString());
            String businessCode = (String) variables.get(VariablesEnum.businessCode.toString());
            log.info("业务退回：{},{}", businessKey, businessCode);
            log.info("业务退回状态：{}", DataConstant.AuditState.BACK.getKey());
            RabbitTemplate rabbitTemplate = SpringContextHolder.getBean(RabbitTemplate.class);
            Map<String, Object> map = new HashMap<>(4);
            map.put(VariablesEnum.businessKey.toString(), businessKey);
            map.put(VariablesEnum.businessCode.toString(), businessCode);
            map.put("flowStatus", DataConstant.AuditState.BACK.getKey());
            rabbitTemplate.convertAndSend(RabbitMqConstant.TOPIC_EXCHANGE_WORKFLOW, RabbitMqConstant.TOPIC_WORKFLOW_KEY + businessCode, map);
        }
        log.info("退出初审节点用户任务完成监听器");
    }
}
