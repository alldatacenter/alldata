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
package org.apache.ambari.server.api.resources;

import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;

public class RequestScheduleResourceDefinition extends BaseResourceDefinition {

  public RequestScheduleResourceDefinition() {
    super(Resource.Type.RequestSchedule);
  }

  @Override
  public String getPluralName() {
    return "request_schedules";
  }

  @Override
  public String getSingularName() {
    return "request_schedule";
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    return Collections.singletonList(new
      RequestScheduleHrefPostProcessor());
  }

  private class RequestScheduleHrefPostProcessor implements PostProcessor {

    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      StringBuilder sb = new StringBuilder();
      String[] tokens = href.split("/");

      for (int i = 0; i < tokens.length; ++i) {
        String s = tokens[i];
        sb.append(s).append('/');
        if ("clusters".equals(s)) {
          sb.append(tokens[i + 1]).append('/');
          break;
        }
      }

      Object scheduleId = resultNode.getObject()
        .getPropertyValue(getClusterController()
          .getSchema(Resource.Type.RequestSchedule)
            .getKeyPropertyId(Resource.Type.RequestSchedule));

      sb.append("request_schedules/").append(scheduleId);

      resultNode.setProperty("href", sb.toString());
    }
  }
}
