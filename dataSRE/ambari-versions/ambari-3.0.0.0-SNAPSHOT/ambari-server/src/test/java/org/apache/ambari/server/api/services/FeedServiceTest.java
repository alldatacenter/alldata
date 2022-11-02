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

package org.apache.ambari.server.api.services;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;


/**
 * Unit tests for FeedService.
 */
public class FeedServiceTest extends BaseServiceTest {


  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getFeed
    FeedService feedService = new TestFeedService("feedName");
    Method m = feedService.getClass().getMethod("getFeed", String.class, HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {null, getHttpHeaders(), getUriInfo(), "feedName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, feedService, m, args, null));

    //getFeeds
    feedService = new TestFeedService(null);
    m = feedService.getClass().getMethod("getFeeds", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, feedService, m, args, null));

    //createFeed
    feedService = new TestFeedService("feedName");
    m = feedService.getClass().getMethod("createFeed", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "feedName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, feedService, m, args, "body"));

    //updateFeed
    feedService = new TestFeedService("feedName");
    m = feedService.getClass().getMethod("updateFeed", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "feedName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, feedService, m, args, "body"));

    //deleteFeed
    feedService = new TestFeedService("feedName");
    m = feedService.getClass().getMethod("deleteFeed", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "feedName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, feedService, m, args, null));

    return listInvocations;
  }


  private class TestFeedService extends FeedService {
    private String m_feedId;

    private TestFeedService(String feedId) {
      m_feedId = feedId;
    }

    @Override
    ResourceInstance createFeedResource(String feedName) {
      assertEquals(m_feedId, feedName);
      return getTestResource();
    }

    @Override
    RequestFactory getRequestFactory() {
      return getTestRequestFactory();
    }

    @Override
    protected RequestBodyParser getBodyParser() {
      return getTestBodyParser();
    }

    @Override
    protected ResultSerializer getResultSerializer() {
      return getTestResultSerializer();
    }
  }

  //todo: test getHostHandler, getServiceHandler, getHostComponentHandler
}
