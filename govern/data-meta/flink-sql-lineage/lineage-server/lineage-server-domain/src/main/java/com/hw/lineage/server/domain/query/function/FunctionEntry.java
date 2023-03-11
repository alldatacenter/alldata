package com.hw.lineage.server.domain.query.function;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: FunctionEntry
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@NoArgsConstructor
public class FunctionEntry {

    private String pluginCode;

    private String catalogName;

    private String database;

    private Long functionId;

    private String functionName;
}
