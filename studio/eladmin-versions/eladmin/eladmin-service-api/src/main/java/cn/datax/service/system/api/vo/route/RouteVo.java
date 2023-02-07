package cn.datax.service.system.api.vo.route;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RouteVo implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 请求url
     */
    private String path;
    /**
     * 对应组件
     */
    private String component;
    /**
     * 父级菜单重定向地址
     */
    private String redirect;
    /**
     * 路由名称
     */
    private String name;
    /**
     * 路由附带内容
     */
    private MetaVo meta;
    /**
     * 隐藏
     */
    private Boolean hidden;
    /**
     * 子路由
     */
    private List<RouteVo> children;
}
