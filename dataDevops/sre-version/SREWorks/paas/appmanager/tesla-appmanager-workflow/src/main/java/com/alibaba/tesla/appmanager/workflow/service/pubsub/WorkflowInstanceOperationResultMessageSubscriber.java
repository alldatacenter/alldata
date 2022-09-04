package com.alibaba.tesla.appmanager.workflow.service.pubsub;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.res.workflow.WorkflowInstanceOperationRes;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

/**
 * Workflow Instance 执行结果消息订阅处理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class WorkflowInstanceOperationResultMessageSubscriber extends MessageListenerAdapter {

    @Autowired
    private RedisTemplate<String, String> template;

    @Override
    public void onMessage(@NotNull Message message, byte[] bytes) {
        String topic = template.getStringSerializer().deserialize(message.getChannel());
        String value = template.getStringSerializer().deserialize(message.getBody());
        if (!WorkflowInstanceMessageConfig.TOPIC_WORKFLOW_INSTANCE_OPERATION_RESULT.equals(topic)) {
            log.error("[result] received invalid message from redis channel|channel={}|value={}", topic, value);
            return;
        }

        // operator 转换到目标对象
        WorkflowInstanceOperationRes result;
        try {
            result = JSONObject.parseObject(value, WorkflowInstanceOperationRes.class);
            if (result == null) {
                throw new RuntimeException("null operation");
            }
        } catch (Exception e) {
            log.error("[result] received invalid message from redis channel, cannot transform it to " +
                    "WorkflowInstanceOperationResult|channel={}|value={}|exception={}", topic, value,
                    ExceptionUtils.getStackTrace(e));
            return;
        }
        String command = result.getCommand();
        Long workflowInstanceId = result.getWorkflowInstanceId();
        boolean success = result.isSuccess();
        String clientHost = result.getClientHost();
        String extMessage = result.getMessage();
        if (workflowInstanceId == null || workflowInstanceId <= 0) {
            log.error("[result] received invalid message from redis channel, empty workflow instance id|channel={}|" +
                            "value={}", topic, value);
            return;
        }

        // 发送信号，触发顶层 wait 等待中断
        log.info("[result] received command execution result to {} workflow instance|workflowInstanceId={}|" +
                        "success={}|clientHost={}|extMessage={}", command, workflowInstanceId, success,
                clientHost, extMessage);
        WorkflowInstanceOperationCommandWaitingObject.triggerFinished(workflowInstanceId, command, result);
    }
}