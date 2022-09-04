package com.alibaba.tesla.appmanager.domain.res.deploy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 发起一次 AppPackage 的部署单响应
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppPackageLaunchRes implements Serializable {

    private static final long serialVersionUID = -7588408452390786573L;

    /**
     * App 部署单 ID，供后续查询
     */
    private long deployAppId;
}
