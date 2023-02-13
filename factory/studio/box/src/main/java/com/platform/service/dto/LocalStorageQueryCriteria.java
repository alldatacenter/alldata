
package com.platform.service.dto;

import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

import com.platform.annotation.Query;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Data
public class LocalStorageQueryCriteria{

    @Query(blurry = "name,suffix,type,createBy,size")
    private String blurry;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> createTime;
}