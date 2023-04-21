package org.dromara.cloudeon.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceInstanceRoleVO {

    /**
     * 角色实例名称
     */
    private String name;
    /**
     * 角色实例id
     */
    private Integer id;
    private String roleStatus;
    private Integer roleStatusValue;
    private Integer nodeId;
    private String nodeHostname;
    private String nodeHostIp;
    private List<String> uiUrls;
    private Integer alertMsgCnt;
    private List<String> alertMsgName;
}
