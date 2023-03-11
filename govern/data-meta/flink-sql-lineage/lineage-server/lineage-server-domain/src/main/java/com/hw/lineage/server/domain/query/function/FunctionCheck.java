package com.hw.lineage.server.domain.query.function;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description: FunctionCheck
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class FunctionCheck {

    @ApiModelProperty(hidden = true)
    private Long catalogId;

    @ApiModelProperty(hidden = true)
    private String database;

    @NotNull
    private String functionName;
}
