package com.hw.lineage.server.domain.graph.basic;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: Edge
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@NoArgsConstructor
public class Edge<N extends Node> {

    private Integer edgeId;

    private N source;

    private N target;

    public Edge(Integer edgeId, N source, N target) {
        this.edgeId = edgeId;
        this.source = source;
        this.target = target;

        this.source.getChildIdSet().add(target.getNodeId());
        this.target.getParentIdSet().add(source.getNodeId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Edge edge = (Edge) o;

        if (!source.equals(edge.source)) return false;
        return target.equals(edge.target);
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + target.hashCode();
        return result;
    }
}
