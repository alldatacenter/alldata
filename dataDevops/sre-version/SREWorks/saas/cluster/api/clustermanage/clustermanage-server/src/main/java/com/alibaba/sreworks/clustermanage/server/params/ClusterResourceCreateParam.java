package com.alibaba.sreworks.clustermanage.server.params;


import com.alibaba.sreworks.domain.DO.ClusterResource;

import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class ClusterResourceCreateParam {

    private Long clusterId;

    private Long accountId;

    private String resourceType;

    private String instanceName;

    private String name;

    private String description;

    public ClusterResource toClusterResource(String operator) {
        return ClusterResource.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .creator(operator)
            .lastModifier(operator)
            .clusterId(clusterId)
            .accountId(accountId)
            .type(resourceType)
            .instanceName(instanceName)
            .name(name)
            .description(description)
            .build();
    }

}
