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

# Host Resources
 
 
### API Summary

- [List hosts](hosts.md)
- [View host information](hosts-host.md)
- [Create host](create-host.md)

### Properties

<table>
  <tr>
    <th>Property</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>Hosts/host_name</td>
    <td>The host name</td>  
  </tr>
  <tr>
    <td>Hosts/cluster_name</td>
    <td>The name of the parent cluster</td>  
  </tr>
  <tr>
    <td>Hosts/ip</td>
    <td>The host ip address</td>  
  </tr>
  <tr>
    <td>Hosts/total_mem</td>
    <td>The total memory available on the host</td>  
  </tr>
  <tr>
    <td>Hosts/cpu_count</td>
    <td>The cpu count of the host</td>  
  </tr>
  <tr>
    <td>Hosts/os_arch</td>
    <td>The OS architechture of the host (e.g. x86_64)</td>  
  </tr>
  <tr>
    <td>Hosts/os_type</td>
    <td>The OS type of the host (e.g. centos6)</td>  
  </tr>
  <tr>
    <td>Hosts/rack_info</td>
    <td>The rack info of the host</td>  
  </tr>
  <tr>
    <td>Hosts/last_heartbeat_time</td>
    <td>The time of the last heartbeat from the host in milliseconds since Unix epoch</td>  
  </tr>
  <tr>
    <td>Hosts/last_agent_env</td>
    <td>Environment information from the host</td>  
  </tr>
  <tr>
    <td>Hosts/last_registration_time</td>
    <td>The time of the last registration of the host in milliseconds since Unix epoc</td>  
  </tr>
  <tr>
    <td>Hosts/disk_info</td>
    <td>The host disk information</td>
  </tr>
  <tr>
    <td>Hosts/host_status</td>
    <td>The host status (UNKNOWN, HEALTHY, UNHEALTHY)</td>  
  </tr>
  <tr>
    <td>Hosts/public_host_name</td>
    <td>The public host name.  Note that this property is typically populated during the creation of the host resource in a way that is particular to the environment.  If the public hostname can not be determined then it will be the same as the host_name property. </td>  
  </tr>
  <tr>
    <td>Hosts/host_state</td>
    <td>The host state</td>  
  </tr>
  <tr>
    <td>Hosts/desired_configs</td>
    <td>The desired configurations</td>  
  </tr>
</table>

### States

The current state of a host resource can be determined by looking at the Hosts/host_state property.


    GET api/v1/clusters/c1/hosts/h1?fields=Hosts/host_state

    200 OK
    {
      "href" : "your.ambari.server/api/v1/clusters/c1/hosts/h1?fields=Hosts/host_state",
      "Hosts" : {
        "cluster_name" : "c1",
        "host_state" : "HEALTHY",
        "host_name" : "h1"
      } 
    }

The following table lists the possible values of the host resource Hosts/host_state.
<table>
  <tr>
    <th>State</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>INIT</td>
    <td>New host state.</td>  
  </tr>
  <tr>
    <td>WAITING_FOR_HOST_STATUS_UPDATES</td>
    <td>Registration request is received from the Host but the host has not responded to its status update check.</td>  
  </tr>
  <tr>
    <td>HEALTHY</td>
    <td>The server is receiving heartbeats regularly from the host and the state of the host is healthy.</td>  
  </tr>
  <tr>
    <td>HEARTBEAT_LOST</td>
    <td>The server has not received a heartbeat from the host in the configured heartbeat expiry window.</td>  
  </tr>
  <tr>
    <td>UNHEALTHY</td>
    <td>Host is in unhealthy state as reported either by the Host itself or via any other additional means ( monitoring layer ).</td>  
  </tr>
</table>

