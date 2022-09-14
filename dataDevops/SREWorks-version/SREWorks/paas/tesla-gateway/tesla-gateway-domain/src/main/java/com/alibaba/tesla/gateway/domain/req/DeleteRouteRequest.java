package com.alibaba.tesla.gateway.domain.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 删除路由配置请求参数
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class DeleteRouteRequest implements Serializable {
    private static final long serialVersionUID = 3608554196160999678L;

    /**
     * 路由名称
     */
    @NotBlank(message = "name can't be empty")
    @Size(max=64, message = "name's max length is 64")
    private String name;

    /**
     * 路由路径
     */
    @NotBlank(message = "path can't be empty")
    @Size(max=128, message = "path's max length is 128")
    private String path;

    /**
     * 删除配置之后的新版本，可空
     */
    private String newVersion;

    /**
     * 删除配置之后的新的路由配置名称，可空
     */
    private String newName;

}
