package com.hw.lineage.server.domain.graph;

import com.google.common.collect.ImmutableSet;
import com.hw.lineage.server.domain.graph.basic.Edge;
import com.hw.lineage.server.domain.graph.basic.Graph;
import com.hw.lineage.server.domain.graph.basic.Node;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @description: GraphHelperTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class GraphHelperTest {

    /**
     * <pre>
     *    1 ------------> 2
     *                 ->  -
     *                -        -
     *              -             ->
     *            -                  5 ------> 6
     *          -                 ->
     *        -                -
     *      -              -
     *    3 -----------> 4 --------> 8
     *      -                      ->
     *        -                   -
     *          -               -
     *            -           -
     *              -      -
     *                -> 7
     *
     * </pre>
     */
    @Test
    public void testComputeChildrenCnt() {
        Graph<Node, Edge<Node>> graph = new Graph<>();

        Node node1 = new Node(1, "node1");
        Node node2 = new Node(2, "node2");
        Node node3 = new Node(3, "node3");
        Node node4 = new Node(4, "node4");
        Node node5 = new Node(5, "node5");
        Node node6 = new Node(6, "node6");
        Node node7 = new Node(7, "node7");
        Node node8 = new Node(8, "node8");

        graph.addNode(node1.getNodeName(), node1);
        graph.addNode(node2.getNodeName(), node2);
        graph.addNode(node3.getNodeName(), node3);
        graph.addNode(node4.getNodeName(), node4);
        graph.addNode(node5.getNodeName(), node5);
        graph.addNode(node6.getNodeName(), node6);
        graph.addNode(node7.getNodeName(), node7);
        graph.addNode(node8.getNodeName(), node8);

        Edge<Node> edge1 = new Edge<>(8, node1, node2);
        Edge<Node> edge2 = new Edge<>(9, node2, node5);
        Edge<Node> edge3 = new Edge<>(10, node5, node6);
        Edge<Node> edge4 = new Edge<>(11, node3, node2);
        Edge<Node> edge5 = new Edge<>(12, node3, node4);
        Edge<Node> edge6 = new Edge<>(13, node4, node5);
        Edge<Node> edge7 = new Edge<>(14, node4, node8);
        Edge<Node> edge8 = new Edge<>(15, node3, node7);
        Edge<Node> edge9 = new Edge<>(16, node7, node8);

        graph.addEdge(edge1);
        graph.addEdge(edge2);
        graph.addEdge(edge3);
        graph.addEdge(edge4);
        graph.addEdge(edge5);
        graph.addEdge(edge6);
        graph.addEdge(edge7);
        graph.addEdge(edge8);
        graph.addEdge(edge9);

        GraphHelper<Node, Edge<Node>> graphHelper = new GraphHelper<>(graph);
        graphHelper.computeChildrenCnt();

        Map<Integer, Set<Integer>> childrenMap = graphHelper.getChildrenMap();

        assertEquals(ImmutableSet.of(2, 5, 6), childrenMap.get(1));
        assertEquals(ImmutableSet.of(5, 6), childrenMap.get(2));
        assertEquals(ImmutableSet.of(2, 4, 5, 6, 7, 8), childrenMap.get(3));
        assertEquals(ImmutableSet.of(5, 6, 8), childrenMap.get(4));
        assertEquals(ImmutableSet.of(6), childrenMap.get(5));
        assertEquals(ImmutableSet.of(), childrenMap.get(6));
        assertEquals(ImmutableSet.of(8), childrenMap.get(7));
        assertEquals(ImmutableSet.of(), childrenMap.get(8));

        assertThat(node1.getChildrenCnt()).isEqualTo(3);
        assertThat(node2.getChildrenCnt()).isEqualTo(2);
        assertThat(node3.getChildrenCnt()).isEqualTo(6);
        assertThat(node4.getChildrenCnt()).isEqualTo(3);
        assertThat(node5.getChildrenCnt()).isEqualTo(1);
        assertThat(node6.getChildrenCnt()).isZero();
        assertThat(node7.getChildrenCnt()).isEqualTo(1);
        assertThat(node8.getChildrenCnt()).isZero();
    }
}