package com.alibaba.tesla.server.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName: AispProperties
 * @Author: dyj
 * @DATE: 2022-03-01
 * @Description:
 **/
@Data
@Component
@ConfigurationProperties(value = "aisp")
public class AispProperties {
    private String url;
}
