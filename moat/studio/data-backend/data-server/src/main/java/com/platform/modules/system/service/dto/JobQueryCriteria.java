
package com.platform.modules.system.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.platform.annotation.Query;
import java.sql.Timestamp;
import java.util.List;

/**
* @author AllDataDC
* @date 2023-01-27 14:49:34
*/
@Data
@NoArgsConstructor
public class JobQueryCriteria {

    @Query(type = Query.Type.INNER_LIKE)
    private String name;

    @Query
    private Boolean enabled;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> createTime;
}