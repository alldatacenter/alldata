package org.dromara.cloudeon.controller.request;

import lombok.Data;

import java.util.List;

@Data
public class ValidServicesDepRequest {
    private Integer clusterId;
    private Integer stackId;
    private List<Integer> installStackServiceIds;
}
