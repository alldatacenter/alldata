package org.dromara.cloudeon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoleNodeInfo {
    /**
     * 角色实例id
     */
    private Integer id;
    private String hostname;
    private String roleName;

}
