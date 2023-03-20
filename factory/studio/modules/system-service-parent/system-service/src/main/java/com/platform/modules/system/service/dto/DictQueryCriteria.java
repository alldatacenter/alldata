
package com.platform.modules.system.service.dto;

import lombok.Data;
import com.platform.annotation.Query;

/**
 * @author AllDataDC
 * 公共查询类
 */
@Data
public class DictQueryCriteria {

    @Query(blurry = "name,description")
    private String blurry;
}
