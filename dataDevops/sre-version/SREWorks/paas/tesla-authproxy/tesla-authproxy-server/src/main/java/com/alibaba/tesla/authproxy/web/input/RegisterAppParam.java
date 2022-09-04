package com.alibaba.tesla.authproxy.web.input;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 应用注册请求参数结构体
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class RegisterAppParam implements Serializable {

    /**
     * 应用标识
     */
    @NotBlank(message = "appId 不能为空")
    @Size(max = 45, message = "appId 长度必须不能超过45")
    String appId;

    /**
     * 授权accessKey
     */
    @NotBlank(message = "appAccessKey 不能为空")
    @Size(max = 128, message = "appAccessKey 长度必须不能超过128")
    String appAccessKey;

    /**
     * 登录后跳转的首页地址，开启登录验证时有效
     */
    @Size(max = 1000, message = "indexUrl 长度必须不能超过1000")
    String indexUrl;

    /**
     * 是否开启登录验证功能
     */
    int loginEnable;
}
