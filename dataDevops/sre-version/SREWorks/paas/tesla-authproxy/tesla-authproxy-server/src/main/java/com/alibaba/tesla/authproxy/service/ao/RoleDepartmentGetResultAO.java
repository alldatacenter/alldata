package com.alibaba.tesla.authproxy.service.ao;

import com.alibaba.tesla.authproxy.model.UserRoleRelDO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleDepartmentGetResultAO {

    private String userId;

    public static RoleDepartmentGetResultAO from(UserRoleRelDO rel) {
        return RoleDepartmentGetResultAO.builder()
            .userId(rel.getUserId())
            .build();
    }
}
