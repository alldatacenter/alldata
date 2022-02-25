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

package org.apache.ambari.eventdb.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowDag {
  
  public static class WorkflowDagEntry {
    
    private String source;
    private List<String> targets = new ArrayList<String>();
    
    public WorkflowDagEntry() {
      /* Required by JAXB. */
    }
    
    /* Getters. */
    public String getSource() {
      return this.source;
    }
    
    public List<String> getTargets() {
      return this.targets;
    }
    
    /* Setters. */
    public void setSource(String source) {
      this.source = source;
    }
    
    public void setTargets(List<String> targets) {
      this.targets = targets;
    }
    
    public void addTarget(String target) {
      this.targets.add(target);
    }
  }
  
  List<WorkflowDagEntry> entries = new ArrayList<WorkflowDagEntry>();
  
  public WorkflowDag() {
    /* Required by JAXB. */
  }
  
  /* Getters. */
  public List<WorkflowDagEntry> getEntries() {
    return this.entries;
  }
  
  /* Setters. */
  public void setEntries(List<WorkflowDagEntry> entries) {
    this.entries = entries;
  }
  
  public void addEntry(WorkflowDag.WorkflowDagEntry entry) {
    this.entries.add(entry);
  }
  
  public int size() {
    Set<String> nodes = new HashSet<String>();
    for (WorkflowDagEntry entry : entries) {
      nodes.add(entry.getSource());
      nodes.addAll(entry.getTargets());
    }
    return nodes.size();
  }
}
