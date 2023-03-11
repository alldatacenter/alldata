package com.hw.lineage.server.application.dto.graph.vertex;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @description: Column
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@AllArgsConstructor
public class Column {

    private Integer id;

    private String name;

    private Integer childrenCnt;
}
