package cn.datax.service.data.market.mapping.config;

import cn.datax.common.rabbitmq.config.RabbitMqConstant;
import cn.datax.common.utils.ThrowableUtil;
import cn.datax.service.data.market.api.entity.DataApiEntity;
import cn.datax.service.data.market.api.feign.DataApiServiceFeign;
import cn.datax.service.data.market.mapping.handler.MappingHandlerMapping;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Slf4j
@Configuration
public class RabbitMqListenerConfig {

    private static String HANDLER_RELEASE = "1";
    private static String HANDLER_CANCEL = "2";

    @Autowired
    private DataApiServiceFeign dataApiServiceFeign;

    @Autowired
    private MappingHandlerMapping mappingHandlerMapping;

    /**
     * api发布与撤销
     * @param map type 1:发布 2:撤销
     * @param channel
     * @param message
     * @return
     * @throws Exception
     */
    @RabbitListener(bindings = @QueueBinding(exchange = @Exchange(name = RabbitMqConstant.FANOUT_EXCHANGE_API, type = "fanout", durable = "true", autoDelete = "false"),
            value = @Queue(value = RabbitMqConstant.FANOUT_API_QUEUE, durable = "true", exclusive = "false", autoDelete = "false")))
    public void fanoutQueueRelease(Map map, Channel channel, Message message) throws Exception {
        try {
            String id = (String) map.get("id");
            String type = (String) map.get("type");
            log.info("fanoutQueueRelease接收到了：{},{}", id, type);
            DataApiEntity dataApiEntity = dataApiServiceFeign.getDataApiById(id);
            if (dataApiEntity != null) {
                if (HANDLER_RELEASE.equals(type)) {
                    mappingHandlerMapping.registerMapping(dataApiEntity);
                } else if (HANDLER_CANCEL.equals(type)) {
                    mappingHandlerMapping.unregisterMapping(dataApiEntity);
                }
            }
        } catch (Exception e) {
            log.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
            if (message.getMessageProperties().getRedelivered()){
                log.error("消息已处理,请勿重复处理！");
                // 拒绝消息
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            }else {
                //记录日志
                log.error("消息消费失败处理：{}", e.getMessage());
                //第一个参数为消息的index，第二个参数是是否批量处理，第三个参数为是否让被拒绝的消息重新入队列
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            }
        } finally {
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }
}
