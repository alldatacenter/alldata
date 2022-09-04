package com.alibaba.tesla.gateway.domain.res;

import lombok.*;

import java.io.Serializable;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RouteOperatorRes implements Serializable {
    private static final long serialVersionUID = -6611465066864626198L;

    /**
     * 状态
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMsg;
}
