package org.dromara.cloudeon.controller.response;

import lombok.Data;

import java.util.List;

@Data
public class StackServiceVO {
    private String label;
    private String name;
    private Integer id;
    private String version;
    private String description;
    private String dockerImage;
    private String iconApp;
    private String iconDanger;
    private boolean supportKerberos;
    private String iconDefault;
    /**
     * 该集群是否已经安装过相同的框架服务
     */
    private boolean installedInCluster;

    /**
     * 角色
     */
    private List<String> roles;




}
