package com.hw.lineage.server.domain.graph.basic;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * @description: node.Node
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@NoArgsConstructor
public class Node {

    private Integer nodeId;

    private String nodeName;

    private Set<Integer> parentIdSet;

    private Set<Integer> childIdSet;

    private Integer childrenCnt;

    public Node(Integer nodeId, String nodeName) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;

        this.parentIdSet = new HashSet<>();
        this.childIdSet = new HashSet<>();
        this.childrenCnt = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        return nodeName.equals(node.nodeName);
    }

    @Override
    public int hashCode() {
        return nodeName.hashCode();
    }
}
