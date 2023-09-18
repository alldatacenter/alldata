package org.dromara.cloudeon.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstanceDetailVO {
    /**
     * 服务实例名
     */
    private String name;
    /**
     * 服务实例id
     */
    private Integer id;
    private String dockerImage;
    private String stackServiceName;
    private Integer stackServiceId;
    private String version;
    private String stackServiceDesc;
    private String serviceState;
    private Integer serviceStateValue;
}
