package com.alibaba.tesla.authproxy.web.input;

import com.alibaba.tesla.authproxy.model.PermissionMetaDO;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 创建并初始化应用的权限请求参数结构体
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class InitAppPermissionMetaParam implements Serializable {

    @NotBlank(message = "userId 不能为空")
    @Size(max = 45, message = "userId 长度必须不能超过45")
    String userId;

    /**
     * 应用标识
     */
    @NotBlank(message = "appId 不能为空")
    @Size(max = 45, message = "appId 长度必须不能超过45")
    String appId;

    /**
     * 权限元数据
     */
    @NotNull(message = "permissionMetaDO can't be null")
    PermissionMetaDO permissionMetaDO;
}
