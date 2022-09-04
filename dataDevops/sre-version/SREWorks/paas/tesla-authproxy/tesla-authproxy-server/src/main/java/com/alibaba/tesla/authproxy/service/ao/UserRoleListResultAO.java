package com.alibaba.tesla.authproxy.service.ao;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserRoleListResultAO {

    private Long total;
    private List<UserRoleGetResultAO> items;

    public UserRoleListResultAO() {
        this.total = 0L;
        this.items = new ArrayList<>();
    }
}
