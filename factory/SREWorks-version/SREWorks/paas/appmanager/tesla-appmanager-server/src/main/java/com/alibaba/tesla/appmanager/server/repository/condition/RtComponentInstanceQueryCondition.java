package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 实时组件实例查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RtComponentInstanceQueryCondition extends BaseCondition {

    private String componentInstanceId;

    private String appInstanceId;

    private String appId;

    private String appIdStartsWith;

    private String componentType;

    private String componentName;

    private String componentNameStartsWith;

    private String clusterId;

    private String namespaceId;

    private String stageId;

    private String version;

    private String status;

    private List<String> statusList;

    private String watchKind;

    private Long timesGreaterThan;

    private Long timesLessThan;

    private String optionKey;

    private String optionValue;
}
