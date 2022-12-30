package com.alibaba.tesla.gateway.log.domain;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@ToString
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyTeslaHeaderResultLog implements Serializable {
    private static final long serialVersionUID = 6864415767347382485L;

    private String requestId;

    private String requestPath;

    private String createTime;

    private String routeId;

    private String content;
}
