package com.alibaba.tesla.gateway.domain.req;

import com.alibaba.tesla.gateway.domain.req.RouteInfo;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 更新路由配置请求参数
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class UpdateRouteRequest implements Serializable {
    private static final long serialVersionUID = -1052674505333257309L;


    /**
     * 要更新的路由名称
     */
    @NotBlank(message = "name can't be empty")
    @Size(max=64, message = "name's max length is 64")
    private String name;

    /**
     * 要更新的路由path
     */
    @NotBlank(message = "path can't be empty")
    @Size(max=128, message = "path's max length is 128")
    private String path;

    /**
     * 更新之后新的路由信息
     */
    @NotNull
    private RouteInfo newRoute;

}
