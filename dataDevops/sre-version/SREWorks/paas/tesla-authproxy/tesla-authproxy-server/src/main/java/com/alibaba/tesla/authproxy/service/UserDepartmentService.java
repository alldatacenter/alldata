package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.service.ao.UserDepartmentListResultAO;

public interface UserDepartmentService {

    UserDepartmentListResultAO getDepIdMapBySupervisorUser(String userId);
}
