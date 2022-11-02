package com.alibaba.tesla.tkgone.server.domain;

import org.apache.commons.collections.CollectionUtils;

import java.util.*;

/**
 * DAG图
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/05/18 17:41
 */
public class DirectedAcyclicGraph<T> {

    private List<NodeChain> nodeChains;
    private Set<T> nodes;

    public DirectedAcyclicGraph() {
        nodeChains = new ArrayList<>();
        nodes = new HashSet<>();
    }

    public void addGraphNodeChain(T from, T to) throws RuntimeException {
        if(Objects.isNull(to)) {
            throw new RuntimeException("topology node chain illegal, to node is null");
        }

        NodeChain nodeChain = new NodeChain(from, to);
        nodeChains.add(nodeChain);
        if (Objects.nonNull(from)) {
            nodes.add(from);
        }
        nodes.add(to);
    }

    public List<T> sort() {
        List<T> sortedNode = new ArrayList<>();

        if (CollectionUtils.isEmpty(nodes)) {
            return sortedNode;
        }

        Map<T, Integer> inDegrees = new HashMap<>();
        nodeChains.forEach(nodeChain -> {
            int toInDegree = 0;
            if (Objects.nonNull(nodeChain.from)) {
                toInDegree = inDegrees.getOrDefault(nodeChain.to, 0) + 1;
                inDegrees.putIfAbsent(nodeChain.from, 0);
            }
            inDegrees.put(nodeChain.to, toInDegree);
        });

        Queue<T> queue = new ArrayDeque<>();
        for (T node : inDegrees.keySet()){
            if (inDegrees.get(node) == 0) {
                queue.offer(node);
            }
        }

        while (!queue.isEmpty()) {
            T node = queue.poll();
            sortedNode.add(node);

            nodeChains.forEach(nodeChain -> {
                if (Objects.nonNull(nodeChain.from) && nodeChain.from.equals(node)) {
                    int curInDegree = inDegrees.get(nodeChain.to) - 1;
                    inDegrees.put(nodeChain.to, curInDegree);
                    if (curInDegree == 0) {
                        queue.offer(nodeChain.to);
                    }
                }
            });
        }

        return sortedNode.size() == nodes.size() ? sortedNode : new ArrayList<>();
    }

    class NodeChain {
        /**
         * 入节点
         */
        private T from;

        /**
         * 出节点
         */
        private  T to;

        NodeChain(T from, T to) {
            this.from = from;
            this.to = to;
        }
    }

}
