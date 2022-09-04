package com.alibaba.tesla.gateway.domain.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RouteInfoQueryReq implements Serializable {
    private static final long serialVersionUID = 3498334720100574952L;

    /**
     * app id
     */
    private String appId;

    /**
     * 服务类型
     * @see com.alibaba.tesla.gateway.common.enums.ServerTypeEnum
     */
    @NotBlank
    private String serverType;

    /**
     * 环境
     */
    private String forwardEnv;
}
