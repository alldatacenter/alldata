package org.dromara.cloudeon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskModel {
    private String taskName;
    private Integer taskSortNum;
    private String roleName;
    private String hostName;
    private String ip;

    private String processorClassName;

}