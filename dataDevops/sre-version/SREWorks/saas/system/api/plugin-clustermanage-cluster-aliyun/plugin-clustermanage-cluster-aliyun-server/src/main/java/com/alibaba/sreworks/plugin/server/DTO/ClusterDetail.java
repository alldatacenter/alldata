package com.alibaba.sreworks.plugin.server.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jinghua.yjh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterDetail {

    private String clusterType;

    private String regionId;

}
