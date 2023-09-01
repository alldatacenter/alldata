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
import java.util.ArrayList;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.MapTaskCompletionEventsUpdate;
import org.apache.hadoop.mapred.TaskAttemptID;
import org.apache.hadoop.mapred.TaskCompletionEvent;
import org.apache.hadoop.mapred.TaskUmbilicalProtocol;
import org.apache.hadoop.mapreduce.RssMRUtils;
import org.apache.hadoop.mapreduce.TaskType;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventFetcherTest {
  private static final int MAX_EVENTS_TO_FETCH = 100;

  @Test
  public void singlePassEventFetch() throws IOException {
    int mapTaskNum = 97;
    TaskAttemptID tid = new TaskAttemptID("12345", 1, TaskType.MAP, 1, 1);
    JobConf jobConf = new JobConf();
    jobConf.setNumMapTasks(mapTaskNum);
    TaskUmbilicalProtocol umbilical = mock(TaskUmbilicalProtocol.class);
    when(umbilical.getMapCompletionEvents(any(JobID.class),
        eq(0), eq(MAX_EVENTS_TO_FETCH), eq(tid)))
        .thenReturn(getMockedCompletionEventsUpdate(0, mapTaskNum));

    RssEventFetcher ef =
        new RssEventFetcher(1, tid, umbilical, jobConf, MAX_EVENTS_TO_FETCH);
    Roaring64NavigableMap expected = Roaring64NavigableMap.bitmapOf();
    for (int mapIndex = 0; mapIndex < mapTaskNum; mapIndex++) {
      long rssTaskId = RssMRUtils.convertTaskAttemptIdToLong(
        new TaskAttemptID("12345", 1, TaskType.MAP, mapIndex, 0), 1);
      expected.addLong(rssTaskId);
    }

    Roaring64NavigableMap taskIdBitmap = ef.fetchAllRssTaskIds();
    assertEquals(expected, taskIdBitmap);
  }

  @Test
  public void singlePassWithRepeatedSuccessEventFetch() throws IOException {
    int mapTaskNum = 91;
    TaskAttemptID tid = new TaskAttemptID("12345", 1, TaskType.MAP, 1, 1);
    JobConf jobConf = new JobConf();
    jobConf.setNumMapTasks(mapTaskNum);
    TaskUmbilicalProtocol umbilical = mock(TaskUmbilicalProtocol.class);
    when(umbilical.getMapCompletionEvents(any(JobID.class),
        eq(0), eq(MAX_EVENTS_TO_FETCH), eq(tid)))
        .thenReturn(getMockedCompletionEventsUpdate(0, mapTaskNum,
        Sets.newHashSet(70, 80, 90),
        Sets.newHashSet(),
        Sets.newHashSet()));

    RssEventFetcher ef =
        new RssEventFetcher(1, tid, umbilical, jobConf, MAX_EVENTS_TO_FETCH);
    Roaring64NavigableMap expected = Roaring64NavigableMap.bitmapOf();
    for (int mapIndex = 0; mapIndex < mapTaskNum; mapIndex++) {
      long rssTaskId = RssMRUtils.convertTaskAttemptIdToLong(
        new TaskAttemptID("12345", 1, TaskType.MAP, mapIndex, 0), 1);
      expected.addLong(rssTaskId);
    }

    Roaring64NavigableMap taskIdBitmap = ef.fetchAllRssTaskIds();
    assertEquals(expected, taskIdBitmap);
  }

  @Test
  public void multiPassEventFetch() throws IOException {
    int mapTaskNum = 203;
    TaskAttemptID tid = new TaskAttemptID("12345", 1, TaskType.MAP, 1, 1);
    JobConf jobConf = new JobConf();
    jobConf.setNumMapTasks(mapTaskNum);

    TaskUmbilicalProtocol umbilical = mock(TaskUmbilicalProtocol.class);
    when(umbilical.getMapCompletionEvents(any(JobID.class),
        eq(0), eq(MAX_EVENTS_TO_FETCH), eq(tid)))
        .thenReturn(getMockedCompletionEventsUpdate(0, MAX_EVENTS_TO_FETCH));
    when(umbilical.getMapCompletionEvents(any(JobID.class),
        eq(MAX_EVENTS_TO_FETCH), eq(MAX_EVENTS_TO_FETCH), eq(tid)))
        .thenReturn(getMockedCompletionEventsUpdate(MAX_EVENTS_TO_FETCH,
        MAX_EVENTS_TO_FETCH));
    when(umbilical.getMapCompletionEvents(any(JobID.class),
        eq(MAX_EVENTS_TO_FETCH * 2), eq(MAX_EVENTS_TO_FETCH), eq(tid)))
        .thenReturn(getMockedCompletionEventsUpdate(MAX_EVENTS_TO_FETCH * 2, 3));

    RssEventFetcher ef =
        new RssEventFetcher(1, tid, umbilical, jobConf, MAX_EVENTS_TO_FETCH);

    Roaring64NavigableMap expected = Roaring64NavigableMap.bitmapOf();
    for (int mapIndex = 0; mapIndex < mapTaskNum; mapIndex++) {
      long rssTaskId = RssMRUtils.convertTaskAttemptIdToLong(
        new TaskAttemptID("12345", 1, TaskType.MAP, mapIndex, 0),
          1);
      expected.addLong(rssTaskId);
    }
    Roaring64NavigableMap taskIdBitmap = ef.fetchAllRssTaskIds();
    assertEquals(expected, taskIdBitmap);
  }

  @Test
  public void missingEventFetch() throws IOException {
    int mapTaskNum = 100;
    TaskAttemptID tid = new TaskAttemptID("12345", 1, TaskType.MAP, 1, 1);
    JobConf jobConf = new JobConf();
    jobConf.setNumMapTasks(mapTaskNum);
    TaskUmbilicalProtocol umbilical = mock(TaskUmbilicalProtocol.class);
    when(umbilical.getMapCompletionEvents(any(JobID.class),
        eq(0), eq(MAX_EVENTS_TO_FETCH), eq(tid)))
        .thenReturn(getInconsistentCompletionEventsUpdate(0, mapTaskNum,
        Sets.newHashSet(45, 67), Sets.newHashSet()));

    RssEventFetcher ef =
        new RssEventFetcher(1, tid, umbilical, jobConf, MAX_EVENTS_TO_FETCH);
    Roaring64NavigableMap expected = Roaring64NavigableMap.bitmapOf();
    for (int mapIndex = 0; mapIndex < mapTaskNum; mapIndex++) {
      long rssTaskId = RssMRUtils.convertTaskAttemptIdToLong(
        new TaskAttemptID("12345", 1, TaskType.MAP, mapIndex, 0),
          1);
      expected.addLong(rssTaskId);
    }
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> ef.fetchAllRssTaskIds());
    assertEquals("TaskAttemptIDs are inconsistent with map tasks", ex.getMessage());
  }

  @Test
  public void extraEventFetch() throws IOException {
    int mapTaskNum = 100;
    TaskAttemptID tid = new TaskAttemptID("12345", 1, TaskType.MAP, 1, 1);
    JobConf jobConf = new JobConf();
    jobConf.setNumMapTasks(mapTaskNum);
    TaskUmbilicalProtocol umbilical = mock(TaskUmbilicalProtocol.class);
    when(umbilical.getMapCompletionEvents(any(JobID.class),
        eq(0), eq(MAX_EVENTS_TO_FETCH), eq(tid)))
        .thenReturn(getInconsistentCompletionEventsUpdate(0, mapTaskNum,
        Sets.newHashSet(), Sets.newHashSet(101)));

    RssEventFetcher ef =
        new RssEventFetcher(1, tid, umbilical, jobConf, MAX_EVENTS_TO_FETCH);
    Roaring64NavigableMap expected = Roaring64NavigableMap.bitmapOf();
    for (int mapIndex = 0; mapIndex < mapTaskNum; mapIndex++) {
      long rssTaskId = RssMRUtils.convertTaskAttemptIdToLong(
        new TaskAttemptID("12345", 1, TaskType.MAP, mapIndex, 0),
          1);
      expected.addLong(rssTaskId);
    }
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> ef.fetchAllRssTaskIds());
    assertEquals("TaskAttemptIDs are inconsistent with map tasks", ex.getMessage());
  }

  @Test
  public void obsoletedAndTipFailedEventFetch() throws IOException {
    int mapTaskNum = 100;
    TaskAttemptID tid = new TaskAttemptID("12345", 1, TaskType.MAP, 1, 1);
    JobConf jobConf = new JobConf();
    jobConf.setNumMapTasks(mapTaskNum);

    Set<Integer> obsoleted = Sets.newHashSet(70, 71);
    Set<Integer> tipFailed = Sets.newHashSet(89);
    TaskUmbilicalProtocol umbilical = mock(TaskUmbilicalProtocol.class);
    when(umbilical.getMapCompletionEvents(any(JobID.class),
        eq(0), eq(MAX_EVENTS_TO_FETCH), eq(tid)))
        .thenReturn(getMockedCompletionEventsUpdate(0, mapTaskNum,
        Sets.newHashSet(), obsoleted, tipFailed));

    ExceptionReporter reporter = mock(ExceptionReporter.class);

    RssEventFetcher ef =
        new RssEventFetcher(1, tid, umbilical, jobConf, MAX_EVENTS_TO_FETCH);

    Roaring64NavigableMap expected = Roaring64NavigableMap.bitmapOf();
    for (int mapIndex = 0; mapIndex < mapTaskNum; mapIndex++) {
      if (!tipFailed.contains(mapIndex) && !obsoleted.contains(mapIndex)) {
        long rssTaskId = RssMRUtils.convertTaskAttemptIdToLong(
          new TaskAttemptID("12345", 1, TaskType.MAP, mapIndex, 0), 1);
        expected.addLong(rssTaskId);
      }
      if (obsoleted.contains(mapIndex)) {
        long rssTaskId = RssMRUtils.convertTaskAttemptIdToLong(
          new TaskAttemptID("12345", 1, TaskType.MAP, mapIndex, 1),
            1);
        expected.addLong(rssTaskId);
      }
    }

    Roaring64NavigableMap taskIdBitmap = ef.fetchAllRssTaskIds();
    assertEquals(expected, taskIdBitmap);
  }

  private MapTaskCompletionEventsUpdate getMockedCompletionEventsUpdate(
      int startIdx, int numEvents) {
    return getMockedCompletionEventsUpdate(startIdx, numEvents,
      Sets.newHashSet(), Sets.newHashSet(), Sets.newHashSet());
  }

  private MapTaskCompletionEventsUpdate getMockedCompletionEventsUpdate(
      int startIdx,
      int numEvents,
      Set<Integer> repeatedSuccEvents,
      Set<Integer> obsoletedEvents,
      Set<Integer> tipFailedEvents) {
    ArrayList<TaskCompletionEvent> tceList = new ArrayList<org.apache.hadoop.mapred.TaskCompletionEvent>(numEvents);
    for (int i = 0; i < numEvents; ++i) {
      int eventIdx = startIdx + i;
      TaskCompletionEvent.Status status;
      if (!tipFailedEvents.contains(i)) {
        status = TaskCompletionEvent.Status.SUCCEEDED;
      } else {
        status = TaskCompletionEvent.Status.TIPFAILED;
      }
      TaskCompletionEvent tce = new TaskCompletionEvent(eventIdx,
          new TaskAttemptID("12345", 1, TaskType.MAP, eventIdx, 0),
          eventIdx, true, status,
          "http://somehost:8888");
      tceList.add(tce);
    }
    obsoletedEvents.forEach(i -> {
      TaskCompletionEvent tce = new TaskCompletionEvent(tceList.size(),
          new TaskAttemptID("12345", 1, TaskType.MAP, i, 0),
          i, true, TaskCompletionEvent.Status.OBSOLETE,
          "http://somehost:8888");
      tceList.add(tce);
    });

    // use new attempt number - 1
    repeatedSuccEvents.forEach(i -> {
      TaskCompletionEvent tce = new TaskCompletionEvent(tceList.size(),
          new TaskAttemptID("12345", 1, TaskType.MAP, i, 1),
          i, true, TaskCompletionEvent.Status.SUCCEEDED,
          "http://somehost:8888");
      tceList.add(tce);
    });

    // use new attempt number - 1
    obsoletedEvents.forEach(i -> {
      TaskCompletionEvent tce = new TaskCompletionEvent(tceList.size(),
          new TaskAttemptID("12345", 1, TaskType.MAP, i, 1),
          i, true, TaskCompletionEvent.Status.SUCCEEDED,
          "http://somehost:8888");
      tceList.add(tce);
    });
    TaskCompletionEvent[] events = {};
    return new MapTaskCompletionEventsUpdate(tceList.toArray(events), false);
  }


  private MapTaskCompletionEventsUpdate getInconsistentCompletionEventsUpdate(
      int startIdx, int numEvents, Set<Integer> missEvents, Set<Integer> extraEvents) {
    ArrayList<TaskCompletionEvent> tceList = new ArrayList<org.apache.hadoop.mapred.TaskCompletionEvent>(numEvents);
    for (int i = 0; i < numEvents; ++i) {
      int eventIdx = startIdx + i;
      if (!missEvents.contains(eventIdx)) {
        TaskCompletionEvent.Status status = TaskCompletionEvent.Status.SUCCEEDED;
        TaskCompletionEvent tce = new TaskCompletionEvent(eventIdx,
            new TaskAttemptID("12345", 1, TaskType.MAP, eventIdx, 0),
            eventIdx, true, status,
            "http://somehost:8888");
        tceList.add(tce);
      }
    }

    extraEvents.forEach(i -> {
      TaskCompletionEvent tce = new TaskCompletionEvent(i,
          new TaskAttemptID("12345", 1, TaskType.MAP, i, 1),
          i, true, TaskCompletionEvent.Status.SUCCEEDED,
          "http://somehost:8888");
      tceList.add(tce);
    });
    TaskCompletionEvent[] events = {};
    return new MapTaskCompletionEventsUpdate(tceList.toArray(events), false);
  }
}
