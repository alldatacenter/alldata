package com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl;

import com.hw.lineage.server.domain.graph.table.TableGraph;
import com.hw.lineage.server.infrastructure.persistence.mybatis.handler.JsonTypeHandler;

/**
 * @description: TableGraphTypeHandler
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class TableGraphTypeHandler extends JsonTypeHandler<TableGraph> {
    public TableGraphTypeHandler() {
        super(TableGraph.class);
    }
}
