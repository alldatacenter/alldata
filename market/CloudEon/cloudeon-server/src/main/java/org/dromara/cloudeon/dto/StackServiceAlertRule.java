package org.dromara.cloudeon.dto;

import lombok.Data;

import java.util.List;

@Data
public class StackServiceAlertRule {

    private String alert;
    private String promql;
    private String alertLevel;
    private String serviceRoleName;
    private String alertAdvice;
    private String alertInfo;


}
