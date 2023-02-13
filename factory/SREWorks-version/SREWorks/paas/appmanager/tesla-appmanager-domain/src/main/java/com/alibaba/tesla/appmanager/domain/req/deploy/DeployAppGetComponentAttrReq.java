package com.alibaba.tesla.appmanager.domain.req.deploy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 查询指定 ComponentPackage 的部署单运行状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppGetComponentAttrReq implements Serializable {

    private static final long serialVersionUID = 3256022989002584168L;

    private Long deployComponentId;
}
