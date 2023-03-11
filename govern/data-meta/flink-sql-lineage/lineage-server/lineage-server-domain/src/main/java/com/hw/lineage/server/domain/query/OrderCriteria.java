package com.hw.lineage.server.domain.query;

import lombok.Data;

/**
 * @description: OrderCriteria
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class OrderCriteria {

    /**
     * sort field, default createTime
     */
    private String sortColumn = "createTime";

    private boolean descending = false;
}
