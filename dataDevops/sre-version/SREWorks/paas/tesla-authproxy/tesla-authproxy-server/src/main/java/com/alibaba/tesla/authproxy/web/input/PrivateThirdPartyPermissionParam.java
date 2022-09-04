package com.alibaba.tesla.authproxy.web.input;

import lombok.Data;

/**
 * 专有云 - 第三方 ADS 数据库增加的参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
public class PrivateThirdPartyPermissionParam {
    private String pk;
    private String aliyunId;
    private String accessKeyId;
    private String accessKeySecret;
    private String phone;
}
