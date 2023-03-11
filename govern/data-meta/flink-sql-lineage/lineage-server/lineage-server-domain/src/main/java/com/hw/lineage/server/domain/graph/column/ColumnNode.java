package com.hw.lineage.server.domain.graph.column;

import com.hw.lineage.server.domain.graph.basic.Node;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @description: ColumnNode
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ColumnNode extends Node {

    private Integer tableNodeId;

    public ColumnNode(Integer nodeId, String nodeName, Integer tableNodeId) {
        super(nodeId, nodeName);
        this.tableNodeId = tableNodeId;
    }
}
