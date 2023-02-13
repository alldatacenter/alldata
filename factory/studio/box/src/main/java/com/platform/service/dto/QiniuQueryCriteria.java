
package com.platform.service.dto;

import lombok.Data;
import com.platform.annotation.Query;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author AllDataDC
 * @date 2023-01-27 09:54:37
 */
@Data
public class QiniuQueryCriteria{

    @Query(type = Query.Type.INNER_LIKE)
    private String key;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> createTime;
}
