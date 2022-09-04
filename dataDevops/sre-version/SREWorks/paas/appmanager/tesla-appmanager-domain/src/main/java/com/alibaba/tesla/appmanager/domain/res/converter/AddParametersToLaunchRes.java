package com.alibaba.tesla.appmanager.domain.res.converter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 向 launch.yaml 中增加 parameterValues 参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AddParametersToLaunchRes implements Serializable {

    private static final long serialVersionUID = 7197364725123831203L;

    /**
     * launch YAML
     */
    private String launchYaml;
}
