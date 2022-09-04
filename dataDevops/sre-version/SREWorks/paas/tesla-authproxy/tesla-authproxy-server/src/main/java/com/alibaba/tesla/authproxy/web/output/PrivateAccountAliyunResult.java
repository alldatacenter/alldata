package com.alibaba.tesla.authproxy.web.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateAccountAliyunResult {
    private String aliyunId;
    private String aliyunPk;
}
