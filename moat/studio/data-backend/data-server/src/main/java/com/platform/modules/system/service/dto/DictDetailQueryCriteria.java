
package com.platform.modules.system.service.dto;

import lombok.Data;
import com.platform.annotation.Query;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Data
public class DictDetailQueryCriteria {

    @Query(type = Query.Type.INNER_LIKE)
    private String label;

    @Query(propName = "name",joinName = "dict")
    private String dictName;
}