package com.alibaba.sreworks.dataset.common.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Data
public class ApplicationProperties {
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.dataset.jdbc-url}")
    private String datasetUrl;

    @Value("${spring.datasource.dataset.username}")
    private String datasetUsername;

    @Value("${spring.datasource.dataset.password}")
    private String datasetPassword;

    @Value("${spring.datasource.pmdb.jdbc-url}")
    private String pmdbUrl;

    @Value("${spring.datasource.pmdb.username}")
    private String pmdbUsername;

    @Value("${spring.datasource.pmdb.password}")
    private String pmdbPassword;

    @Value("${spring.skywalking.protocol}")
    private String skywalkingProtocol;

    @Value("${spring.skywalking.host}")
    private String skywalkingHost;

    @Value("${spring.skywalking.port}")
    private String skywalkingPort;
}