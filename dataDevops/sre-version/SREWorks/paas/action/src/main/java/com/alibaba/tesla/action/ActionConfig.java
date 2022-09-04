package com.alibaba.tesla.action;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
public class ActionConfig {

    @Value("${kg.search.endpoint}")
    private String searchEndpoint;

    @Value("${kg.insert.endpoint}")
    private String insertEndpoint;
}