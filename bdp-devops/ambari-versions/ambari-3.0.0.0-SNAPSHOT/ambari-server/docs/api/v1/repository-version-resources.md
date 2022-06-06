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

# Repository Version Resources

##### Create Instance
New repository versions may be created through the API. At least one set of base repository URLs should be provided.

    POST http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/
    {
      "RepositoryVersions": {
        "repository_version": "2.2.1.1",
        "display_name": "HDP-2.2.1.1"
      },
      "operating_systems": [
        {
          "OperatingSystems": {
            "os_type": "redhat6"
          },
          "repositories": [
            {
              "Repositories": {
                "repo_id": "HDP-2.2",
                "repo_name": "HDP",
                "base_url": "http://..."
              }
            },
            {
              "Repositories": {
                "repo_id": "HDP-UTILS-1.1.0.20",
                "repo_name": "HDP-UTILS",
                "base_url": "http://..."
              }
            }
          ]
        }
      ]
    }

##### Get Repository Versions
The user may query for all repository versions of a particular stack.

    GET http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/

    {
      href: "http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/",
      items: [
        {
          href: "http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/1",
          RepositoryVersions: {
            id: 1,
            stack_name: "HDP",
            stack_version: "2.2"
          }
        },
        {
          href: "http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/2",
          RepositoryVersions: {
            id: 2,
            stack_name: "HDP",
            stack_version: "2.2"
          }
        }
      ]
    }

##### Get Repository Version
Returns single repository version.

    GET http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/1

    {
      href: "http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/1",
      RepositoryVersions: {
        display_name: "HDP-2.2.0.0-2041",
        id: 1,
        repository_version: "2.2.0.0-2041",
        stack_name: "HDP",
        stack_version: "2.2",
        upgrade_pack: "upgrade-2.2"
      },
      operating_systems: [
        {
          href: "http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/1/operating_systems/redhat5",
          OperatingSystems: {
            os_type: "redhat5",
            repository_version_id: 1,
            stack_name: "HDP",
            stack_version: "2.2"
          }
        },
        {
          href: "http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/1/operating_systems/redhat6",
          OperatingSystems: {
            os_type: "redhat6",
            repository_version_id: 1,
            stack_name: "HDP",
            stack_version: "2.2"
          }
        }
      ]
    }
    
##### Delete Repository Version
Deregisters repository version. It won't be possible to remove repository version which is installed on any of the clusters.

    DELETE http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/1

##### Update Repository Version
Updates repository version. It is possible to change display name and base URLs. 

    PUT http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/1
    
    {
      "RepositoryVersions": {
        "display_name": "HDP-2.2.1.1",
        "id": 1,
        "repository_version": "2.2.1.1"
      },
      "operating_systems": [
        {
          "href": "http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/1/operating_systems/redhat5",
          "OperatingSystems": {
            "os_type": "redhat5"
          },
          "repositories": [
            {
              "href": "http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/1/operating_systems/redhat5/repositories/HDP-2.2",
              "Repositories": {
                "base_url": "http://...",
                "os_type": "redhat5",
                "repo_id": "HDP-2.2",
                "repo_name": "HDP"
              }
            },
            {
              "href": "http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/repository_versions/1/operating_systems/redhat5/repositories/HDP-UTILS-1.1.0.20",
              "Repositories": {
                "base_url": "http://...",
                "os_type": "redhat5",
                "repo_id": "HDP-UTILS-1.1.0.20",
                "repo_name": "HDP-UTILS"
              }
            }
          ]
        }
      ]
    }
    
##### Validate Base URL
User may validate URLs of repositories before persisting.

    POST http://<server>:8080/api/v1/stacks/<stack_name>/versions/<stack_version>/operating_systems/redhat5/repositories/HDP-UTILS-1.1.0.20?validate_only=true

    {
      "Repositories": {
        "base_url": "http://..."
      }
    }
    
