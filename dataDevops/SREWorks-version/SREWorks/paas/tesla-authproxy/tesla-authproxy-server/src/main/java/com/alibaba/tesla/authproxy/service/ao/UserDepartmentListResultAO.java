package com.alibaba.tesla.authproxy.service.ao;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserDepartmentListResultAO {

    private Long total;
    private List<UserDepartmentGetResultAO> items;

    public UserDepartmentListResultAO() {
        this.total = 0L;
        this.items = new ArrayList<>();
    }
}
