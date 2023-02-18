package com.datasophon.api.test.dag;

import com.datasophon.common.model.DAGGraph;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DagTest {

    @Test
    public void testDagGraph(){
        DAGGraph<String, String, String> dag = new DAGGraph<>();

        Map<String,String> activeTaskNode = new ConcurrentHashMap<>();
        dag.addNode("a","1");
        dag.addNode("b","2");
        dag.addNode("c","3");
        dag.addNode("d","4");
        dag.addNode("e","4");
        dag.addEdge("a","b",false);
        dag.addEdge("a","c",false);

        ergodic(dag, activeTaskNode);

//        DAGGraph<String, String, String> reverseDagGraph = dag.getReverseDagGraph(dag);
//        ergodic(reverseDagGraph,activeTaskNode);

    }

    private void ergodic(DAGGraph<String, String, String> dag, Map<String, String> activeTaskNode) {
        Collection<String> beginNode1 = dag.getBeginNode();
        for (String node : beginNode1) {
            activeTaskNode.put(node,"");

        }
        System.out.println("beginNode is "+ beginNode1);
        int i = 0;
        for (String node : activeTaskNode.keySet()) {
            i++;
            System.out.println(node);
            Set<String> subsequentNodes = dag.getSubsequentNodes(node);
            for (String subsequentNode : subsequentNodes) {
                System.out.println(subsequentNode);
                activeTaskNode.put(subsequentNode,"");
            }
        }
    }

}
