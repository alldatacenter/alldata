package com.hw.lineage.server.domain.graph.basic;

import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @description: Graph
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class Graph<N extends Node, E extends Edge<N>> {

    private Map<String, N> nodeMap;

    private Set<E> edgeSet;

    public Graph() {
        this.nodeMap = new HashMap<>();
        this.edgeSet = new HashSet<>();
    }

    public Set<N> queryNodeSet() {
        return new HashSet<>(nodeMap.values());
    }

    public N queryNode(String nodeName) {
        return nodeMap.get(nodeName);
    }

    public void addNode(String nodeName, N node) {
        nodeMap.put(nodeName, node);
    }

    public void addEdge(E edge) {
        edgeSet.add(edge);
    }
}

