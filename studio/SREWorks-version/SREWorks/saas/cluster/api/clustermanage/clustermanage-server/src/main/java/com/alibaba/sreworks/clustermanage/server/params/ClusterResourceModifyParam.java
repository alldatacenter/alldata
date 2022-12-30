package com.alibaba.sreworks.clustermanage.server.params;


import com.alibaba.sreworks.domain.DO.ClusterResource;

import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class ClusterResourceModifyParam {

    private Long id;

    private String name;

    private String description;

    public void patchClusterResource(ClusterResource clusterResource, String operator) {
        clusterResource.setGmtModified(System.currentTimeMillis() / 1000);
        clusterResource.setLastModifier(operator);
        clusterResource.setName(name);
        clusterResource.setDescription(description);
    }

}
