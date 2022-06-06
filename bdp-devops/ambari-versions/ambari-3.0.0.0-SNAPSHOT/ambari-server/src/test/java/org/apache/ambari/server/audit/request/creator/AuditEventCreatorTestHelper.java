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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.api.services.NamedPropertySet;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultMetadata;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.request.RequestAuditLogger;
import org.apache.ambari.server.audit.request.RequestAuditLoggerImpl;
import org.apache.ambari.server.audit.request.eventcreator.RequestAuditEventCreator;
import org.apache.ambari.server.controller.spi.PageRequest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SortRequest;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.easymock.Capture;
import org.easymock.EasyMock;

public class AuditEventCreatorTestHelper {

  public static AuditEvent getEvent(RequestAuditEventCreator eventCreator, Request request, Result result) {
    Set<RequestAuditEventCreator> creatorSet = new HashSet<>();
    creatorSet.add(eventCreator);

    AuditLogger auditLogger = EasyMock.createNiceMock(AuditLogger.class);
    EasyMock.expect(auditLogger.isEnabled()).andReturn(true).anyTimes();
    Capture<AuditEvent> capture = EasyMock.newCapture();
    auditLogger.log(EasyMock.capture(capture));
    EasyMock.expectLastCall();
    EasyMock.replay(auditLogger);

    RequestAuditLogger requestAuditLogger = new RequestAuditLoggerImpl(auditLogger, creatorSet);

    requestAuditLogger.log(request, result);

    return capture.getValue();
  }

  public static Request createRequest(final Request.Type requestType, final Resource.Type resourceType, final Map<String,Object> properties, final Map<Resource.Type, String> resource) {
    return createRequest(requestType, resourceType, properties, resource, "");
  }

  public static Request createRequest(final Request.Type requestType, final Resource.Type resourceType, final Map<String,Object> properties, final Map<Resource.Type, String> resource, final String queryString) {
    return new Request() {

      RequestBody body = new RequestBody();

      @Override
      public Result process() {
        return null;
      }

      @Override
      public ResourceInstance getResource() {
        return new ResourceInstance() {
          @Override
          public void setKeyValueMap(Map<Resource.Type, String> keyValueMap) {

          }

          @Override
          public Map<Resource.Type, String> getKeyValueMap() {
            return resource;
          }

          @Override
          public Query getQuery() {
            return null;
          }

          @Override
          public ResourceDefinition getResourceDefinition() {
            return new ResourceDefinition() {
              @Override
              public String getPluralName() {
                return null;
              }

              @Override
              public String getSingularName() {
                return null;
              }

              @Override
              public Resource.Type getType() {
                return resourceType;
              }

              @Override
              public Set<SubResourceDefinition> getSubResourceDefinitions() {
                return null;
              }

              @Override
              public List<PostProcessor> getPostProcessors() {
                return null;
              }

              @Override
              public Renderer getRenderer(String name) throws IllegalArgumentException {
                return null;
              }

              @Override
              public Collection<String> getCreateDirectives() {
                return null;
              }

              @Override
              public Collection<String> getReadDirectives() {
                return null;
              }

              @Override
              public boolean isCreatable() {
                return false;
              }

              @Override
              public Collection<String> getUpdateDirectives() {
                return null;
              }

              @Override
              public Collection<String> getDeleteDirectives() {
                return null;
              }
            };
          }

          @Override
          public Map<String, ResourceInstance> getSubResources() {
            return null;
          }

          @Override
          public boolean isCollectionResource() {
            return false;
          }
        };
      }

      @Override
      public String getURI() {
        return "http://example.com:8080/api/v1/test" + queryString;
      }

      @Override
      public Type getRequestType() {
        return requestType;
      }

      @Override
      public int getAPIVersion() {
        return 0;
      }

      @Override
      public Predicate getQueryPredicate() {
        return null;
      }

      @Override
      public Map<String, TemporalInfo> getFields() {
        return null;
      }

      @Override
      public RequestBody getBody() {
        if(properties != null) {
          NamedPropertySet nps = new NamedPropertySet("", properties);
          body.addPropertySet(nps);
        }
        return body;
      }

      @Override
      public Map<String, List<String>> getHttpHeaders() {
        return null;
      }

      @Override
      public PageRequest getPageRequest() {
        return null;
      }

      @Override
      public SortRequest getSortRequest() {
        return null;
      }

      @Override
      public Renderer getRenderer() {
        return null;
      }

      @Override
      public String getRemoteAddress() {
        return "1.2.3.4";
      }
    };
  }

  public static Result createResult(final ResultStatus status) {
    return createResult(status, null);
  }

  public static Result createResult(final ResultStatus status, final TreeNode<Resource> resultTree) {
    return new Result() {
      @Override
      public TreeNode<Resource> getResultTree() {
        return resultTree;
      }

      @Override
      public boolean isSynchronous() {
        return false;
      }

      @Override
      public ResultStatus getStatus() {
        return status;
      }

      @Override
      public void setResultStatus(ResultStatus status) {

      }

      @Override
      public void setResultMetadata(ResultMetadata resultMetadata) {

      }

      @Override
      public ResultMetadata getResultMetadata() {
        return null;
      }
    };
  }
}
