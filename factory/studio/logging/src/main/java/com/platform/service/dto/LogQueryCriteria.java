
package com.platform.service.dto;

import lombok.Data;
import com.platform.annotation.Query;
import java.sql.Timestamp;
import java.util.List;

/**
 * 日志查询类
 * @author AllDataDC
 * @date 2023-01-27 09:23:07
 */
@Data
public class LogQueryCriteria {

    @Query(blurry = "username,description,address,requestIp,method,params")
    private String blurry;

    @Query
    private String username;

    @Query
    private String logType;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> createTime;
}
