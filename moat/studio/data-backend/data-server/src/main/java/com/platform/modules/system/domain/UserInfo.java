package com.platform.modules.system.domain;

import com.platform.modules.system.service.dto.UserDto;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfo implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 用户基本信息
     */
    private UserDto userVo;
    /**
     * 权限标识集合
     */
    private String[] perms;
}
