package org.dromara.cloudeon.dto;

import lombok.Data;

@Data
public class ServiceCustomConf {

    private String name;
    private Integer id;
    private String value;
    /**
     * 所属配置文件
     */
    private String confFile;

}