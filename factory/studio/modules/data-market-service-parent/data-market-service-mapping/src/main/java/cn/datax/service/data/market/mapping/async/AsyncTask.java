package cn.datax.service.data.market.mapping.async;

import cn.datax.service.data.market.api.dto.ApiLogDto;
import cn.datax.service.data.market.mapping.service.ApiLogService;
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
    private ApiLogService apiLogService;

    @Async("taskExecutor")
    public void doTask(ApiLogDto apiLogDto) {
        apiLogService.saveApiLog(apiLogDto);
    }
}
