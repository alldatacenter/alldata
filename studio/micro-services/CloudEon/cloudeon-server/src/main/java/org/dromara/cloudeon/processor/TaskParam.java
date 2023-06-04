package org.dromara.cloudeon.processor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskParam {
    private Integer commandTaskId;
    private Integer commandId;

    /**
     * 任务关联的服务实例id
     */
    private Integer serviceInstanceId;
    private String serviceInstanceName;
    private Integer stackServiceId;
    private String taskName;
    /**
     * 任务关联的角色名
     */
    private String roleName;
    /**
     * 任务执行的主机host
     */
    private String hostName;
    /**
     * 任务执行的主机ip
     */
    private String ip;


}
