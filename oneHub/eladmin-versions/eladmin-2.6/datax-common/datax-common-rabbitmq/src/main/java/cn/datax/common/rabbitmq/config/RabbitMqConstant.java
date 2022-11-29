package cn.datax.common.rabbitmq.config;

public class RabbitMqConstant {

    /**
     * FANOUT类型的交换机：api发布与撤销
     */
    public static final String FANOUT_EXCHANGE_API = "fanout.exchange.api";

    /**
     * FANOUT类型的队列：api发布与撤销
     */
    public static final String FANOUT_API_QUEUE = "fanout.api.queue";

    /**
     * TOPIC类型的交换机：工作流
     */
    public static final String TOPIC_EXCHANGE_WORKFLOW = "topic.exchange.workflow";

    /**
     * TOPIC类型的队列：工作流
     */
    public static final String TOPIC_WORKFLOW_QUEUE = "topic.workflow.queue";

    /**
     * TOPIC类型的路由键：工作流 {}占位符替换
     */
    public static final String TOPIC_WORKFLOW_KEY = "topic.workflow.key.";
}
