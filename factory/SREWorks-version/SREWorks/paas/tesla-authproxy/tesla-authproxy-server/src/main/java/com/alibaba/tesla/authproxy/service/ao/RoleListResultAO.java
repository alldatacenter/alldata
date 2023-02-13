package com.alibaba.tesla.authproxy.service.ao;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoleListResultAO {

    private Long total;
    private List<RoleGetResultAO> items;

    public RoleListResultAO() {
        this.total = 0L;
        this.items = new ArrayList<>();
    }
}
