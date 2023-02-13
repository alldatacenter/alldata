package cn.datax.service.data.masterdata.config;

import cn.datax.common.rabbitmq.config.RabbitMqConstant;
import cn.datax.common.utils.ThrowableUtil;
import cn.datax.service.data.masterdata.api.entity.ModelEntity;
import cn.datax.service.data.masterdata.dao.ModelDao;
import cn.datax.service.workflow.api.enums.VariablesEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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

    @Autowired
    private ModelDao modelDao;

    /**
     * 消费工作流 业务编码 5011
     * @param map
     * @param channel
     * @param message
     * @return
     * @throws Exception
     */
    @RabbitListener(bindings = @QueueBinding(exchange = @Exchange(name = RabbitMqConstant.TOPIC_EXCHANGE_WORKFLOW, type = "topic", durable = "true", autoDelete = "false"),
            key = { RabbitMqConstant.TOPIC_WORKFLOW_KEY + "5011" },
            value = @Queue(value = RabbitMqConstant.TOPIC_WORKFLOW_QUEUE, durable = "true", exclusive = "false", autoDelete = "false")))
    public void fanoutQueueRelease(Map map, Channel channel, Message message) throws Exception {
        try {
            log.info("接收到了消息：{}", map);
            String businessKey = (String) map.get(VariablesEnum.businessKey.toString());
            String businessCode = (String) map.get(VariablesEnum.businessCode.toString());
            String flowStatus = (String) map.get("flowStatus");
            LambdaUpdateWrapper<ModelEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(ModelEntity::getFlowStatus, flowStatus);
            updateWrapper.eq(ModelEntity::getId, businessKey);
            modelDao.update(null, updateWrapper);
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
