package com.alibaba.tesla.gateway.domain.dto;

import com.alibaba.tesla.gateway.common.enums.RouteTypeEnum;
import com.alibaba.tesla.gateway.domain.req.AuthRedirection;
import com.alibaba.tesla.gateway.domain.req.BlackListConf;
import com.alibaba.tesla.gateway.domain.req.RouteRateLimit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RouteInfoDTO implements Serializable {
    private static final long serialVersionUID = -9209500610962834409L;

    /**
     * name
     */
    @NotBlank(message = "name can't be empty")
    @Size(max=64, message = "reason's max length is 64")
    private String name;

    /**
     * route id
     */
    private String routeId;

    /**
     * 路由匹配path
     */
    private String path;

    /**
     * type为host时，通过配置host来转发
     */
    private String host;

    /**
     * 阶段，支持多个，多个用逗号分隔
     */
    private String stageId;

    /**
     * 路由目标地址
     */
    @Size(max=512, message = "reason's max length is 512")
    private String url;

    /**
     * 环境转发
     */
    private String forwardEnv;

    /**
     * 路由类别
     */
    private RouteTypeEnum routeType;


    /**
     * 是否可用
     * 0-不可用
     * 1-可用
     */
    private boolean enable;

    /**
     * 是否开启登录验证
     * 0-否
     * 1-是
     */
    private boolean authLogin;

    /**
     * 是否开启path权限验证
     */
    private boolean authCheck;

    /**
     * 兼容Tesla老的认证头信息认证 1-是 0-否
     */
    private boolean authHeader;

    /**
     * 启用swagger doc
     */
    private boolean enableSwaggerDoc;

    /**
     * doc uri
     * 默认v2/api-docs
     */
    private String docUri;

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
     * 黑名单配置
     */
    private BlackListConf blackListConf;

    /**
     * 忽略鉴权的uri，默认所有uri都要鉴权
     */
    private String authIgnorePath;

    /**
     * 鉴权重定向，如果401会走统一登录
     */
    private AuthRedirection authRedirect;

    /**
     * 超时
     */
    private Long timeout;

    /**
     * 顺序
     */
    private int order;

    /**
     * 描述信息
     */
    @Size(max=512, message = "description's max length is 512")
    private String description;

    /**
     * 服务类型
     */
    @NotBlank
    private String serverType;

    /**
     * appId
     */
    @NotBlank
    private String appId;

    /**
     * 是否启用函数
     */
    private boolean enableFunction;

}
