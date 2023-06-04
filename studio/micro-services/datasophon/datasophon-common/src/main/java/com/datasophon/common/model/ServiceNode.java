package com.datasophon.common.model;

import lombok.Data;

import java.util.List;

@Data
public class ServiceNode {

    private String serviceName;
    private List<ServiceRoleInfo> masterRoles ;
    private List<ServiceRoleInfo> elseRoles;

    private String commandId;

    private Integer serviceInstanceId;


}
