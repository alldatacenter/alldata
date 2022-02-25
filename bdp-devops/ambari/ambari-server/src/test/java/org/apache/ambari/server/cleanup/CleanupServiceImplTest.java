/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.cleanup;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.orm.dao.Cleanable;
import org.easymock.Capture;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import junit.framework.Assert;


public class CleanupServiceImplTest {

  private static final String CLUSTER_NAME = "cluster-1";
  private static final Long FROM_DATE_TIMESTAMP = 10L;

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private Cleanable cleanableDao;

  private CleanupServiceImpl cleanupServiceImpl;
  private TimeBasedCleanupPolicy cleanupPolicy;
  private Capture<TimeBasedCleanupPolicy> timeBasedCleanupPolicyCapture;
  private Set<Cleanable> cleanables;

  @Before
  public void setUp() throws Exception {
    reset(cleanableDao);
    timeBasedCleanupPolicyCapture = newCapture();
    cleanupPolicy = new TimeBasedCleanupPolicy(CLUSTER_NAME, FROM_DATE_TIMESTAMP);
  }

  @Test
  public void testShouldDaosBeCalledWithTheCleanupPolicy() throws Exception {
    // GIVEN
    cleanables = new HashSet<>();
    cleanables.add(cleanableDao);
    expect(cleanableDao.cleanup(capture(timeBasedCleanupPolicyCapture))).andReturn(2L);

    replay(cleanableDao);
    cleanupServiceImpl = new CleanupServiceImpl(cleanables);

    // WHEN
    cleanupServiceImpl.cleanup(cleanupPolicy);

    // THEN
    Assert.assertNotNull("The argument is null", timeBasedCleanupPolicyCapture.getValue());
    Assert.assertEquals("The cluster name is wrong!", timeBasedCleanupPolicyCapture.getValue().getClusterName(), CLUSTER_NAME);
    Assert.assertEquals("The to date is wrong!", timeBasedCleanupPolicyCapture.getValue().getToDateInMillis(), FROM_DATE_TIMESTAMP);
  }

  @Test
  public void testAffectedRowsNoError() throws Exception {
    // GIVEN
    cleanables = new HashSet<>();
    cleanables.add(cleanableDao);
    expect(cleanableDao.cleanup(cleanupPolicy)).andReturn(2L);

    replay(cleanableDao);
    cleanupServiceImpl = new CleanupServiceImpl(cleanables);

    // WHEN
    CleanupService.CleanupResult res = cleanupServiceImpl.cleanup(cleanupPolicy);

    // THEN
    Assert.assertEquals("The affected rows count is wrong", 2L, res.getAffectedRows());
    Assert.assertEquals("The error count is wrong", 0L, res.getErrorCount());
  }

  @Test
  public void testAffectedRowsWithErrors() throws Exception {
    // GIVEN
    cleanables = new HashSet<>();
    cleanables.add(cleanableDao);
    expect(cleanableDao.cleanup(cleanupPolicy)).andThrow(new RuntimeException());


    replay(cleanableDao);
    cleanupServiceImpl = new CleanupServiceImpl(cleanables);

    // WHEN
    CleanupService.CleanupResult res = cleanupServiceImpl.cleanup(cleanupPolicy);

    // THEN
    Assert.assertEquals("The affected rows count is wrong", 0L, res.getAffectedRows());
    Assert.assertEquals("The error count is wrong", 1L, res.getErrorCount());
  }

}