package com.alibaba.tesla.authproxy.web.input;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 初始化应用的权限请求参数
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class InitAppPermissionParam implements Serializable {

    @NotBlank(message = "userId 不能为空")
    @Size(max = 45, message = "userId 长度必须不能超过45")
    String userId;

    @NotBlank(message = "appId 不能为空")
    @Size(max = 45, message = "appId 长度必须不能超过45")
    String appId;

    @NotBlank(message = "accessKey 不能为空")
    @Size(max = 128, message = "accessKey 长度必须不能超过128")
    String accessKey;

}
