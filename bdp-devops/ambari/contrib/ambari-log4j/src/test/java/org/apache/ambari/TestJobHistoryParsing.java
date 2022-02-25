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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apache.ambari.eventdb.model.WorkflowContext;
import org.apache.ambari.eventdb.model.WorkflowDag.WorkflowDagEntry;
import org.apache.ambari.log4j.hadoop.mapreduce.jobhistory.MapReduceJobHistoryUpdater;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobHistory;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.tools.rumen.JobSubmittedEvent;
import org.apache.hadoop.util.StringUtils;

/**
 * 
 */
public class TestJobHistoryParsing extends TestCase {
  static final char LINE_DELIMITER_CHAR = '.';
  static final char[] charsToEscape = new char[] {'"', '=', LINE_DELIMITER_CHAR};
  private static final char DELIMITER = ' ';
  private static final String ID = "WORKFLOW_ID";
  private static final String NAME = "WORKFLOW_NAME";
  private static final String NODE = "WORKFLOW_NODE_NAME";
  private static final String ADJ = "WORKFLOW_ADJACENCIES";
  private static final String ID_PROP = "mapreduce.workflow.id";
  private static final String NAME_PROP = "mapreduce.workflow.name";
  private static final String NODE_PROP = "mapreduce.workflow.node.name";
  private static final String ADJ_PROP = "mapreduce.workflow.adjacency";
  
  public void test1() {
    Map<String,String[]> adj = new HashMap<String,String[]>();
    adj.put("10", new String[] {"20", "30"});
    adj.put("20", new String[] {"30"});
    adj.put("30", new String[] {});
    test("id_0-1", "something.name", "10", adj);
  }
  
  public void test2() {
    Map<String,String[]> adj = new HashMap<String,String[]>();
    adj.put("1=0", new String[] {"2 0", "3\"0."});
    adj.put("2 0", new String[] {"3\"0."});
    adj.put("3\"0.", new String[] {});
    test("id_= 0-1", "something.name", "1=0", adj);
  }
  
  public void test3() {
    String s = "`~!@#$%^&*()-_=+[]{}|,.<>/?;:'\"";
    test(s, s, s, new HashMap<String,String[]>());
  }
  
  public void test4() {
    Map<String,String[]> adj = new HashMap<String,String[]>();
    adj.put("X", new String[] {});
    test("", "jobName", "X", adj);
  }
  
  public void test(String workflowId, String workflowName, String workflowNodeName, Map<String,String[]> adjacencies) {
    Configuration conf = new Configuration();
    setProperties(conf, workflowId, workflowName, workflowNodeName, adjacencies);
    String log = log("JOB", new String[] {ID, NAME, NODE, ADJ},
        new String[] {conf.get(ID_PROP), conf.get(NAME_PROP), conf.get(NODE_PROP), JobHistory.JobInfo.getWorkflowAdjacencies(conf)});
    ParsedLine line = new ParsedLine(log);
    JobID jobid = new JobID("id", 1);
    JobSubmittedEvent event = new JobSubmittedEvent(jobid, workflowName, "", 0l, "", null, "", line.get(ID), line.get(NAME), line.get(NODE), line.get(ADJ));
    WorkflowContext context = MapReduceJobHistoryUpdater.buildWorkflowContext(event);
    
    String resultingWorkflowId = workflowId;
    if (workflowId.isEmpty())
      resultingWorkflowId = jobid.toString().replace("job_", "mr_");
    assertEquals("Didn't recover workflowId", resultingWorkflowId, context.getWorkflowId());
    assertEquals("Didn't recover workflowName", workflowName, context.getWorkflowName());
    assertEquals("Didn't recover workflowNodeName", workflowNodeName, context.getWorkflowEntityName());
    
    Map<String,String[]> resultingAdjacencies = adjacencies;
    if (resultingAdjacencies.size() == 0) {
      resultingAdjacencies = new HashMap<String,String[]>();
      resultingAdjacencies.put(workflowNodeName, new String[] {});
    }
    assertEquals("Got incorrect number of adjacencies", resultingAdjacencies.size(), context.getWorkflowDag().getEntries().size());
    for (WorkflowDagEntry entry : context.getWorkflowDag().getEntries()) {
      String[] sTargets = resultingAdjacencies.get(entry.getSource());
      assertNotNull("No original targets for " + entry.getSource(), sTargets);
      List<String> dTargets = entry.getTargets();
      assertEquals("Got incorrect number of targets for " + entry.getSource(), sTargets.length, dTargets.size());
      for (int i = 0; i < sTargets.length; i++) {
        assertEquals("Got incorrect target for " + entry.getSource(), sTargets[i], dTargets.get(i));
      }
    }
  }
  
  private static void setProperties(Configuration conf, String workflowId, String workflowName, String workflowNodeName, Map<String,String[]> adj) {
    conf.set(ID_PROP, workflowId);
    conf.set(NAME_PROP, workflowName);
    conf.set(NODE_PROP, workflowNodeName);
    for (Entry<String,String[]> entry : adj.entrySet()) {
      conf.setStrings(ADJ_PROP + "." + entry.getKey(), entry.getValue());
    }
  }
  
  private static String log(String recordType, String[] keys, String[] values) {
    int length = recordType.length() + keys.length * 4 + 2;
    for (int i = 0; i < keys.length; i++) {
      values[i] = StringUtils.escapeString(values[i], StringUtils.ESCAPE_CHAR, charsToEscape);
      length += values[i].length() + keys[i].toString().length();
    }
    
    // We have the length of the buffer, now construct it.
    StringBuilder builder = new StringBuilder(length);
    builder.append(recordType);
    builder.append(DELIMITER);
    for (int i = 0; i < keys.length; i++) {
      builder.append(keys[i]);
      builder.append("=\"");
      builder.append(values[i]);
      builder.append("\"");
      builder.append(DELIMITER);
    }
    builder.append(LINE_DELIMITER_CHAR);
    
    return builder.toString();
  }
  
  private static class ParsedLine {
    static final String KEY = "(\\w+)";
    static final String VALUE = "([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)";
    static final Pattern keyValPair = Pattern.compile(KEY + "=" + "\"" + VALUE + "\"");
    Map<String,String> props = new HashMap<String,String>();
    private String type;
    
    ParsedLine(String fullLine) {
      int firstSpace = fullLine.indexOf(" ");
      
      if (firstSpace < 0) {
        firstSpace = fullLine.length();
      }
      
      if (firstSpace == 0) {
        return; // This is a junk line of some sort
      }
      
      type = fullLine.substring(0, firstSpace);
      
      String propValPairs = fullLine.substring(firstSpace + 1);
      
      Matcher matcher = keyValPair.matcher(propValPairs);
      
      while (matcher.find()) {
        String key = matcher.group(1);
        String value = matcher.group(2);
        props.put(key, value);
      }
    }
    
    protected String getType() {
      return type;
    }
    
    protected String get(String key) {
      return props.get(key);
    }
  }
}
