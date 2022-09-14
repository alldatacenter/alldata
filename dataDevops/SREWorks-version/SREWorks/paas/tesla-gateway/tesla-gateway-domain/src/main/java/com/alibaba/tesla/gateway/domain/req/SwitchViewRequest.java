package com.alibaba.tesla.gateway.domain.req;

import lombok.Data;

import java.io.Serializable;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
public class SwitchViewRequest implements Serializable {
	private static final long serialVersionUID = 4942399765532166120L;

	/**
	 * 要变成的人的 EmpId
	 */
	private String switchEmpId;
}
