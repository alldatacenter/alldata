package org.dromara.cloudeon.controller.request;

import org.dromara.cloudeon.dto.ServiceCustomConf;
import org.dromara.cloudeon.dto.ServicePresetConf;
import lombok.Data;

import java.util.List;

@Data
public class ServiceConfUpgradeRequest {
    private List<ServicePresetConf> presetConfList;
    private List<ServiceCustomConf> customConfList;
    private Integer serviceInstanceId;


}
