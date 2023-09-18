package org.dromara.cloudeon.dto;

import org.dromara.cloudeon.enums.TaskGroupType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTaskGroupType {
    private Integer id;
    private String stackServiceName;
    private String serviceName;
    private LinkedHashMap<String,List<NodeInfo>> roleHostMaps;

    private List<TaskGroupType> taskGroupTypes;



}
