package com.alibaba.tesla.authproxy.web.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleListParam {

    /**
     * 应用 ID
     */
    private String appId;
}
