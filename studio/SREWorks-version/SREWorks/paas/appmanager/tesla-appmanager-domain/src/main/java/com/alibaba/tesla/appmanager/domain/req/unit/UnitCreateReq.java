package com.alibaba.tesla.appmanager.domain.req.unit;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 单元创建请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitCreateReq implements Serializable {

    private static final long serialVersionUID = 3713947384802436082L;

    /**
     * 单元唯一标识
     */
    @NotBlank
    private String unitId;

    /**
     * 单元名称
     */
    @NotBlank
    private String unitName;

    /**
     * 代理 IP
     */
    private String proxyIp;

    /**
     * 代理端口
     */
    private String proxyPort;

    /**
     * Super Client ID
     */
    private String clientId;

    /**
     * Super Client Secret
     */
    private String clientSecret;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * abm-operator Endpoint
     */
    private String operatorEndpoint;

    /**
     * 扩展字段
     */
    private JSONObject extra;
}
