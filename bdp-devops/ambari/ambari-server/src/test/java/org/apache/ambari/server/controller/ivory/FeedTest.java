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

package org.apache.ambari.server.controller.ivory;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Feed tests.
 */
public class FeedTest {
  @Test
  public void testGetName() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "st", "end", "l", "a", "target", "st", "end", "l", "a", props);
    Assert.assertEquals("Feed1", feed.getName());
  }

  @Test
  public void testGetDescription() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "desc", "s", "sch", "source", "st", "end", "l", "a", "target", "st", "end", "l", "a", props);
    Assert.assertEquals("desc", feed.getDescription());
  }

  @Test
  public void testGetStatus() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "WAITING", "sch", "source", "st", "end", "l", "a", "target", "st", "end", "l", "a", props);
    Assert.assertEquals("WAITING", feed.getStatus());
  }

  @Test
  public void testGetSchedule() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "WAITING", "frequency", "source", "st", "end", "l", "a", "target", "st", "end", "l", "a", props);
    Assert.assertEquals("frequency", feed.getSchedule());
  }

  @Test
  public void testGetSourceClusterName() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "st", "end", "l", "a", "target", "st", "end", "l", "a", props);
    Assert.assertEquals("source", feed.getSourceClusterName());
  }

  @Test
  public void testGetSourceClusterStart() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "sst", "end", "l", "a", "target", "st", "end", "l", "a", props);
    Assert.assertEquals("sst", feed.getSourceClusterStart());
  }

  @Test
  public void testGetSourceClusterEnd() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "st", "send", "l", "a", "target", "st", "end", "l", "a", props);
    Assert.assertEquals("send", feed.getSourceClusterEnd());
  }

  @Test
  public void testGetSourceClusterLimit() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "st", "end", "sl", "a", "target", "st", "end", "l", "a", props);
    Assert.assertEquals("sl", feed.getSourceClusterLimit());
  }

  @Test
  public void testGetSourceClusterAction() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "st", "end", "l", "sa", "target", "st", "end", "l", "a", props);
    Assert.assertEquals("sa", feed.getSourceClusterAction());
  }

  @Test
  public void testGetTargetClusterName() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "st", "end", "l", "a", "target", "st", "end", "l", "a", props);
    Assert.assertEquals("target", feed.getTargetClusterName());
  }

  @Test
  public void testGetTargetClusterStart() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "sst", "end", "l", "a", "target", "tst", "end", "l", "a", props);
    Assert.assertEquals("tst", feed.getTargetClusterStart());
  }

  @Test
  public void testGetTargetClusterEnd() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "st", "send", "l", "a", "target", "st", "tend", "l", "a", props);
    Assert.assertEquals("tend", feed.getTargetClusterEnd());
  }

  @Test
  public void testGetTargetClusterLimit() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "st", "end", "sl", "a", "target", "st", "end", "tl", "a", props);
    Assert.assertEquals("tl", feed.getTargetClusterLimit());
  }

  @Test
  public void testGetTargetClusterAction() throws Exception {
    Map<String,String> props = new HashMap<>();

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "st", "end", "l", "sa", "target", "st", "end", "l", "ta", props);
    Assert.assertEquals("ta", feed.getTargetClusterAction());
  }

  @Test
  public void testGetProperties() throws Exception {
    Map<String,String> props = new HashMap<>();
    props.put("p1", "v1");

    Feed feed = new Feed("Feed1", "d", "s", "sch", "source", "st", "end", "l", "sa", "target", "st", "end", "l", "ta", props);
    Assert.assertEquals(props, feed.getProperties());
  }
}
