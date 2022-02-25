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

# Component Resources
Component resources are the individual components of a service (e.g. HDFS/NameNode and MapReduce/JobTracker).  Components are sub-resources of services.

### API Summary

- [List service components](components.md)
- [View component information](components-component.md)
- [Create component](create-component.md)



### Properties

<table>
  <tr>
    <th>Property</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>ServiceComponentInfo/service_name</td>
    <td>The name of the parent service</td>  
  </tr>
  <tr>
    <td>ServiceComponentInfo/component_name</td>
    <td>The component name</td>  
  </tr>
  <tr>
    <td>ServiceComponentInfo/cluster_name</td>
    <td>The name of the parent cluster</td>  
  </tr>
  <tr>
    <td>ServiceComponentInfo/description</td>
    <td>The component description</td>  
  </tr>
  <tr>
    <td>ServiceComponentInfo/desired_configs</td>
    <td>The desired configurations</td>  
  </tr>
</table>

