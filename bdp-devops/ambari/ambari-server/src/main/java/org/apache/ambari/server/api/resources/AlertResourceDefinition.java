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

import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer;
import org.apache.ambari.server.api.query.render.AlertSummaryRenderer;
import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Definition for alert resources.
 */
public class AlertResourceDefinition extends BaseResourceDefinition {

  /**
   * Constructor.
   *
   */
  public AlertResourceDefinition() {
    super(Resource.Type.Alert);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPluralName() {
    return "alerts";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSingularName() {
    return "alert";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Renderer getRenderer(String name) {
    if (null == name) {
      return super.getRenderer(name);
    }

    if (name.equals("summary")) {
      return new AlertSummaryRenderer();
    } else if (name.equals("groupedSummary")) {
      return new AlertSummaryGroupedRenderer();
    } else {
      return super.getRenderer(name);
    }
  }
}
