package com.hw.lineage.server.application.command.catalog;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @description: CreateTableCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class CreateTableCmd {

    @ApiModelProperty(hidden = true)
    private Long catalogId;

    @ApiModelProperty(hidden = true)
    private String database;

    @NotBlank
    private String createSql;
}
