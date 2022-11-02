package com.alibaba.tesla.gateway.domain.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionServerReq implements Serializable {
    private static final long serialVersionUID = -4543241502773472322L;

    private String appId;

}
