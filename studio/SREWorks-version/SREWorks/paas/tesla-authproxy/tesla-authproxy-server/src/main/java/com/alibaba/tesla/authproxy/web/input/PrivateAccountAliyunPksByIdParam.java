package com.alibaba.tesla.authproxy.web.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 根据 aliyun id 获取 aliyun pk 的参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateAccountAliyunPksByIdParam {
    private String aliyunIds;
}

