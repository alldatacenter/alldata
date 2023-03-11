package com.hw.lineage.server.domain.graph.column;

import com.hw.lineage.server.domain.graph.basic.Edge;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @description: ColumnEdge
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ColumnEdge extends Edge<ColumnNode> {

    private String transform;


    public ColumnEdge(Integer edgeId,ColumnNode source, ColumnNode target, String transform) {
        super(edgeId,source, target);
        this.transform = transform;
    }
}
