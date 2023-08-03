/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.optimizing;

import java.util.HashMap;
import java.util.Map;

public class OptimizingTaskSummary {

  public static final String DATA_FILE_CNT = "data_file_cnt";

  public static final String DATA_FILE_TOTAL_SIZE = "data_file_total_size";

  public static final String EQ_DELETE_FILE_CNT = "eq_delete_file_cnt";

  public static final String EQ_DELETE_FILE_TOTAL_SIZE = "eq_delete_file_total_size";

  public static final String POS_DELETE_FILE_CNT = "pos_delete_file_cnt";

  public static final String POS_DELETE_FILE_TOTAL_SIZE = "pos_delete_file_total_size";

  public static final String EXECUTE_DURATION = "executor_duration";

  private Map<String, String> summary = new HashMap<>();

  public void setDataFileCnt(int dataFileCnt) {
    summary.put(DATA_FILE_CNT, String.valueOf(dataFileCnt));
  }

  public void setDataFileTotalSize(long dataFileTotalSize) {
    summary.put(DATA_FILE_TOTAL_SIZE, String.valueOf(dataFileTotalSize));
  }

  public void setEqDeleteFileCnt(int eqDeleteFileCnt) {
    summary.put(EQ_DELETE_FILE_CNT, String.valueOf(eqDeleteFileCnt));
  }

  public void setEqDeleteFileTotalSize(long eqDeleteFileTotalSize) {
    summary.put(EQ_DELETE_FILE_TOTAL_SIZE, String.valueOf(eqDeleteFileTotalSize));
  }

  public void setPosDeleteFileCnt(int posDeleteFileCnt) {
    summary.put(POS_DELETE_FILE_CNT, String.valueOf(posDeleteFileCnt));
  }

  public void setPosDeleteFileTotalSize(long posDeleteFileTotalSize) {
    summary.put(POS_DELETE_FILE_TOTAL_SIZE, String.valueOf(posDeleteFileTotalSize));
  }

  public void setExecuteDuration(long executeDuration) {
    summary.put(EXECUTE_DURATION, String.valueOf(executeDuration));
  }

  public Map<String, String> getSummary() {
    return summary;
  }
}
