package com.alibaba.tesla.authproxy.service.ao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPermissionCheckResultAO {

    private List<String> permissions;
}
