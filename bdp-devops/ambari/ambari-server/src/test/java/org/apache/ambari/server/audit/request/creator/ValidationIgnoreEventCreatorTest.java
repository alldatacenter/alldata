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

package org.apache.ambari.server.audit.request.creator;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.request.eventcreator.ValidationIgnoreEventCreator;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

public class ValidationIgnoreEventCreatorTest extends AuditEventCreatorTestBase{

  @Test(expected = AssertionError.class)
  public void postTest() {
    ValidationIgnoreEventCreator creator = new ValidationIgnoreEventCreator();

    Request request = AuditEventCreatorTestHelper.createRequest(Request.Type.POST, Resource.Type.Validation, null, null);
    Result result = AuditEventCreatorTestHelper.createResult(new ResultStatus(ResultStatus.STATUS.OK));

    AuditEventCreatorTestHelper.getEvent(creator, request, result);
  }


}
