package com.hw.lineage.server.application.dto;

import com.hw.lineage.server.application.dto.basic.BasicDTO;
import lombok.Data;

/**
 * @description: FunctionDTO
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class FunctionDTO extends BasicDTO {

    private Long functionId;

    private Long catalogId;

    private String functionName;

    private String database;

    private String invocation;

    private String functionPath;

    private String className;

    private String descr;
}
