package com.alibaba.tesla.authproxy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 切换视图用户清单
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchViewUserDO implements Serializable {

    private Long id;

    /**
     * 工号
     */
    private String empId;

    /**
     * 用户名称
     */
    private String loginName;

    /**
     * Buc ID
     */
    private String bucId;

    private Date gmtCreate;

    private Date gmtModified;
}
