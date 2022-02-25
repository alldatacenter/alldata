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
package org.apache.ambari.annotations;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Properties;

import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.annotations.TransactionalLock.LockType;
import org.apache.ambari.server.configuration.Configuration;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests {@link TransactionalLock} and associated classes.
 */
public class TransactionalLockTest {

  /**
   * Tests that {@link LockArea} is correctly enabled/disabled.
   */
  @Test
  public void testLockAreaEnabled() throws Exception {
    final Properties ambariProperties = new Properties();
    ambariProperties.put(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED.getKey(), "true");
    Configuration configuration = new Configuration(ambariProperties);

    LockArea lockArea = LockArea.HRC_STATUS_CACHE;
    lockArea.clearEnabled();

    Assert.assertTrue(lockArea.isEnabled(configuration));
  }

  /**
   * Tests that {@link LockArea} is correctly enabled/disabled.
   */
  @Test
  public void testLockAreaEnabledDisabled() throws Exception {
    final Properties ambariProperties = new Properties();
    ambariProperties.put(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED.getKey(), "false");
    Configuration configuration = new Configuration(ambariProperties);

    LockArea lockArea = LockArea.HRC_STATUS_CACHE;
    lockArea.clearEnabled();

    Assert.assertFalse(lockArea.isEnabled(configuration));
  }

  /**
   * Tests that annotations are actually equal (more of a proof of the javadoc
   * than anything).
   */
  @Test
  public void testAnnotationEquality() {
    HashSet<TransactionalLock> annotations = new HashSet<>();

    int annotationsFound = 0;
    Method[] methods = getClass().getDeclaredMethods();
    for (Method method : methods) {
      TransactionalLock annotation = method.getAnnotation(TransactionalLock.class);
      if (null != annotation) {
        annotations.add(annotation);
        annotationsFound++;
      }
    }

    // there should be 3 discovered annotations, but only 2 in the hashset since
    // they were collapsed
    Assert.assertEquals(2, annotations.size());
    Assert.assertEquals(3, annotationsFound);
  }

  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.READ)
  private void transactionalHRCRead() {
  }

  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.READ)
  private void transactionalHRCRead2() {
  }

  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
  private void transactionalHRCWrite() {
  }
}