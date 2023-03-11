package com.hw.lineage.server.domain.query.function;

import com.hw.lineage.server.domain.query.PageOrderCriteria;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @description: FunctionQuery
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class FunctionQuery extends PageOrderCriteria {
    @ApiModelProperty(hidden = true)
    private Long catalogId;

    @ApiModelProperty(hidden = true)
    private String database;

    private String functionName;
}