
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
public class ServerDeployQueryCriteria{

	/**
	 * 模糊
	 */
	@Query(blurry = "name,ip,account")
    private String blurry;

	@Query(type = Query.Type.BETWEEN)
	private List<Timestamp> createTime;
}
