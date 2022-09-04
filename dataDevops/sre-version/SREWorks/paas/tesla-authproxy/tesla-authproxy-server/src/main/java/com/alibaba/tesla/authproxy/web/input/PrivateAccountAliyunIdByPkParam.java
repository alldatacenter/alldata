package com.alibaba.tesla.authproxy.web.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 根据 aliyun pk 获取 aliyun id 的参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateAccountAliyunIdByPkParam {
    private String aliyunPk;
}

