package cn.datax.service.workflow.flowable;

import cn.datax.common.core.DataConstant;
import cn.datax.common.rabbitmq.config.RabbitMqConstant;
import cn.datax.common.utils.SpringContextHolder;
import cn.datax.service.workflow.api.enums.VariablesEnum;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EndTaskListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution delegateExecution) {
        log.info("业务编号{}", delegateExecution.getProcessInstanceBusinessKey());
        log.info("节点{},{},{}", delegateExecution.getId(), delegateExecution.getEventName(), delegateExecution.getCurrentActivityId());
        log.info("Variables：{}", delegateExecution.getVariables());
        // 业务状态变更操作
        Map<String, Object> variables = delegateExecution.getVariables();
        // 发送消息队列
        String businessKey = (String) variables.get(VariablesEnum.businessKey.toString());
        String businessCode = (String) variables.get(VariablesEnum.businessCode.toString());
        log.info("业务结束：{},{}", businessKey, businessCode);
        Map<String, Object> map = new HashMap<>(4);
        map.put(VariablesEnum.businessKey.toString(), businessKey);
        map.put(VariablesEnum.businessCode.toString(), businessCode);
        if (delegateExecution.getCurrentActivityId().equals(VariablesEnum.approveEnd.toString())) {
            log.info("业务结束状态：{}", DataConstant.AuditState.AGREE.getKey());
            map.put("flowStatus", DataConstant.AuditState.AGREE.getKey());
        } else if (delegateExecution.getCurrentActivityId().equals(VariablesEnum.rejectEnd.toString())) {
            log.info("业务结束状态：{}", DataConstant.AuditState.REJECT.getKey());
            map.put("flowStatus", DataConstant.AuditState.REJECT.getKey());
        }
        RabbitTemplate rabbitTemplate = SpringContextHolder.getBean(RabbitTemplate.class);
        rabbitTemplate.convertAndSend(RabbitMqConstant.TOPIC_EXCHANGE_WORKFLOW, RabbitMqConstant.TOPIC_WORKFLOW_KEY + businessCode, map);
    }
}
