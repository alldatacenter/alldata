package com.alibaba.tesla.appmanager.domain.res.deploy;

import com.alibaba.tesla.appmanager.common.enums.DeployComponentStateEnum;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 获取 Component 的部署详情结果
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetDeployComponentHandlerRes implements Serializable {

    private static final long serialVersionUID = 5661997478887477035L;

    /**
     * 部署后的 ComponentSchema 对象
     */
    private ComponentSchema componentSchema;

    /**
     * 部署状态
     */
    private DeployComponentStateEnum status;

    /**
     * 解释信息
     */
    private String message;
}
