package com.elasticsearch.cloud.monitor.metric.common.pojo;

import lombok.Builder;
import lombok.Data;

/**
 * 故障实例
 *
 * @author: fangzong.lyj
 * @date: 2022/01/27 17:28
 */
@Data
@Builder
public class FailureInstance {

    Integer failureDefId;

    String appInstanceId;

    String appComponentInstanceId;

    Long incidentId;

    String name;

    String level;

    Long occurTs;

    Long recoveryTs;

    String content;
}
