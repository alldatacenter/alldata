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
public class Instance {

    private String name;

    private String alias;

    private String status;

    private Boolean isNormal;

    private InstanceDetail detail;

}
