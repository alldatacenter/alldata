package com.alibaba.tesla.authproxy.service.ao;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserListResultAO {

    private Long total;
    private List<UserGetResultAO> items;

    public UserListResultAO() {
        this.total = 0L;
        this.items = new ArrayList<>();
    }
}
