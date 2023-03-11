package com.hw.lineage.server.domain.graph.column;


import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @description: ColumnEdgeTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class ColumnEdgeTest {

    /**
     * Add @EqualsAndHashCode(callSuper = true) to ColumnEdge and ColumnNode, otherwise the result is true
     */
    @Test
    public void testColumnEdgeEquals() {
        ColumnNode source1 = new ColumnNode(1, "id", 100);
        ColumnNode target1 = new ColumnNode(2, "id", 102);
        ColumnEdge edge1 = new ColumnEdge(1, source1, target1, null);

        ColumnNode source2 = new ColumnNode(3, "name", 100);
        ColumnNode target2 = new ColumnNode(4, "name", 102);
        ColumnEdge edge2 = new ColumnEdge(2, source2, target2, null);

        assertThat(edge1.equals(edge2)).isFalse();
    }

}