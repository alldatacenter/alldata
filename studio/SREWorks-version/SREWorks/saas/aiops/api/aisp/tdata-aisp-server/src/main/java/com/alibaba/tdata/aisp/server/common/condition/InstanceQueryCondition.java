package com.alibaba.tdata.aisp.server.common.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: InstanceQueryCondition
 * @Author: dyj
 * @DATE: 2021-11-16
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceQueryCondition {
    private String instanceCode;

    private String sceneCode;

    private String detectorCode;

    private String entityId;
}
