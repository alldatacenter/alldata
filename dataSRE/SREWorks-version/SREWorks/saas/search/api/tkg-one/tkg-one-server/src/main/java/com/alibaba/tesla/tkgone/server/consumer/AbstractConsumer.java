package com.alibaba.tesla.tkgone.server.consumer;

import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.Consumer;
import com.alibaba.tesla.tkgone.server.domain.ConsumerHistory;
import com.alibaba.tesla.tkgone.server.domain.ConsumerHistoryMapper;
import com.alibaba.tesla.tkgone.server.domain.ConsumerMapper;
import com.alibaba.tesla.tkgone.server.domain.dto.ConsumerDto;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author yangjinghua
 */
@Slf4j
@Service
public abstract class AbstractConsumer extends BasicConsumer {
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    @Autowired
    ConsumerMapper consumerMapper;
    @Autowired
    ConsumerHistoryMapper consumerHistoryMapper;
    @Value("${consumer.enable:true}")
    private Boolean enable;

    Map<Long, ScheduledFuture<?>> runningConsumer = new ConcurrentHashMap<>();

    public ScheduledExecutorService scheduledExecutorService;

    public void afterPropertiesSet(int concurrentExecNum, int effectiveInterval,
                                   ConsumerSourceType consumerSourceType) {
        log.info("Consumer={} enable={}", getClass().getSimpleName(), enable);
        if (enable) {
            scheduledExecutorService = Executors.newScheduledThreadPool(concurrentExecNum);
            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> runOnce(consumerSourceType), 0,
                effectiveInterval,
                TimeUnit.SECONDS);
        }
    }

    public void stopRunningConsumer(long id) {
        if (runningConsumer.containsKey(id)) {
            runningConsumer.get(id).cancel(true);
            while (true) {
                log.info("CONSUMER [" + id + "] IS CANCELING");
                if (runningConsumer.get(id).isDone() || runningConsumer.get(id).isCancelled()) {
                    runningConsumer.remove(id);
                    break;
                }
                Tools.sleepOneSecond();
            }
        }
        log.info(String.format("STOPPED_CONSUMER[%s]", id));
    }

    public void runOnce(ConsumerSourceType consumerSourceType) {
        try {
            Map<Long, ConsumerDto> consumerDtoMap = new HashMap<>(0);
            List<ConsumerDto> consumerDtoList = getLocalConsumerDtoList(consumerSourceType);
            List<Long> shouldRunningConsumerIdList = consumerDtoList.stream().map(Consumer::getId)
                .collect(Collectors.toList());
            consumerDtoList.forEach(x -> consumerDtoMap.put(x.getId(), x));

            List<Long> shouldRemoveConsumerIdList = new ArrayList<>();

            // 根据业务获取需要启动或者停止的作业
            for (long id : runningConsumer.keySet()) {
                if (shouldRunningConsumerIdList.contains(id)) {
                    shouldRunningConsumerIdList.remove(id);
                } else {
                    shouldRemoveConsumerIdList.add(id);
                }
            }

            // 停止冗余作业
            for (Long id : shouldRemoveConsumerIdList) {
                stopRunningConsumer(id);
            }

            // 启动作业
            for (Long id : shouldRunningConsumerIdList) {
                Run run = new Run(consumerDtoMap.get(id));
                ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(run, run.startPeriod,
                    run.interval, TimeUnit.SECONDS);
                runningConsumer.put(id, scheduledFuture);
                log.info(String.format("STARTED_CONSUMER[%s] %s %s %s", id, consumerDtoMap.get(id).getName(),
                    run.startPeriod, run.interval));
            }

        } catch (Exception e) {
            log.error(String.format("[runOnce]%s数据导入报错: ", consumerSourceType.toString()), e);
        }

    }

    @NoArgsConstructor
    class Run implements Runnable {

        ConsumerDto consumerDto;
        int interval;
        int startPeriod;
        int offset = 0;
        boolean isPartition;
        String sourceType;
        String dateMatch;

        Run(ConsumerDto consumerDto) {

            this.consumerDto = consumerDto;
            this.startPeriod = consumerDto.getSourceInfoJson().getIntValue("startPeriod");
            this.interval = consumerDto.getSourceInfoJson().getIntValue("interval");
            this.interval = this.interval == 0 ? 1 : this.interval;
            this.isPartition = consumerDto.getSourceInfoJson().getBooleanValue("isPartition");
            if (!StringUtils.isEmpty(consumerDto.getOffset()) && Tools.isNumber(consumerDto.getOffset())) {
                this.offset = Integer.parseInt(consumerDto.getOffset());
            }
            this.startPeriod = this.interval - ((int)(System.currentTimeMillis() / 1000) - this.offset);
            this.startPeriod = this.startPeriod < 0 ? 0 : this.startPeriod;
            this.sourceType = consumerDto.getSourceType();
            this.dateMatch = consumerDto.getSourceInfoJson().getString("dateMatch");
        }

        private boolean checkDateMatch(ConsumerDto consumerDto) {
            if (!StringUtils.isEmpty(this.dateMatch)) {
                String currentDate = Tools.currentDataString();
                return Pattern.compile(this.dateMatch).matcher(currentDate).find();
            }
            return true;
        }

        @Override
        public void run() {
            Tools.sleepOneSecond();
            try {
                Consumer consumer = new Consumer();
                consumer.setId(consumerDto.getId());

                ConsumerHistory consumerHistory = new ConsumerHistory();
                consumerHistory.setConsumerId(consumerDto.getId());
                consumerHistory.setGmtCreate(new Date());
                consumerHistory.setName(consumerDto.getName());
                consumerHistory.setStates("RUNNING");
                consumerHistory.setDetail("");
                consumerHistory.setGmtModified(new Date());
                consumerHistoryMapper.insert(consumerHistory);

                try {
                    if (checkDateMatch(consumerDto)) {
                        log.info(String.format("%s[%s]开始获取数据", this.sourceType, consumerDto.getName()));
                        long startTime = System.currentTimeMillis();
                        consumeStime = startTime;
                        int rows = consumerDataByConsumerDto(consumerDto);
                        long endTime = System.currentTimeMillis();
                        log.info(String.format("[consume]%s[%s]写入完成, 总耗时: %s ms", this.sourceType,
                                consumerDto.getName(),
                            endTime - startTime));
                        consumerHistory.setDetail(String.format("总耗时: %sms 抽取记录数：%d", (endTime - startTime), rows));
                    } else {
                        String detail = String.format("%s [%s] [%s] check dataMatch 不通过!未执行", consumerDto.getName(),
                            Tools.currentDataString(), this.dateMatch);
                        log.info(detail);
                        consumerHistory.setDetail(detail);
                    }
                    consumer.setStatus(STATUS_SUCCESS);
                    consumerHistory.setStates(STATUS_SUCCESS);
                    if (ConsumerSourceType.odpsTable.toString().equals(consumerDto.getSourceType())) {
                        // consumer.setOffset(consumerDto.getOffset());
                    } else {
                        consumer.setOffset(Long.toString(System.currentTimeMillis() / 1000));
                    }
                } catch (Throwable e) {
                    log.error(String.format("[consume]执行%s失败: ", consumerDto.getName()), e);
                    consumer.setStatus(STATUS_FAILED);
                    consumerHistory.setDetail(e.getMessage());
                    consumerHistory.setStates(STATUS_FAILED);
                    Tools.sleep(interval/2);
                }
                consumer.setGmtModified(new Date());
                consumerMapper.updateByPrimaryKeySelective(consumer);
                consumerHistory.setGmtModified(new Date());
                consumerHistoryMapper.updateByPrimaryKeySelective(consumerHistory);
                if (STATUS_FAILED.equals(consumer.getStatus())) {
                    Tools.sleepOneSecond();
                    stopRunningConsumer(consumerDto.getId());
                }
            } catch (Throwable e) {
                log.error(this.sourceType + "写入整体失败", e);
                Tools.sleep(60);
                stopRunningConsumer(consumerDto.getId());
            }
        }
    }

    /**
     * 用户自定义consumerDto中数据的消费方案
     *
     * @param consumerDto consumerDto数据
     * @throws Exception 所有数据都抛出来，会被记录到数据库中
     * @return 返回抽取记录数
     */
    abstract int consumerDataByConsumerDto(ConsumerDto consumerDto) throws Exception;

}
