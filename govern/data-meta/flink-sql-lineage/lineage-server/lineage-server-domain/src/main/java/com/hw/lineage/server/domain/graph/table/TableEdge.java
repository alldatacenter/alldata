package com.hw.lineage.server.domain.graph.table;

import com.hw.lineage.server.domain.graph.basic.Edge;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @description: TableEdge
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TableEdge extends Edge<TableNode> {

    private String sqlSource;

    public TableEdge(Integer edgeId,TableNode source, TableNode target, String sqlSource) {
        super(edgeId,source, target);
        this.sqlSource = sqlSource;
    }
}
