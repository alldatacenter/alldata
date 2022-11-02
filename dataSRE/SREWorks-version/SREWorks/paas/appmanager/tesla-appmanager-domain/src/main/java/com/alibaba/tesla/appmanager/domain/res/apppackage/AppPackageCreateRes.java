package com.alibaba.tesla.appmanager.domain.res.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 创建 App Package 响应
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageCreateRes implements Serializable {

    private static final long serialVersionUID = -2489023056077770711L;

    /**
     * App Package ID
     */
    private Long appPackageId;
}
