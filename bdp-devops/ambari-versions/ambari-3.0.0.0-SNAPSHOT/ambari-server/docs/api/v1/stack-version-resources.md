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

# Stack Version Resources

##### Get Stack Versions
Retrieves all stack versions.

For cluster:

    GET http://<server>:8080/api/v1/clusters/<cluster_name>/stack_versions/
    
    {
      href: "http://<server>:8080/api/v1/clusters/<cluster_name>/stack_versions",
      items: [
        {
          href: "http://<server>:8080/api/v1/clusters/<cluster_name>/stack_versions/1",
          ClusterStackVersions: {
            cluster_name: "<cluster_name>",
            id: 1,
            repository_version: 1,
            stack: "HDP",
            version: "2.2"
          }
        }
      ]
    }
    

For host:

    GET http://<server>:8080/api/v1/hosts/<host_name>/stack_versions
    {
      href: "http://<server>:8080/api/v1/hosts/<host_name>/stack_versions",
      items: [
        {
          href: "http://<server>:8080/api/v1/hosts/<host_name>/stack_versions/1",
          HostStackVersions: {
            host_name: "<host_name>",
            id: 1,
            repository_version: 1,
            stack: "HDP",
            version: "2.2"
          }
        }
      ]
    }
    
##### Get Stack Version
Retrieves single stack version.

For cluster:
  
      GET http://<server>:8080/api/v1/clusters/<cluster_name>/stack_versions/1
      
      {
      href: "http://<server>:8080/api/v1/clusters/<cluster_name>/stack_versions/1",
      ClusterStackVersions: {
        cluster_name: "<cluster_name>",
        id: 1,
        repository_version: 1,
        stack: "HDP",
        state: "CURRENT",
        version: "2.2",
        host_states: {
          CURRENT: [
            "<host_name>"
          ],
          INSTALLED: [
            
          ],
          INSTALLING: [
            
          ],
          INSTALL_FAILED: [
            
          ],
          OUT_OF_SYNC: [
            
          ],
          UPGRADED: [
            
          ],
          UPGRADE_FAILED: [
            
          ],
          UPGRADING: [
            
          ]
        }
      },
      repository_versions: [
        {
          href: "http://<server>:8080/api/v1/clusters/<cluster_name>/stack_versions/1/repository_versions/1",
          RepositoryVersions: {
            id: 1,
            stack_name: "HDP",
            stack_version: "2.2"
          }
        }
      ]
    }
    
For host:

    GET http://<server>:8080/api/v1/hosts/<host_name>/stack_versions/1
    
    {
      href: "http://<server>:8080/api/v1/hosts/<host_name>/stack_versions/1",
      HostStackVersions: {
        host_name: "<host_name>",
        id: 1,
        repository_version: 1,
        stack: "HDP",
        state: "CURRENT",
        version: "2.2"
      },
      repository_versions: [
        {
          href: "http://<server>:8080/api/v1/hosts/<host_name>/stack_versions/1/repository_versions/1",
          RepositoryVersions: {
            id: 1,
            stack_name: "HDP",
            stack_version: "2.2"
          }
        }
      ]
    }
    
##### Install Repository Version
Performs install of given repository version on the resource.

For cluster:

    POST http://<server>:8080/api/v1/clusters/<cluster_name>/stack_versions/
    {
      "ClusterStackVersions": {
        "stack": "HDP",
        "version": "2.2",
        "repository_version": "2.2.0.1-885"
      }
    }

For host:

    POST http://<server>:8080/api/v1/hosts/<host_name>/stack_versions/
    
    {
      "HostStackVersions": {
        "stack": "HDP",
        "version": "2.2",
        "repository_version": "2.2.0.1-885"
      }
    }
