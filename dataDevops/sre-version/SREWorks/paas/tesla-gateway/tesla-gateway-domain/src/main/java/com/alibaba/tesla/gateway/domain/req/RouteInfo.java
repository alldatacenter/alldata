package com.alibaba.tesla.gateway.domain.req;


import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;

/**
 * @author tony.ly@alibaba-inc.com
 *         tandong.td@alibaba-inc.com
 *
 */
@ToString
@Data
public class RouteInfo {

    /**
     * 路由id
     * 上到生产后再删除
     */
    @Deprecated
    private long id;

    /**
     * 名称
     */
    @NotBlank(message = "name can't be empty")
    @Size(max=64, message = "reason's max length is 64")
    private String name;

    /**
     * 路由匹配path
     */
    @NotBlank(message = "path can't be empty")
    @Size(max=128, message = "reason's max length is 128")
    private String path;

    /**
     * 路由目标地址
     */
    @Size(max=512, message = "reason's max length is 512")
    private String url;

    /**
     * 服务名称，便于后续扩展支持根据path路由的服务注册中心
     */
    @Size(max=512, message = "serviceId's max length is 512")
    private String serviceId;

    /**
     * 是否跳过前缀
     * true，转发时不使用前缀
     * false，转发时将path进行拼接
     */
    private boolean stripPrefix = true;

    /**
     * 是否可用
     * 0-不可用
     * 1-可用
     */
    private int enable;

    /**
     * 是否开启登录验证
     * 0-否
     * 1-是
     */
    private int authLogin;

    /**
     * 是否开启path权限验证
     * 0-否
     * 1-是
     */
    private int authCheck;

    /**
     * 兼容Tesla老的认证头信息认证 1-是 0-否
     */
    private int authHeader;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 路由的限流策略
     */
    private RouteRateLimit rateLimit;

    /**
     * 超时
     */
    private Long timeout;

    /**
     * 顺序
     */
    private int order;

    /**
     * todo
     * @CommaListValidatorAnnotation(max = 10, message = "addHeaders max size is 10")111
     * 转发路由时需要追加的headers，最多支持追加10个header
     */
    private List<AddHeader> addHeaders;

    /**
     * 描述信息
     */
    @Size(max=512, message = "description's max length is 512")
    private String description;

    /**
     * 服务类型
     */
    private String serverType;

    /**
     * appId
     */
    private String appId;
}
