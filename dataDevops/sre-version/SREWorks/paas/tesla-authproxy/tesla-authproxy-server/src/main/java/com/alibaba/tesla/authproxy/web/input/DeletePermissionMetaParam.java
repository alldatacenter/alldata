package com.alibaba.tesla.authproxy.web.input;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 删除权限元数据请求参数结构体
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class DeletePermissionMetaParam implements Serializable {

    /**
     * 应用标识
     */
    @NotBlank(message = "appId 不能为空")
    @Size(max = 45, message = "appId 长度必须不能超过45")
    String appId;

    /**
     * 权限元数据标识
     */
    @NotBlank(message = "permissionCode 不能为空")
    @Size(max = 100, message = "permissionCode 长度必须不能超过100")
    String permissionCode;

    /**
     * 操作用户工号
     */
    @NotBlank(message = "userId 不能为空")
    @Size(max = 20, message = "userId 长度必须不能超过20")
    String userId;

    /**
     * 是否删除ACL或OAM中对应的权限
     */
    boolean delAclOam = true;
}
