/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari;

import java.util.List;

import junit.framework.TestCase;

import org.apache.ambari.eventdb.model.WorkflowContext;
import org.apache.ambari.eventdb.model.WorkflowDag;
import org.apache.ambari.eventdb.model.WorkflowDag.WorkflowDagEntry;
import org.apache.ambari.log4j.hadoop.mapreduce.jobhistory.MapReduceJobHistoryUpdater;

/**
 * 
 */
public class TestMapReduceJobHistoryUpdater extends TestCase {
  public void testDagMerging() {
    WorkflowDag dag1 = new WorkflowDag();
    dag1.addEntry(getEntry("a", "b", "c"));
    dag1.addEntry(getEntry("b", "d"));
    WorkflowContext one = new WorkflowContext();
    one.setWorkflowDag(dag1);
    
    WorkflowDag dag2 = new WorkflowDag();
    dag2.addEntry(getEntry("a", "d"));
    dag2.addEntry(getEntry("c", "e"));
    WorkflowContext two = new WorkflowContext();
    two.setWorkflowDag(dag2);
    
    WorkflowDag emptyDag = new WorkflowDag();
    WorkflowContext three = new WorkflowContext();
    three.setWorkflowDag(emptyDag);
    
    WorkflowDag mergedDag = new WorkflowDag();
    mergedDag.addEntry(getEntry("a", "b", "c", "d"));
    mergedDag.addEntry(getEntry("b", "d"));
    mergedDag.addEntry(getEntry("c", "e"));
    
    assertEquals(mergedDag, MapReduceJobHistoryUpdater.constructMergedDag(one, two));
    assertEquals(mergedDag, MapReduceJobHistoryUpdater.constructMergedDag(two, one));
    
    // test blank dag
    assertEquals(dag1, MapReduceJobHistoryUpdater.constructMergedDag(three, one));
    assertEquals(dag1, MapReduceJobHistoryUpdater.constructMergedDag(one, three));
    assertEquals(dag2, MapReduceJobHistoryUpdater.constructMergedDag(three, two));
    assertEquals(dag2, MapReduceJobHistoryUpdater.constructMergedDag(two, three));
    
    // test null dag
    assertEquals(dag1, MapReduceJobHistoryUpdater.constructMergedDag(new WorkflowContext(), one));
    assertEquals(dag1, MapReduceJobHistoryUpdater.constructMergedDag(one, new WorkflowContext()));
    assertEquals(dag2, MapReduceJobHistoryUpdater.constructMergedDag(new WorkflowContext(), two));
    assertEquals(dag2, MapReduceJobHistoryUpdater.constructMergedDag(two, new WorkflowContext()));
    
    // test same dag
    assertEquals(dag1, MapReduceJobHistoryUpdater.constructMergedDag(one, one));
    assertEquals(dag2, MapReduceJobHistoryUpdater.constructMergedDag(two, two));
    assertEquals(emptyDag, MapReduceJobHistoryUpdater.constructMergedDag(three, three));
  }
  
  private static WorkflowDagEntry getEntry(String source, String... targets) {
    WorkflowDagEntry entry = new WorkflowDagEntry();
    entry.setSource(source);
    for (String target : targets) {
      entry.addTarget(target);
    }
    return entry;
  }
  
  private static void assertEquals(WorkflowDag dag1, WorkflowDag dag2) {
    assertEquals(dag1.size(), dag2.size());
    List<WorkflowDagEntry> entries1 = dag1.getEntries();
    List<WorkflowDagEntry> entries2 = dag2.getEntries();
    assertEquals(entries1.size(), entries2.size());
    for (int i = 0; i < entries1.size(); i++) {
      WorkflowDagEntry e1 = entries1.get(i);
      WorkflowDagEntry e2 = entries2.get(i);
      assertEquals(e1.getSource(), e2.getSource());
      List<String> t1 = e1.getTargets();
      List<String> t2 = e2.getTargets();
      assertEquals(t1.size(), t2.size());
      for (int j = 0; j < t1.size(); j++) {
        assertEquals(t1.get(j), t2.get(j));
      }
    }
  }
}
