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

# Service Resources
Service resources are services of a Hadoop cluster (e.g. HDFS, MapReduce and Ganglia).  Service resources are sub-resources of clusters. 

### API Summary

- [List services](services.md)
- [View service information](services-service.md)
- [Create service](create-service.md)
- [Update services](update-services.md)
- [Update service](update-service.md)

### Properties

<table>
  <tr>
    <th>Property</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>ServiceInfo/service_name</td>
    <td>The service name</td>  
  </tr>
  <tr>
    <td>ServiceInfo/cluster_name</td>
    <td>The parent cluster name</td>  
  </tr>
  <tr>
    <td>ServiceInfo/state</td>
    <td>The current state of the service</td>  
  </tr>
  <tr>
    <td>ServiceInfo/desired_configs</td>
    <td>The desired configurations</td>  
  </tr>
</table>



### States

The current state of a service resource can be determined by looking at the ServiceInfo/state property.


    GET api/v1/clusters/c1/services/HDFS?fields=ServiceInfo/state

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS?fields=ServiceInfo/state",
      "ServiceInfo" : {
        "cluster_name" : "c1",
        "state" : "INSTALLED",
        "service_name" : "HDFS"
      }
    }

The following table lists the possible values of the service resource ServiceInfo/state property.
<table>
  <tr>
    <th>State</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>INIT</td>
    <td>The initial clean state after the service is first created.</td>  
  </tr>
  <tr>
    <td>INSTALLING</td>
    <td>In the process of installing the service.</td>  
  </tr>
  <tr>
    <td>INSTALL_FAILED</td>
    <td>The service install failed.</td>  
  </tr>
  <tr>
    <td>INSTALLED</td>
    <td>The service has been installed successfully but is not currently running.</td>  
  </tr>
  <tr>
    <td>STARTING</td>
    <td>In the process of starting the service.</td>  
  </tr>
  <tr>
    <td>STARTED</td>
    <td>The service has been installed and started.</td>  
  </tr>
  <tr>
    <td>STOPPING</td>
    <td>In the process of stopping the service.</td>  
  </tr>

  <tr>
    <td>UNINSTALLING</td>
    <td>In the process of uninstalling the service.</td>  
  </tr>
  <tr>
    <td>UNINSTALLED</td>
    <td>The service has been successfully uninstalled.</td>  
  </tr>
  <tr>
    <td>WIPING_OUT</td>
    <td>In the process of wiping out the installed service.</td>  
  </tr>
  <tr>
    <td>UPGRADING</td>
    <td>In the process of upgrading the service.</td>  
  </tr>
  <tr>
    <td>MAINTENANCE</td>
    <td>The service has been marked for maintenance.</td>  
  </tr>
  <tr>
    <td>UNKNOWN</td>
    <td>The service state can not be determined.</td>  
  </tr>
</table>

### Starting a Service
A service can be started through the API by setting its state to be STARTED (see [update service](update-service.md)).

### Stopping a Service
A service can be stopped through the API by setting its state to be INSTALLED (see [update service](update-service.md)).



