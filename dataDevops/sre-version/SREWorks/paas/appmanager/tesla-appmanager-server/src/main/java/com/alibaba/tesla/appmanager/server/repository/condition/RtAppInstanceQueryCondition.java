package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 实时应用实例查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RtAppInstanceQueryCondition extends BaseCondition {

    private String appInstanceId;

    private String appId;

    private String clusterId;

    private String version;

    private String status;

    private Boolean visit;

    private Boolean upgrade;

    private String namespaceId;

    private String stageId;

    private String optionKey;

    private String optionValue;
}
