package com.alibaba.tesla.authproxy.web.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * 根据 aliyun pk 获取 access keys 列表
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateAccountAccessKeysByPkParam {
    @NotEmpty
    private String aliyunPk;
}

