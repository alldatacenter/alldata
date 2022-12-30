package com.alibaba.tesla.appmanager.domain.res.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 生成应用包的 launch yaml 返回对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageGetLaunchYamlRes implements Serializable {

    private static final long serialVersionUID = 4683891158458850041L;

    /**
     * Yaml
     */
    private String yaml;
}
