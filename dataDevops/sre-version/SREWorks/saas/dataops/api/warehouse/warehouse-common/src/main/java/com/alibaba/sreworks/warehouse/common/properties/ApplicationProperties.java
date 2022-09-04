package com.alibaba.sreworks.warehouse.common.properties;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Data
public class ApplicationProperties {

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.url}")
    private String datasetUrl;

    @Value("${spring.datasource.username}")
    private String datasetUsername;

    @Value("${spring.datasource.password}")
    private String datasetPassword;

    @Value("${spring.elasticsearch.rest.protocol}")
    private String esProtocol;

    @Value("${spring.elasticsearch.rest.host}")
    private String esHost;

    @Value("${spring.elasticsearch.rest.port}")
    private Integer esPort;

    @Value("${spring.elasticsearch.rest.username}")
    private String esUsername;

    @Value("${spring.elasticsearch.rest.password}")
    private String esPassword;
}