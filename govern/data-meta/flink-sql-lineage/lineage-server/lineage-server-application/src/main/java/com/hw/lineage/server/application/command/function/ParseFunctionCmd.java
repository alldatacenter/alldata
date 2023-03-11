package com.hw.lineage.server.application.command.function;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @description: ParseFunctionCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class ParseFunctionCmd {

    @NotNull
    private Long pluginId;
    @NotBlank
    private String functionPath;

}
