
package com.platform.modules.mnt.service.dto;

import lombok.Data;
import java.io.Serializable;
import java.sql.Timestamp;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Data
public class DeployHistoryDto implements Serializable {

	/**
	 * 编号
	 */
    private String id;

	/**
	 * 应用名称
	 */
    private String appName;

	/**
	 * 部署IP
	 */
    private String ip;

	/**
	 * 部署时间
	 */
	private Timestamp deployDate;

	/**
	 * 部署人员
	 */
	private String deployUser;

	/**
	 * 部署编号
	 */
	private Long deployId;
}
