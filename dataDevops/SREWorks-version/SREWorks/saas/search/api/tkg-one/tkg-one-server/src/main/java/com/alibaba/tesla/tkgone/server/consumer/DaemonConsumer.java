package com.alibaba.tesla.tkgone.server.consumer;

import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.ConsumerNodeExample;
import com.alibaba.tesla.tkgone.server.domain.dto.ConsumerNodeDto;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author yangjinghua
 */
@Service
@Log4j
public class DaemonConsumer extends BasicConsumer implements InitializingBean {

    private String localIp = Tools.getLocalIp();

    @Override
    public void afterPropertiesSet() {

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
        scheduledExecutorService.scheduleAtFixedRate(this::heartBeat, 1,
            consumerConfigService.getConsumerNodeHeartBeatInterval(), TimeUnit.SECONDS);

    }

    private void heartBeat() {
        try {
            ConsumerNodeDto consumerNodeDto = new ConsumerNodeDto();
            consumerNodeDto.setHost(localIp);
            consumerNodeDto.setGmtCreate(new Date());
            consumerNodeDto.setGmtModified(new Date());

            ConsumerNodeExample consumerNodeExample = new ConsumerNodeExample();
            consumerNodeExample.createCriteria().andHostEqualTo(localIp);

            if (CollectionUtils.isEmpty(consumerNodeMapper.selectByExample(consumerNodeExample))) {
                consumerNodeMapper.insertSelective(consumerNodeDto.toConsumerNode());
            } else {
                consumerNodeMapper.updateByExampleSelective(consumerNodeDto.toConsumerNode(), consumerNodeExample);
            }
        } catch (Exception e) {
            log.info("HEART_BEAT_ERROR", e);
        }
    }

}
