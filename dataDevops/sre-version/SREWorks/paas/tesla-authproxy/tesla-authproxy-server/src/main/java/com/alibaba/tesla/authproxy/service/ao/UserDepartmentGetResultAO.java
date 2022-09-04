package com.alibaba.tesla.authproxy.service.ao;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDepartmentGetResultAO {

    private String userId;
    private String depId;
    private String depDesc;
}
