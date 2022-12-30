/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.mongodb.source.split;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TaskGroupInfo implements Serializable {
  private List<List<Range>> taskGroupInfo;

  public TaskGroupInfo(int taskGroupNum) {
    this.taskGroupInfo = new ArrayList<>(taskGroupNum);
    for (int i = 0; i < taskGroupNum; i++) {
      List<Range> oneGroup = new ArrayList<>();
      taskGroupInfo.add(oneGroup);
    }
  }

  public List<Range> get(int i) {
    return taskGroupInfo.get(i);
  }

  public void set(int i, List<Range> rangeList) {
    taskGroupInfo.set(i, rangeList);
  }

  public int size() {
    return taskGroupInfo.size();
  }
}
