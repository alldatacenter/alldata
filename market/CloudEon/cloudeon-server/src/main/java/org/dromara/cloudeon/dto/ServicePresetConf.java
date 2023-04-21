package org.dromara.cloudeon.dto;

import lombok.Data;

@Data
public  class ServicePresetConf {

    private String name;
    private Integer id;
    private String value;
    private String recommendedValue;

}