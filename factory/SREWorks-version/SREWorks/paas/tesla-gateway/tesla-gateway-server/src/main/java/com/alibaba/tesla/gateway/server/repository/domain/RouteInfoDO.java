package com.alibaba.tesla.gateway.server.repository.domain;

import com.alibaba.tesla.gateway.domain.req.AuthRedirection;
import com.alibaba.tesla.gateway.domain.req.BlackListConf;
import com.alibaba.tesla.gateway.domain.req.RouteRateLimit;
import lombok.*;
import java.util.Date;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RouteInfoDO {

    /**
     * 名称
     */
    private String name;

    /**
     * rote id
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
    private String url;

    /**
     * 路由方式， {@link com.alibaba.tesla.gateway.common.enums.RouteTypeEnum}
     */
    private String routeType;

    /**
     * 是否可用
     */
    private boolean enable;

    /**
     * 是否开启登录验证
     */
    private boolean authLogin;

    /**
     * 是否开启path权限验证
     */
    private boolean authCheck;

    /**
     * 兼容Tesla老的认证头信息认证
     */
    private boolean authHeader;

    /**
     * 是否启用swagger doc
     */
    private boolean enableSwaggerDoc;

    /**
     * doc uri
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
     * 超时
     */
    private Long timeout;

    /**
     * 转发
     */
    private String forwardEnv;

    /**
     * 多个用逗号隔开，/error/**,/doc.html
     * 忽略鉴权的uri，默认所有uri都要鉴权
     */
    private String authIgnorePath;

    /**
     * 鉴权重定向，如果401会走统一登录
     */
    private AuthRedirection authRedirect;

    /**
     * 顺序
     */
    private int order;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 服务类型
     */
    private String serverType;

    /**
     * appId
     */
    private String appId;

    /**
     * 是否启用函数
     */
    private boolean enableFunction;

}
