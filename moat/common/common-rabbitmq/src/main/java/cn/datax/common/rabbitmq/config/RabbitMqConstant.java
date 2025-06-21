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

}
