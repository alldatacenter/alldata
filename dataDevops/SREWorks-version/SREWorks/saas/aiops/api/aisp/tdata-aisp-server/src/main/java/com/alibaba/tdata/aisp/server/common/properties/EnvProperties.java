package com.alibaba.tdata.aisp.server.common.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName: EnvProperties
 * @Author: dyj
 * @DATE: 2021-12-16
 * @Description:
 **/
@Component
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "aisp.env")
public class EnvProperties {
    private String stageId;
}
