
package com.platform.modules.mnt.service.dto;

import lombok.Data;
import com.platform.annotation.Query;
import java.sql.Timestamp;
import java.util.List;

/**
* @author AllDataDC
* @date 2022-10-27
*/
@Data
public class DatabaseQueryCriteria{

	/**
	 * 模糊
	 */
    @Query(type = Query.Type.INNER_LIKE)
    private String name;

	/**
	 * 精确
	 */
    @Query
    private String jdbcUrl;

	@Query(type = Query.Type.BETWEEN)
	private List<Timestamp> createTime;
}
