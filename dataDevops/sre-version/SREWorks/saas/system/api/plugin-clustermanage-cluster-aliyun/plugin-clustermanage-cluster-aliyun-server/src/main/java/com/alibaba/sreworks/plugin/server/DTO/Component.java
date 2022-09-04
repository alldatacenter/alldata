package com.alibaba.sreworks.plugin.server.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Component {

    private String category;

    private String name;

    private String version;

    private String nextVersion;

    private Boolean canUpgrade;

    private Boolean isDeployed;

}
