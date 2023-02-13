package com.alibaba.sreworks.clustermanage.server.params;


import com.alibaba.sreworks.domain.DO.Cluster;

import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class ClusterModifyParam {

    private String name;

    private String description;

    public void patchCluster(Cluster cluster, String operator) {
        cluster.setGmtModified(System.currentTimeMillis() / 1000);
        cluster.setLastModifier(operator);
        cluster.setName(name);
        cluster.setDescription(description);
    }

}
