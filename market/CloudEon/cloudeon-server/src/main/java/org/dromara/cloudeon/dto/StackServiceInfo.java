package org.dromara.cloudeon.dto;

import lombok.Data;

import java.util.List;

@Data
public class StackServiceInfo {

    private String name;
    private String label;
    private String description;
    private Boolean supportKerberos;
    private String version;
    private String dockerImage;
    private String runAs;
    private Dashboard dashboard;
    private List<String> dependencies;
    private List<String> customConfigFiles;
    private List<StackServiceRole> roles;
    private List<StackConfiguration> configurations;
    private List<String> persistencePaths;
}
