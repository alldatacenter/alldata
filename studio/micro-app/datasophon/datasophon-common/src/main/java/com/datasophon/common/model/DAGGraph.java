package com.datasophon.common.model;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DAGGraph<Node,NodeInfo,EdgeInfo> {

    private static final Logger logger = LoggerFactory.getLogger(DAGGraph.class);

    private Map<Node, NodeInfo> nodesMap;

    private Map<Node, Map<Node, EdgeInfo>> edgesMap;

    private Map<Node, Map<Node, EdgeInfo>> reverseEdgesMap;

    public DAGGraph() {
        //初始化邻接表
        nodesMap = new HashMap<>();
        edgesMap = new HashMap<>();
        reverseEdgesMap = new HashMap<>();
    }

    public Map<Node, NodeInfo> getNodesMap() {
        return nodesMap;
    }

    public void setNodesMap(Map<Node, NodeInfo> nodesMap) {
        this.nodesMap = nodesMap;
    }

    public Map<Node, Map<Node, EdgeInfo>> getEdgesMap() {
        return edgesMap;
    }

    public void setEdgesMap(Map<Node, Map<Node, EdgeInfo>> edgesMap) {
        this.edgesMap = edgesMap;
    }

    public Map<Node, Map<Node, EdgeInfo>> getReverseEdgesMap() {
        return reverseEdgesMap;
    }

    public void setReverseEdgesMap(Map<Node, Map<Node, EdgeInfo>> reverseEdgesMap) {
        this.reverseEdgesMap = reverseEdgesMap;
    }

    public void addNode(Node node, NodeInfo nodeInfo) {
        nodesMap.put(node, nodeInfo);
    }

    public boolean addEdge(Node fromNode, Node toNode,boolean createNode) {
        //由于有向图中边是有向的，v->w 边
        if (!isLegalAddEdge(fromNode, toNode, createNode)) {
            logger.error("serious error: add edge({} -> {}) is invalid, cause cycle！", fromNode, toNode);
            return false;
        }
        addNodeIfAbsent(fromNode, null);
        addNodeIfAbsent(toNode, null);

        addEdge(fromNode, toNode,  edgesMap);
        addEdge(toNode, fromNode,  reverseEdgesMap);

        return true;

    }

    private void addEdge(Node fromNode, Node toNode,  Map<Node, Map<Node, EdgeInfo>> edges) {
        edges.putIfAbsent(fromNode, new HashMap<>());
        Map<Node, EdgeInfo> toNodeEdges = edges.get(fromNode);
        toNodeEdges.put(toNode, null);
    }

    private void addNodeIfAbsent(Node node, NodeInfo nodeInfo) {
        if (!containsNode(node)) {
            addNode(node, nodeInfo);
        }
    }

    public boolean containsNode(Node node) {
        return nodesMap.containsKey(node);
    }

    /**
     * get the start node of DAG
     *
     * @return the start node of DAG
     */
    public Collection<Node> getBeginNode() {
        return CollectionUtils.subtract(nodesMap.keySet(), reverseEdgesMap.keySet());
    }

    private boolean isLegalAddEdge(Node fromNode, Node toNode, boolean createNode) {
        if (fromNode.equals(toNode)) {
            logger.error("edge fromNode({}) can't equals toNode({})", fromNode, toNode);
            return false;
        }

        if (!createNode) {
            if (!containsNode(fromNode) || !containsNode(toNode)){
                logger.error("edge fromNode({}) or toNode({}) is not in vertices map", fromNode, toNode);
                return false;
            }
        }

        // Whether an edge can be successfully added(fromNode -> toNode),need to determine whether the DAG has cycle!
        int verticesCount = getNodesCount();

        Queue<Node> queue = new LinkedList<>();

        queue.add(toNode);

        // if DAG doesn't find fromNode, it's not has cycle!
        while (!queue.isEmpty() && (--verticesCount > 0)) {
            Node key = queue.poll();

            for (Node subsequentNode : getSubsequentNodes(key)) {
                if (subsequentNode.equals(fromNode)) {
                    return false;
                }

                queue.add(subsequentNode);
            }
        }

        return true;
    }

    public int getNodesCount() {
        return nodesMap.size();
    }

    public Set<Node> getSubsequentNodes(Node node) {
        return getNeighborNodes(node, edgesMap);
    }

    public Set<Node> getPreviousNodes(Node node) {
        return getNeighborNodes(node, reverseEdgesMap);
    }

    private Set<Node> getNeighborNodes(Node node, final Map<Node, Map<Node, EdgeInfo>> edges) {
        final Map<Node, EdgeInfo> neighborEdges = edges.get(node);

        if (neighborEdges == null) {
            return Collections.EMPTY_MAP.keySet();
        }

        return neighborEdges.keySet();
    }

    public NodeInfo getNode(Node node) {
        return nodesMap.get(node);
    }

    public DAGGraph<Node,NodeInfo,EdgeInfo> getReverseDagGraph(DAGGraph<Node,NodeInfo,EdgeInfo> dagGraph){
        DAGGraph<Node, NodeInfo, EdgeInfo> reverseDagGraph = new DAGGraph<>();
        reverseDagGraph.setNodesMap(dagGraph.getNodesMap());
        reverseDagGraph.setEdgesMap(dagGraph.getReverseEdgesMap());
        reverseDagGraph.setReverseEdgesMap(dagGraph.getEdgesMap());
        return reverseDagGraph;
    }

    public static void main(String[] args) {
        DAGGraph<String, String, String> dag = new DAGGraph<>();

        Map<String,String> activeTaskNode = new ConcurrentHashMap<>();
        dag.addNode("a","1");
        dag.addNode("b","2");
        dag.addNode("c","3");
        dag.addNode("d","4");
        dag.addEdge("a","b",false);
        dag.addEdge("a","c",false);

        Collection<String> beginNode1 = dag.getBeginNode();
        for (String node : beginNode1) {
            activeTaskNode.put(node,"");

        }
        for (String node : activeTaskNode.keySet()) {
            Set<String> subsequentNodes = dag.getSubsequentNodes(node);
            for (String subsequentNode : subsequentNodes) {
                System.out.println(subsequentNode);
                activeTaskNode.put(subsequentNode,"");
            }
            activeTaskNode.remove(node);
        }
        System.out.println(beginNode1);

    }


}
