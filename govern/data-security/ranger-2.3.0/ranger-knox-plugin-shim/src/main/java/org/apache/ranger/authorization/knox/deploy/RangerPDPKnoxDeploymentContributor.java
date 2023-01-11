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
package org.apache.ranger.authorization.knox.deploy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.knox.gateway.deploy.DeploymentContext;
import org.apache.knox.gateway.deploy.ProviderDeploymentContributorBase;
import org.apache.knox.gateway.descriptor.FilterParamDescriptor;
import org.apache.knox.gateway.descriptor.ResourceDescriptor;
import org.apache.knox.gateway.topology.Provider;
import org.apache.knox.gateway.topology.Service;

public class RangerPDPKnoxDeploymentContributor extends ProviderDeploymentContributorBase {

  private static final String FILTER_CLASSNAME = "org.apache.ranger.authorization.knox.RangerPDPKnoxFilter";

  @Override
  public String getRole() {
    return "authorization";
  }

  @Override
  public String getName() {
	// This MUST match a corresponding change in the topology file.  For upgrade purposes this name remains as is, i.e. XASecure* and not Ranger*.
    return "XASecurePDPKnox";
  }

  @Override
  public void contributeProvider( DeploymentContext context, Provider provider ) {
  }

  @Override
  public void contributeFilter( DeploymentContext context, Provider provider, Service service,
      ResourceDescriptor resource, List<FilterParamDescriptor> params ) {
    if (params == null) {
      params = new ArrayList<FilterParamDescriptor>();
    }
    // add resource role to params so that we can determine the acls to enforce at runtime
    params.add( resource.createFilterParam().name( "resource.role" ).value(resource.role() ) );

    // blindly add all the provider params as filter init params
    // this will include any {resource.role}-ACLS parameters to be enforced - such as NAMENODE-ACLS
    Map<String, String> providerParams = provider.getParams();
    for(Entry<String, String> entry : providerParams.entrySet()) {
      params.add( resource.createFilterParam().name( entry.getKey().toLowerCase() ).value( entry.getValue() ) );
    }

    resource.addFilter().name( getName() ).role( getRole() ).impl( FILTER_CLASSNAME ).params( params );
  }
}
