package com.alibaba.sreworks.plugin.server.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jinghua.yjh
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceDetail {

    private String architectureType;

    private String regionId;

    private String instanceId;

    private String connectionDomain;

    private Long port;

}
