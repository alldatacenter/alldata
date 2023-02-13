package com.alibaba.tdata.aisp.server.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @ClassName: AispMicrometerConfiguration
 * @Author: dyj
 * @DATE: 2021-11-25
 * @Description:
 **/
@Slf4j
@Component
public class AispRegister {
    private static final String METRICS_AISP_REQUEST = "metrics_aisp_request";

    @Autowired
    private MeterRegistry meterRegistry;

    public void record(String sceneCode, String detectorCode, double cost){
        Tags tags = Tags.of("sceneCode", sceneCode, "detectorCode", detectorCode);
        meterRegistry.summary(METRICS_AISP_REQUEST, tags).record(cost);
    }
}
