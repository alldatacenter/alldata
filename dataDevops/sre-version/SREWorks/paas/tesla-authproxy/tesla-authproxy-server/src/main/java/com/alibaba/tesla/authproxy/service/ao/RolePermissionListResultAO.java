package com.alibaba.tesla.authproxy.service.ao;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RolePermissionListResultAO {

    private Long total;
    private List<RolePermissionGetResultAO> items;

    public RolePermissionListResultAO() {
        this.total = 0L;
        this.items = new ArrayList<>();
    }
}
