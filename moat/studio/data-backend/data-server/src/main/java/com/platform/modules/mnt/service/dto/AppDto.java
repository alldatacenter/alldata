
package com.platform.modules.mnt.service.dto;

import lombok.Getter;
import lombok.Setter;
import com.platform.base.BaseDTO;
import java.io.Serializable;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Getter
@Setter
public class AppDto extends BaseDTO implements Serializable {

	/**
	 * 应用编号
	 */
    private Long id;

	/**
	 * 应用名称
	 */
	private String name;

	/**
	 * 端口
	 */
	private Integer port;

	/**
	 * 上传目录
	 */
	private String uploadPath;

	/**
	 * 部署目录
	 */
	private String deployPath;

	/**
	 * 备份目录
	 */
	private String backupPath;

	/**
	 * 启动脚本
	 */
	private String startScript;

	/**
	 * 部署脚本
	 */
	private String deployScript;

}
