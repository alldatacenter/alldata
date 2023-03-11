package com.hw.lineage.server.domain.graph.table;

import com.hw.lineage.server.domain.graph.basic.Node;
import com.hw.lineage.server.domain.graph.column.ColumnNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: TableNode
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TableNode extends Node {

    private List<ColumnNode> columnNodeList;

    public TableNode(Integer nodeId, String nodeName) {
        super(nodeId, nodeName);
        this.columnNodeList = new ArrayList<>();
    }

    public void addColumnNode(ColumnNode columnNode) {
        columnNodeList.add(columnNode);
    }
}
