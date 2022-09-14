package com.alibaba.tdata.aisp.server.common.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName: CacheProperties
 * @Author: dyj
 * @DATE: 2021-11-25
 * @Description:
 **/
@Component
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "aisp.cache")
public class CacheProperties {
    private String type="local";

    private String uri;

    private Long expire=120L;
}
