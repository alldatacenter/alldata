package com.alibaba.tesla.authproxy.web.input;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 开启某个应用的服务权限
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class EnablePermissionMetaParam implements Serializable {

    @NotBlank(message = "appId 不能为空")
    @Size(max = 45, message = "appId 长度必须不能超过45")
    String appId;

    @NotBlank(message = "serviceCode 不能为空")
    @Size(max = 45, message = "serviceCode 长度必须不能超过45")
    String serviceCode;

    /**
     * 为空时开启所有权限
     */
    List<Long> permissionMetaIds;
}
