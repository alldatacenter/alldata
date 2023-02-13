package com.alibaba.tesla.authproxy.service.ao;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoleDepartmentListResultAO {

    private Long total;
    private List<RoleDepartmentGetResultAO> items;

    public RoleDepartmentListResultAO() {
        this.total = 0L;
        this.items = new ArrayList<>();
    }
}
