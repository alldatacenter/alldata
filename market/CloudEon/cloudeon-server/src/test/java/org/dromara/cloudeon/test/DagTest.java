package org.dromara.cloudeon.test;

import org.dromara.cloudeon.utils.DAG;
import org.junit.jupiter.api.Test;

public class DagTest {
    @Test
    public void test() throws Exception {
        DAG<String, Object, Object> dag = new DAG<>();
        dag.addNode("HDFS",null);
        dag.addNode("YARN",null);
        dag.addNode("ZOOKEEPER",null);


        dag.addEdge("ZOOKEEPER", "HDFS");
        dag.addEdge("ZOOKEEPER", "YARN");
        dag.addEdge("HDFS", "YARN");

        dag.topologicalSort().stream().forEach(System.out::println);
    }
}
