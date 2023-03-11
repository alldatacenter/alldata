package com.hw.lineage.server.domain.query;

import lombok.Data;

/**
 * @description: PageOrderCriteria
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class PageOrderCriteria extends OrderCriteria {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
