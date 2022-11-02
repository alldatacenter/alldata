package com.alibaba.tdata.aisp.server.common.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName: TaskRemainProperties
 * @Author: dyj
 * @DATE: 2021-12-08
 * @Description:
 **/
@Component
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "aisp.task.remain")
public class TaskRemainProperties {
    private Integer days=3;

}
