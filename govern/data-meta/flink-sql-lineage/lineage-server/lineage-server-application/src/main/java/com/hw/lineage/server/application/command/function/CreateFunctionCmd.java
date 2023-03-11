package com.hw.lineage.server.application.command.function;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @description: CreateFunctionCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class CreateFunctionCmd {
    @ApiModelProperty(hidden = true)
    private Long catalogId;

    @ApiModelProperty(hidden = true)
    private String database;

    @NotBlank
    private String functionName;

    private String invocation;

    @NotBlank
    private String functionPath;

    @NotBlank
    private String className;

    private String descr;
}
