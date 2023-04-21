package org.dromara.cloudeon.controller.response;

import lombok.Data;

import java.util.List;

@Data
public class ServiceInstanceVO {
    private String serviceName;
    private Integer id;
    private String serviceState;
    private Integer serviceStateValue;
    private String icon;
    private Integer alertMsgCnt;
    private List<String> alertMsgName;

}
