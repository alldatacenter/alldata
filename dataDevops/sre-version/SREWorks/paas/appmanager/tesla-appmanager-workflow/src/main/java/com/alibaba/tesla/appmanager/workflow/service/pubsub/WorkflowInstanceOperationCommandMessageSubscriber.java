package com.alibaba.tesla.appmanager.workflow.service.pubsub;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.domain.res.workflow.WorkflowInstanceOperationRes;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowInstanceService;
import com.alibaba.tesla.appmanager.workflow.util.WorkflowNetworkUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

/**
 * Workflow Instance 命令操作消息订阅处理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class WorkflowInstanceOperationCommandMessageSubscriber extends MessageListenerAdapter {

    @Autowired
    private RedisTemplate<String, String> template;

    @Autowired
    private WorkflowInstanceService workflowInstanceService;

    @Autowired
    private WorkflowNetworkUtil workflowNetworkUtil;

    @Override
    public void onMessage(@NotNull Message message, byte[] bytes) {
        String topic = template.getStringSerializer().deserialize(message.getChannel());
        String value = template.getStringSerializer().deserialize(message.getBody());
        if (!WorkflowInstanceMessageConfig.TOPIC_WORKFLOW_INSTANCE_OPERATION_COMMAND.equals(topic)) {
            log.error("received invalid message from redis channel|channel={}|value={}", topic, value);
            return;
        }

        // operator 转换到目标对象
        WorkflowInstanceOperationCommand operation;
        try {
            operation = JSONObject.parseObject(value, WorkflowInstanceOperationCommand.class);
            if (operation == null) {
                throw new RuntimeException("null operation");
            }
        } catch (Exception e) {
            log.error("[command] received invalid message from redis channel, cannot transform it to " +
                            "WorkflowInstanceOperationCommand|channel={}|value={}|exception={}", topic, value,
                    ExceptionUtils.getStackTrace(e));
            return;
        }
        String command = operation.getCommand();
        Long workflowInstanceId = operation.getWorkflowInstanceId();
        if (workflowInstanceId == null || workflowInstanceId <= 0) {
            log.error("[command] received invalid message from redis channel, empty workflow instance id|channel={}|" +
                            "value={}", topic, value);
            return;
        }

        // 根据不同的命令执行不同的 service 动作，如果本机执行成功，那么再次广播当前执行结果到全部节点
        log.info("[command] prepare to {} workflow instance|workflowInstanceId={}", command, workflowInstanceId);
        operate(workflowInstanceId, command);
    }

    /**
     * 终止指定工作流实例
     *
     * @param workflowInstanceId 工作流实例 ID
     */
    private void operate(Long workflowInstanceId, String command) {
        String clientHost = workflowNetworkUtil.getClientHost();
        WorkflowInstanceDO instance = workflowInstanceService.get(workflowInstanceId, true);
        if (instance == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "[command] workflow instance not exists");
        }
        if (!clientHost.equals(instance.getClientHost())) {
            log.info("[command] current client host is not equal to request workflow instance, skip|" +
                            "workflowInstanceId={}|currentClientHost={}|workflowClientHost={}",
                    workflowInstanceId, clientHost, instance.getClientHost());
            return;
        }

        // 如果 client_host 比对成功，属于自身管控工作流，那么执行本地终止命令
        boolean success;
        if (WorkflowInstanceOperationCommand.COMMAND_TERMINATE.equals(command)) {
            success = workflowInstanceService.localTerminate(instance);
        } else if (WorkflowInstanceOperationCommand.COMMAND_RESUME.equals(command)) {
            success = workflowInstanceService.localResume(instance);
        } else if (WorkflowInstanceOperationCommand.COMMAND_RETRY.equals(command)) {
            success = workflowInstanceService.localRetry(instance);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid command " + command);
        }
        WorkflowInstanceOperationRes result;
        if (success) {
            result = WorkflowInstanceOperationRes.builder()
                    .command(command)
                    .workflowInstanceId(workflowInstanceId)
                    .clientHost(clientHost)
                    .success(true)
                    .message("")
                    .build();
        } else {
            result = WorkflowInstanceOperationRes.builder()
                    .command(command)
                    .workflowInstanceId(workflowInstanceId)
                    .clientHost(clientHost)
                    .success(false)
                    .message("")
                    .build();
        }
        broadcastResultMessage(result);
        log.info("[command] {} of the workflow instance succeed and the message has been broadcast " +
                "to all nodes|workflowInstanceId={}|result={}", command, workflowInstanceId,
                JSONObject.toJSONString(result));
    }

    /**
     * 发送广播命令结果消息
     *
     * @param result 结果详细
     */
    private void broadcastResultMessage(WorkflowInstanceOperationRes result) {
        template.convertAndSend(WorkflowInstanceMessageConfig.TOPIC_WORKFLOW_INSTANCE_OPERATION_RESULT,
                JSONObject.toJSONString(result));
    }
}