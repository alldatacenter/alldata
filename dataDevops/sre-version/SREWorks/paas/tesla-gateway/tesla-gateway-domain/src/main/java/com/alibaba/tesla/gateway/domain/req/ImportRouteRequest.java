package com.alibaba.tesla.gateway.domain.req;

import com.alibaba.tesla.gateway.domain.req.RouteInfo;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 导入路由配置参数结构
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class ImportRouteRequest implements Serializable {
    private static final long serialVersionUID = -4834049660250920330L;

    /**
     * 路由配置列表
     */
    @NotNull(message = "routes can't be empty and size must be greater than zero.")
    private List<RouteInfo> routes;
}
