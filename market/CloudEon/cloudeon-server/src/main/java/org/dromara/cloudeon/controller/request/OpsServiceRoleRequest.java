package org.dromara.cloudeon.controller.request;

import lombok.Data;

import java.util.List;

@Data
public class OpsServiceRoleRequest {
    private Integer serviceInstanceId;
    List<Integer> roleInstanceIds;
}
