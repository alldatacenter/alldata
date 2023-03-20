
package com.platform.modules.mnt.service.dto;

import lombok.Data;
import com.platform.annotation.Query;
import java.sql.Timestamp;
import java.util.List;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Data
public class DeployHistoryQueryCriteria{

	/**
	 * 精确
	 */
	@Query(blurry = "appName,ip,deployUser")
	private String blurry;

	@Query
	private Long deployId;

	@Query(type = Query.Type.BETWEEN)
	private List<Timestamp> deployDate;
}
