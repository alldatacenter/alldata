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

package org.apache.hadoop.mapreduce.task.reduce;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapTaskCompletionEventsUpdate;
import org.apache.hadoop.mapred.TaskCompletionEvent;
import org.apache.hadoop.mapred.TaskUmbilicalProtocol;
import org.apache.hadoop.mapreduce.RssMRUtils;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.common.exception.RssException;

public class RssEventFetcher<K,V> {
  private static final Log LOG = LogFactory.getLog(RssEventFetcher.class);

  private final TaskAttemptID reduce;
  private final TaskUmbilicalProtocol umbilical;
  private int fromEventIdx = 0;
  private final int maxEventsToFetch;
  private JobConf jobConf;

  private List<TaskAttemptID> successMaps = new LinkedList<>();
  private List<TaskAttemptID> obsoleteMaps = new LinkedList<>();
  private int tipFailedCount = 0;
  private final int totalMapsCount;
  private final int appAttemptId;

  public RssEventFetcher(
      int appAttemptId,
      TaskAttemptID reduce,
      TaskUmbilicalProtocol umbilical,
      JobConf jobConf,
      int maxEventsToFetch) {
    this.jobConf = jobConf;
    this.totalMapsCount = jobConf.getNumMapTasks();
    this.reduce = reduce;
    this.umbilical = umbilical;
    this.maxEventsToFetch = maxEventsToFetch;
    this.appAttemptId = appAttemptId;
  }

  public Roaring64NavigableMap fetchAllRssTaskIds() {
    try {
      acceptMapCompletionEvents();
    } catch (Exception e) {
      throw new RssException("Reduce: " + reduce
          + " fails to accept completion events due to: "
          + e.getMessage());
    }

    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf();
    Roaring64NavigableMap mapIndexBitmap = Roaring64NavigableMap.bitmapOf();
    String errMsg = "TaskAttemptIDs are inconsistent with map tasks";
    for (TaskAttemptID taskAttemptID: successMaps) {
      if (!obsoleteMaps.contains(taskAttemptID)) {
        long rssTaskId = RssMRUtils.convertTaskAttemptIdToLong(taskAttemptID, appAttemptId);
        int mapIndex = taskAttemptID.getTaskID().getId();
        // There can be multiple successful attempts on same map task.
        // So we only need to accept one of them.
        if (!mapIndexBitmap.contains(mapIndex)) {
          taskIdBitmap.addLong(rssTaskId);
          if (mapIndex < totalMapsCount) {
            mapIndexBitmap.addLong(mapIndex);
          } else {
            LOG.error(taskAttemptID + " has overflowed mapIndex");
            throw new IllegalStateException(errMsg);
          }
        } else {
          LOG.warn(taskAttemptID + " is redundant on index: " + mapIndex);
        }
      } else {
        LOG.warn(taskAttemptID + " is successful but cancelled by obsolete event");
      }
    }
    // each map should have only one success attempt
    if (mapIndexBitmap.getLongCardinality() != taskIdBitmap.getLongCardinality()) {
      throw new IllegalStateException(errMsg);
    }
    if (tipFailedCount != 0) {
      LOG.warn("There are " + tipFailedCount + " tipFailed tasks");
    }
    if (taskIdBitmap.getLongCardinality() + tipFailedCount != totalMapsCount) {
      for (int index = 0; index < totalMapsCount; index++) {
        if (!mapIndexBitmap.contains(index)) {
          LOG.error("Fail to fetch " + " map task on index: " + index);
        }
      }
      throw new IllegalStateException(errMsg);
    }
    return taskIdBitmap;
  }

  public void resolve(TaskCompletionEvent event) {
    // Process the TaskCompletionEvents:
    // 1. Save the SUCCEEDED maps in knownOutputs to fetch the outputs.
    // 2. Save the OBSOLETE/FAILED/KILLED maps in obsoleteOutputs to stop
    //    fetching from those maps.
    // 3. Remove TIPFAILED maps from neededOutputs since we don't need their
    //    outputs at all.
    switch (event.getTaskStatus()) {
      case SUCCEEDED:
        successMaps.add(event.getTaskAttemptId());
        break;

      case FAILED:
      case KILLED:
      case OBSOLETE:
        obsoleteMaps.add(event.getTaskAttemptId());
        LOG.info("Ignoring obsolete output of "
            + event.getTaskStatus() + " map-task: '" + event.getTaskAttemptId() + "'");
        break;

      case TIPFAILED:
        tipFailedCount++;
        LOG.info("Ignoring output of failed map TIP: '"
            + event.getTaskAttemptId() + "'");
        break;

      default:
        break;
    }
  }

  // Since slow start is disabled, the reducer can get all completed maps
  public void acceptMapCompletionEvents() throws IOException {

    TaskCompletionEvent[] events = null;

    do {
      MapTaskCompletionEventsUpdate update =
          umbilical.getMapCompletionEvents(
              (org.apache.hadoop.mapred.JobID) reduce.getJobID(),
              fromEventIdx,
              maxEventsToFetch,
              (org.apache.hadoop.mapred.TaskAttemptID) reduce);
      events = update.getMapTaskCompletionEvents();
      LOG.debug("Got " + events.length + " map completion events from "
          + fromEventIdx);

      assert !update.shouldReset() : "Unexpected legacy state";

      // Update the last seen event ID
      fromEventIdx += events.length;

      for (TaskCompletionEvent event : events) {
        resolve(event);
      }
    } while (events.length == maxEventsToFetch);
  }
}
