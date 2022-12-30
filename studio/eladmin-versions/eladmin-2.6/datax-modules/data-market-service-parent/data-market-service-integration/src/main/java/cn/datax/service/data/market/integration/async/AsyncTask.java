package cn.datax.service.data.market.integration.async;

import cn.datax.service.data.market.api.dto.ServiceLogDto;
import cn.datax.service.data.market.integration.service.ServiceLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步处理
 */
@Slf4j
@Component
public class AsyncTask {

    @Autowired
    private ServiceLogService serviceLogService;

    @Async("taskExecutor")
    public void doTask(ServiceLogDto serviceLog) {
        serviceLogService.saveServiceLog(serviceLog);
    }
}
