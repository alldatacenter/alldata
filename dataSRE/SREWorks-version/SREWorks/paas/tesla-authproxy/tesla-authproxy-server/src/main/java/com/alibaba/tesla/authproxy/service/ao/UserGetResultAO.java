package com.alibaba.tesla.authproxy.service.ao;

import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.UserExtDO;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class UserGetResultAO {

    private String tenantId;
    private String userId;
    private String empId;
    private String nickname;
    private String email;
    private String depId;
    private List<UserRoleGetResultAO> roles;
    private UserExtDO ext;

    /**
     * 根据 UserDO 构造当前返回内容数据
     *
     * @param user 用户对象
     * @return UserGetResultAO
     */
    public static UserGetResultAO from(UserDO user) {
        List<UserRoleGetResultAO> userRoles = new ArrayList<>();
        if (user.getRoles() != null && user.getRoles().size() > 0) {
            userRoles = user.getRoles().stream().map(UserRoleGetResultAO::from).collect(Collectors.toList());
        }
        return UserGetResultAO.builder()
            .tenantId(user.getTenantId())
            .userId(user.getUserId())
            .empId(user.getEmpId())
            .nickname(user.getNickName())
            .email(user.getEmail())
            .depId(user.getDepId())
            .roles(userRoles)
            .ext(user.getExt())
            .build();
    }
}
