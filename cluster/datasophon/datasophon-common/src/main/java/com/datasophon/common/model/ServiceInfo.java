package com.datasophon.common.model;

import lombok.Data;

import java.util.List;

@Data
public class ServiceInfo {
    private String name;

    private String label;

    private String version;

    private String description;

    private List<ServiceRoleInfo> roles;

    private List<ServiceConfig> parameters;

    private List<String> dependencies;

    private ConfigWriter configWriter;

    private String packageName;

    private String decompressPackageName;

    private ExternalLink externalLink;

    private Integer sortNum;


}
