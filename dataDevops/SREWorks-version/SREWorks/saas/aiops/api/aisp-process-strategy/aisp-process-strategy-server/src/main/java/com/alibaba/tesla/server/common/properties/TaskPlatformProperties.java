package com.alibaba.tesla.server.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName: TaskPlatfromProperties
 * @Author: dyj
 * @DATE: 2022-02-28
 * @Description:
 **/
@Data
@Component
@ConfigurationProperties(value = "taskplatform")
public class TaskPlatformProperties {

    private String submitUrl;

    private String queryUrl;
}
