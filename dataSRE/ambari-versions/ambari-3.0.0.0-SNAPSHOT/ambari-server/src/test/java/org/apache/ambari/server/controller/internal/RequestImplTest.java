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

package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

/**
 *
 */
public class RequestImplTest {

  private static final Set<String> propertyIds = new HashSet<>();

  static {
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p2"));
    propertyIds.add(PropertyHelper.getPropertyId("c2", "p3"));
    propertyIds.add(PropertyHelper.getPropertyId("c3", "p4"));
  }

  @Before
  public void setup() throws Exception {
    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    ResourceProviderFactory resourceProviderFactory = injector.getInstance(ResourceProviderFactory.class);
    AbstractControllerResourceProvider.init(resourceProviderFactory);

    DefaultProviderModule defaultProviderModule = injector.getInstance(DefaultProviderModule.class);
    for( Resource.Type type : Resource.Type.values() ){
      try {
        defaultProviderModule.getResourceProvider(type);
      } catch (Exception exception) {
        // ignore
      }
    }
  }

  @Test
  public void testGetPropertyIds() {
    Request request = PropertyHelper.getReadRequest(propertyIds);

    Assert.assertEquals(propertyIds, request.getPropertyIds());
  }

  @Test
  public void testValidPropertyIds() {
    Request request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.HostComponent));
    Set<String> validPropertyIds = request.getPropertyIds();

    //HostComponent resource properties
    Assert.assertFalse(validPropertyIds.contains("HostRoles/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("params/run_smoke_test"));
    Assert.assertTrue(validPropertyIds.contains("HostRoles/actual_configs"));
    Assert.assertTrue(validPropertyIds.contains("HostRoles/desired_stack_id"));
    Assert.assertTrue(validPropertyIds.contains("HostRoles/version"));
    Assert.assertTrue(validPropertyIds.contains("HostRoles/desired_repository_version"));
    Assert.assertTrue(validPropertyIds.contains("HostRoles/desired_state"));
    Assert.assertTrue(validPropertyIds.contains("HostRoles/state"));
    Assert.assertTrue(validPropertyIds.contains("HostRoles/component_name"));
    Assert.assertTrue(validPropertyIds.contains("HostRoles/host_name"));
    Assert.assertTrue(validPropertyIds.contains("HostRoles/cluster_name"));
    Assert.assertTrue(validPropertyIds.contains("HostRoles/role_id"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.Cluster));
    validPropertyIds = request.getPropertyIds();

    //Cluster resource properties
    Assert.assertFalse(validPropertyIds.contains("Clusters/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("Clusters/cluster_id"));
    Assert.assertTrue(validPropertyIds.contains("Clusters/cluster_name"));
    Assert.assertTrue(validPropertyIds.contains("Clusters/version"));
    Assert.assertTrue(validPropertyIds.contains("Clusters/state"));
    Assert.assertTrue(validPropertyIds.contains("Clusters/desired_configs"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.Service));
    validPropertyIds = request.getPropertyIds();

    //Service resource properties
    Assert.assertFalse(validPropertyIds.contains("ServiceInfo/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("ServiceInfo/service_name"));
    Assert.assertTrue(validPropertyIds.contains("ServiceInfo/cluster_name"));
    Assert.assertTrue(validPropertyIds.contains("ServiceInfo/state"));
    Assert.assertTrue(validPropertyIds.contains("params/run_smoke_test"));
    Assert.assertTrue(validPropertyIds.contains("params/reconfigure_client"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.Host));
    validPropertyIds = request.getPropertyIds();

    //Host resource properties
    Assert.assertFalse(validPropertyIds.contains("Hosts/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/cluster_name"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/host_name"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/ip"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/attributes"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/total_mem"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/cpu_count"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/ph_cpu_count"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/os_arch"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/os_type"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/rack_info"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/last_heartbeat_time"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/last_agent_env"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/last_registration_time"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/disk_info"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/host_status"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/host_health_report"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/public_host_name"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/host_state"));
    Assert.assertTrue(validPropertyIds.contains("Hosts/desired_configs"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.Component));
    validPropertyIds = request.getPropertyIds();

    //Component resource properties
    Assert.assertFalse(validPropertyIds.contains("ServiceComponentInfo/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("ServiceComponentInfo/service_name"));
    Assert.assertTrue(validPropertyIds.contains("ServiceComponentInfo/component_name"));
    Assert.assertTrue(validPropertyIds.contains("ServiceComponentInfo/cluster_name"));
    Assert.assertTrue(validPropertyIds.contains("ServiceComponentInfo/state"));
    Assert.assertTrue(validPropertyIds.contains("ServiceComponentInfo/display_name"));
    Assert.assertTrue(validPropertyIds.contains("params/run_smoke_test"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.Action));
    validPropertyIds = request.getPropertyIds();

    //Action resource properties
    Assert.assertFalse(validPropertyIds.contains("Action/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("Actions/action_name"));
    Assert.assertTrue(validPropertyIds.contains("Actions/action_type"));
    Assert.assertTrue(validPropertyIds.contains("Actions/inputs"));
    Assert.assertTrue(validPropertyIds.contains("Actions/target_service"));
    Assert.assertTrue(validPropertyIds.contains("Actions/target_component"));
    Assert.assertTrue(validPropertyIds.contains("Actions/description"));
    Assert.assertTrue(validPropertyIds.contains("Actions/target_type"));
    Assert.assertTrue(validPropertyIds.contains("Actions/default_timeout"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.Request));
    validPropertyIds = request.getPropertyIds();

    //Request resource properties
    Assert.assertFalse(validPropertyIds.contains("Requests/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("Requests/id"));
    Assert.assertTrue(validPropertyIds.contains("Requests/cluster_name"));
    Assert.assertTrue(validPropertyIds.contains("Requests/request_status"));
    Assert.assertTrue(validPropertyIds.contains("Requests/request_context"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.Task));
    validPropertyIds = request.getPropertyIds();

    //Task resource properties
    Assert.assertFalse(validPropertyIds.contains("Task/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/id"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/request_id"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/cluster_name"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/stage_id"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/host_name"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/command"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/role"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/status"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/exit_code"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/stderr"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/stdout"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/start_time"));
    Assert.assertTrue(validPropertyIds.contains("Tasks/attempt_cnt"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.User));
    validPropertyIds = request.getPropertyIds();
    Assert.assertFalse(validPropertyIds.contains("Users/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("Users/user_name"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.Stack));
    validPropertyIds = request.getPropertyIds();

    //Stack resource properties
    Assert.assertFalse(validPropertyIds.contains("Stacks/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("Stacks/stack_name"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.StackVersion));
    validPropertyIds = request.getPropertyIds();

    //StackVersion resource properties
    Assert.assertFalse(validPropertyIds.contains("Versions/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("Versions/stack_name"));
    Assert.assertTrue(validPropertyIds.contains("Versions/stack_version"));
    Assert.assertTrue(validPropertyIds.contains("Versions/min_upgrade_version"));

    request = PropertyHelper.getReadRequest(OperatingSystemResourceProvider.propertyIds);
    validPropertyIds = request.getPropertyIds();

    //OperatingSystem resource properties
    Assert.assertFalse(validPropertyIds.contains("OperatingSystems/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("OperatingSystems/stack_name"));
    Assert.assertTrue(validPropertyIds.contains("OperatingSystems/stack_version"));
    Assert.assertTrue(validPropertyIds.contains("OperatingSystems/os_type"));

    request = PropertyHelper.getReadRequest(RepositoryResourceProvider.propertyIds);
    validPropertyIds = request.getPropertyIds();

    //Repository resource properties
    Assert.assertFalse(validPropertyIds.contains("Repositories/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("Repositories/stack_name"));
    Assert.assertTrue(validPropertyIds.contains("Repositories/stack_version"));
    Assert.assertTrue(validPropertyIds.contains("Repositories/os_type"));
    Assert.assertTrue(validPropertyIds.contains("Repositories/base_url"));
    Assert.assertTrue(validPropertyIds.contains("Repositories/repo_id"));
    Assert.assertTrue(validPropertyIds.contains("Repositories/repo_name"));
    Assert.assertTrue(validPropertyIds.contains("Repositories/mirrors_list"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.StackService));
    validPropertyIds = request.getPropertyIds();

    //Repository resource properties
    Assert.assertFalse(validPropertyIds.contains("StackServices/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("StackServices/stack_name"));
    Assert.assertTrue(validPropertyIds.contains("StackServices/stack_version"));
    Assert.assertTrue(validPropertyIds.contains("StackServices/service_name"));
    Assert.assertTrue(validPropertyIds.contains("StackServices/user_name"));
    Assert.assertTrue(validPropertyIds.contains("StackServices/comments"));
    Assert.assertTrue(validPropertyIds.contains("StackServices/service_version"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.StackConfiguration));
    validPropertyIds = request.getPropertyIds();

    //StackConfigurations resource properties
    Assert.assertFalse(validPropertyIds.contains("StackConfigurations/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("StackConfigurations/stack_name"));
    Assert.assertTrue(validPropertyIds.contains("StackConfigurations/stack_version"));
    Assert.assertTrue(validPropertyIds.contains("StackConfigurations/service_name"));
    Assert.assertTrue(validPropertyIds.contains("StackConfigurations/property_name"));
    Assert.assertTrue(validPropertyIds.contains("StackConfigurations/property_description"));
    Assert.assertTrue(validPropertyIds.contains("StackConfigurations/property_value"));

    request = PropertyHelper.getReadRequest(PropertyHelper.getPropertyIds(Resource.Type.StackServiceComponent));
    validPropertyIds = request.getPropertyIds();

    //StackServiceComponent resource properties
    Assert.assertFalse(validPropertyIds.contains("StackServiceComponents/unsupported_property_id"));
    Assert.assertTrue(validPropertyIds.contains("StackServiceComponents/stack_version"));
    Assert.assertTrue(validPropertyIds.contains("StackServiceComponents/stack_name"));
    Assert.assertTrue(validPropertyIds.contains("StackServiceComponents/service_name"));
    Assert.assertTrue(validPropertyIds.contains("StackServiceComponents/component_name"));
    Assert.assertTrue(validPropertyIds.contains("StackServiceComponents/component_category"));
    Assert.assertTrue(validPropertyIds.contains("StackServiceComponents/is_master"));
    Assert.assertTrue(validPropertyIds.contains("StackServiceComponents/is_client"));
  }

  @Test
  public void testDryRunRequest() {
    Request dryRunRequest = PropertyHelper.getCreateRequest(Collections.emptySet(), Collections.singletonMap(Request.DIRECTIVE_DRY_RUN, "true"));
    Request nonDryRunReqest1 = PropertyHelper.getCreateRequest(Collections.emptySet(), Collections.singletonMap(Request.DIRECTIVE_DRY_RUN, "false"));
    Request nonDryRunReqest2 = PropertyHelper.getCreateRequest(Collections.emptySet(), Collections.emptyMap());

    Assert.assertTrue(dryRunRequest.isDryRunRequest());
    Assert.assertFalse(nonDryRunReqest1.isDryRunRequest());
    Assert.assertFalse(nonDryRunReqest2.isDryRunRequest());
  }
}
