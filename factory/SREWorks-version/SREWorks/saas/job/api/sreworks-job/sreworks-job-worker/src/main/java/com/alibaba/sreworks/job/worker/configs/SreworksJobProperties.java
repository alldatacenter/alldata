package com.alibaba.sreworks.job.worker.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sreworks.job")
public class SreworksJobProperties {

    private String masterEndpoint;

}
