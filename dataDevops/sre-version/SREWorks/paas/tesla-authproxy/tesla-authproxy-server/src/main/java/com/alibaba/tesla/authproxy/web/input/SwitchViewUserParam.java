package com.alibaba.tesla.authproxy.web.input;

import lombok.Data;

/**
 * 切换视图中增加用户参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
public class SwitchViewUserParam {

    /**
     * 工号
     */
    private String empId;
}
