package com.alibaba.tesla.authproxy.service.ao;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserPermissionListResultAO {

    private Long total;
    private List<UserPermissionGetResultAO> items;

    public UserPermissionListResultAO() {
        this.total = 0L;
        this.items = new ArrayList<>();
    }
}
