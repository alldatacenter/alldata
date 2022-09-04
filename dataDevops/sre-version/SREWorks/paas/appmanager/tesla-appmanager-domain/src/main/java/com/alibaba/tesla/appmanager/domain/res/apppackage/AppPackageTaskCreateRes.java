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
public class AppPackageTaskCreateRes implements Serializable {

    private static final long serialVersionUID = -1509758673039186253L;

    /**
     * App Package 创建任务 ID
     */
    private Long appPackageTaskId;

    /**
     * Package Version
     */
    private String packageVersion;
}
