package org.dromara.cloudeon.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActiveAlertVO {
    private String alertName;
    private Integer alertId;
    private Date createTime;
    private String alertLevelMsg;
    private Integer serviceInstanceId;
    private Integer serviceRoleInstanceId;
    private String serviceInstanceName;
    private String serviceRoleLabel;
    private String hostname;
    private String info;
    private String advice;
}
