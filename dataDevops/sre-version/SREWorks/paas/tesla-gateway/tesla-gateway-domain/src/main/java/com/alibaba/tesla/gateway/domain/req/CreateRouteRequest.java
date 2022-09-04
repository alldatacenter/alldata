package com.alibaba.tesla.gateway.domain.req;

import com.alibaba.tesla.gateway.domain.req.RouteInfo;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 导入路由配置参数结构
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class CreateRouteRequest implements Serializable {
    private static final long serialVersionUID = -2505216274980987658L;

    private String newName = "route-config";

    private String newVersion = "1.0";

    @NotNull
    private RouteInfo route;

}
