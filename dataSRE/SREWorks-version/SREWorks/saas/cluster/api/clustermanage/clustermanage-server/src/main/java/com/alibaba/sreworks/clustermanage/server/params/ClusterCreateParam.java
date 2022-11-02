package com.alibaba.sreworks.clustermanage.server.params;


import com.alibaba.sreworks.domain.DO.Cluster;

import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class ClusterCreateParam {

    private Long teamId;

    private Long accountId;

    private String clusterName;

    private String name;

    private String description;

    private String kubeconfig;

    private String deployClient;

    public Cluster toCluster(String operator) {
        return Cluster.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .creator(operator)
            .lastModifier(operator)
            .teamId(teamId)
            .accountId(accountId)
            .clusterName(clusterName)
            .name(name)
            .description(description)
            .build();
    }

}
