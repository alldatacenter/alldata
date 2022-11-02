package com.alibaba.tesla.appmanager.domain.res.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * App Package 同步到外部环境中
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageSyncExternalRes implements Serializable {

    private static final long serialVersionUID = 6672056486632902588L;

    /**
     * 远端返回的应用包 ID
     */
    private Long appPackageId;
}
