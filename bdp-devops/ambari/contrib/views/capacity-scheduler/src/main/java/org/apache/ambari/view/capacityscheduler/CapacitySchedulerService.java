/**
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

package org.apache.ambari.view.capacityscheduler;

import javax.ws.rs.Path;
import org.apache.ambari.view.ViewContext;
import com.google.inject.Inject;

/**
 * Root capacity-scheduler service
 */
public class CapacitySchedulerService {

  @Inject
  ViewContext context;

  /**
   * @see org.apache.ambari.view.capacityscheduler.HelpService
   * @return service
   */
  @Path("/help")
  public HelpService help() {
    return new HelpService(context);
  }

  /**
   * @see org.apache.ambari.view.capacityscheduler.ConfigurationService
   * @return service
   */
  @Path("/configuration")
  public ConfigurationService configuration() {
    return new ConfigurationService(context);
  }
}
