/*
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

package org.apache.ambari.server.audit;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.ambari.server.audit.event.OperationStatusAuditEvent;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class OperationStatusAuditEventTest {

  @Test
  public void testAuditMessage() throws Exception {
    // Given
    Long testRequestId = 100L;
    String testStatus = "IN PROGRESS";

    String testRemoteIp = "127.0.0.1";

    OperationStatusAuditEvent evnt = OperationStatusAuditEvent.builder()
      .withTimestamp(System.currentTimeMillis())
      .withRequestId(testRequestId.toString())
      .withStatus(testStatus)
      .withRemoteIp(testRemoteIp)
      .withUserName("testuser")
      .withRequestContext("Start Service")
      .build();

    // When
    String actualAuditMessage = evnt.getAuditMessage();

    // Then
    String expectedAuditMessage = String.format("User(testuser), RemoteIp(127.0.0.1), Operation(Start Service), Status(%s), RequestId(%s)", testStatus, testRequestId);

    assertThat(actualAuditMessage, equalTo(expectedAuditMessage));
  }

  @Test
  public void testTimestamp() throws Exception {
    // Given
    long testTimestamp = System.currentTimeMillis();
    OperationStatusAuditEvent evnt = OperationStatusAuditEvent.builder()
      .withTimestamp(testTimestamp)
      .build();

    // When
    long actualTimestamp = evnt.getTimestamp();

    // Then
    assertThat(actualTimestamp, equalTo(testTimestamp));

  }

  @Test
  public void testEquals() throws Exception {
    EqualsVerifier.forClass(OperationStatusAuditEvent.class)
      .verify();
  }
}
