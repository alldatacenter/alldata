package com.alibaba.tesla.appmanager.workflow.service.pubsub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Workflow 实例消息配置
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Configuration
public class WorkflowInstanceMessageConfig {

    /**
     * TOPIC: Workflow 实例命令 (包括 resume / terminate / retry，均需要 publish 到所有 subscriber)
     */
    public static final String TOPIC_WORKFLOW_INSTANCE_OPERATION_COMMAND = "WORKFLOW_INSTANCE_OPERATION_COMMAND";

    /**
     * TOPIC: Workflow 实例命令执行结果
     */
    public static final String TOPIC_WORKFLOW_INSTANCE_OPERATION_RESULT = "WORKFLOW_INSTANCE_OPERATION_RESULT";

    @Autowired
    private WorkflowInstanceOperationCommandMessageSubscriber commandMessageSubscriber;

    @Autowired
    private WorkflowInstanceOperationResultMessageSubscriber resultMessageSubscriber;

    @Bean
    RedisMessageListenerContainer container(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(commandMessageSubscriber,
                new PatternTopic(TOPIC_WORKFLOW_INSTANCE_OPERATION_COMMAND));
        container.addMessageListener(resultMessageSubscriber,
                new PatternTopic(TOPIC_WORKFLOW_INSTANCE_OPERATION_RESULT));
        return container;
    }
}
