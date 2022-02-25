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
package org.apache.ambari.server.api;

import org.apache.ambari.server.api.handlers.CreateHandlerTest;
import org.apache.ambari.server.api.handlers.DeleteHandlerTest;
import org.apache.ambari.server.api.handlers.QueryCreateHandlerTest;
import org.apache.ambari.server.api.handlers.ReadHandlerTest;
import org.apache.ambari.server.api.handlers.UpdateHandlerTest;
import org.apache.ambari.server.api.predicate.operators.AndOperatorTest;
import org.apache.ambari.server.api.predicate.operators.EqualsOperatorTest;
import org.apache.ambari.server.api.predicate.operators.GreaterEqualsOperatorTest;
import org.apache.ambari.server.api.predicate.operators.GreaterOperatorTest;
import org.apache.ambari.server.api.predicate.operators.InOperatorTest;
import org.apache.ambari.server.api.predicate.operators.LessEqualsOperatorTest;
import org.apache.ambari.server.api.predicate.operators.NotEqualsOperatorTest;
import org.apache.ambari.server.api.predicate.operators.NotOperatorTest;
import org.apache.ambari.server.api.predicate.operators.OrOperatorTest;
import org.apache.ambari.server.api.query.QueryImplTest;
import org.apache.ambari.server.api.services.ClusterServiceTest;
import org.apache.ambari.server.api.services.ComponentServiceTest;
import org.apache.ambari.server.api.services.DeleteRequestTest;
import org.apache.ambari.server.api.services.GetRequestTest;
import org.apache.ambari.server.api.services.HostComponentServiceTest;
import org.apache.ambari.server.api.services.HostServiceTest;
import org.apache.ambari.server.api.services.NamedPropertySetTest;
import org.apache.ambari.server.api.services.PersistenceManagerImplTest;
import org.apache.ambari.server.api.services.PostRequestTest;
import org.apache.ambari.server.api.services.PutRequestTest;
import org.apache.ambari.server.api.services.QueryPostRequestTest;
import org.apache.ambari.server.api.services.RequestBodyTest;
import org.apache.ambari.server.api.services.ServiceServiceTest;
import org.apache.ambari.server.api.services.parsers.BodyParseExceptionTest;
import org.apache.ambari.server.api.services.parsers.JsonRequestBodyParserTest;
import org.apache.ambari.server.api.services.serializers.JsonSerializerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * All api unit tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ClusterServiceTest.class, HostServiceTest.class, ServiceServiceTest.class,
    ComponentServiceTest.class, HostComponentServiceTest.class, ReadHandlerTest.class, QueryImplTest.class,
    JsonRequestBodyParserTest.class, CreateHandlerTest.class, UpdateHandlerTest.class, DeleteHandlerTest.class,
    PersistenceManagerImplTest.class, GetRequestTest.class, PutRequestTest.class, PostRequestTest.class,
    DeleteRequestTest.class, QueryPostRequestTest.class, JsonSerializerTest.class, QueryCreateHandlerTest.class,
    InOperatorTest.class,AndOperatorTest.class, OrOperatorTest.class, EqualsOperatorTest.class,
    GreaterEqualsOperatorTest.class, GreaterOperatorTest.class, LessEqualsOperatorTest.class,
    LessEqualsOperatorTest.class, NotEqualsOperatorTest.class, NotOperatorTest.class, RequestBodyTest.class,
    NamedPropertySetTest.class, BodyParseExceptionTest.class})
public class TestSuite {
}
