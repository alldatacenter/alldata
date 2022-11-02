package com.alibaba.tesla.gateway.domain.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlackListConf implements Serializable {
    private static final long serialVersionUID = 3100155399583429653L;

    /**
     * user 白名单
     */
    private List<String> userBlackList;

    /**
     * app 白名单
     */
    private List<String> appBlackList;

    /**
     * ip 黑名单
     */
    private List<String> ipBlackList;
}
