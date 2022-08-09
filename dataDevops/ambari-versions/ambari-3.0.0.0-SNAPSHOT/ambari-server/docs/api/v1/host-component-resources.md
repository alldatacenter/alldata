<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Host Component Resources
### API Summary


- [List host components](host-components.md)
- [View host component information](host-component.md)
- [Create host component](create-hostcomponent.md)
- [Update host component](update-hostcomponent.md)

### Properties

<table>
  <tr>
    <th>Property</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>HostRoles/role_id</td>
    <td>The host component id</td>  
  </tr>
  <tr>
    <td>HostRoles/cluster_name</td>
    <td>The name of the parent cluster</td>  
  </tr>
  <tr>
    <td>HostRoles/host_name</td>
    <td>The name of the parent host</td>  
  </tr>
  <tr>
    <td>HostRoles/component_name</td>
    <td>The name of the parent component</td>
  </tr>
  <tr>
    <td>HostRoles/state</td>
    <td>The state of the host component</td>  
  </tr>
  <tr>
    <td>HostRoles/actual_configs</td>
    <td>The actual configuration</td>  
  </tr>
  <tr>
    <td>HostRoles/desired_configs</td>
    <td>The desired configuration</td>  
  </tr>
  <tr>
    <td>HostRoles/stack_id</td>
    <td>The stack id (e.g. HDP-1.3.0)</td>  
  </tr>
</table>



### States

The current state of a host component resource can be determined by looking at the ServiceComponentInfo/state property.


    GET api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=ServiceComponentInfo/state

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=ServiceComponentInfo/state",
      "ServiceComponentInfo" : {
        "cluster_name" : "c1",
        "component_name" : "NAMENODE",
        "state" : "INSTALLED",
        "service_name" : "HDFS"
      }
    }

The following table lists the possible values of the service resource ServiceComponentInfo/state property.
<table>
  <tr>
    <th>State</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>INIT</td>
    <td>The initial clean state after the component is first created.</td>  
  </tr>
  <tr>
    <td>INSTALLING</td>
    <td>In the process of installing the component.</td>  
  </tr>
  <tr>
    <td>INSTALL_FAILED</td>
    <td>The component install failed.</td>  
  </tr>
  <tr>
    <td>INSTALLED</td>
    <td>The component has been installed successfully but is not currently running.</td>  
  </tr>
  <tr>
    <td>STARTING</td>
    <td>In the process of starting the component.</td>  
  </tr>
  <tr>
    <td>STARTED</td>
    <td>The component has been installed and started.</td>  
  </tr>
  <tr>
    <td>STOPPING</td>
    <td>In the process of stopping the component.</td>  
  </tr>

  <tr>
    <td>UNINSTALLING</td>
    <td>In the process of uninstalling the component.</td>  
  </tr>
  <tr>
    <td>UNINSTALLED</td>
    <td>The component has been successfully uninstalled.</td>  
  </tr>
  <tr>
    <td>WIPING_OUT</td>
    <td>In the process of wiping out the installed component.</td>  
  </tr>
  <tr>
    <td>UPGRADING</td>
    <td>In the process of upgrading the component.</td>  
  </tr>
  <tr>
    <td>MAINTENANCE</td>
    <td>The component has been marked for maintenance.</td>  
  </tr>
  <tr>
    <td>UNKNOWN</td>
    <td>The component state can not be determined.</td>  
  </tr>
</table>


### Starting a Host Component
A component can be started through the API by setting its state to be STARTED (see [update host component](update-hostcomponent.md)).

### Stopping a Host Component
A component can be stopped through the API by setting its state to be INSTALLED (see [update host component](update-hostcomponent.md)).

### Maintenance

The user can update the desired state of a host component through the API to be MAINTENANCE (see [update host component](update-hostcomponent.md)).  When a host component is into maintenance state it is basically taken off line. This state can be used, for example, to move a component like NameNode.  The NameNode component can be put in MAINTENANCE mode and then a new NameNode can be created for the service. 



