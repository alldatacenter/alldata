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

# Rolling Upgrade Check Resources

##### Get Check Results
Retrieves results of rolling upgrade checks. All checks should have status PASS before running rolling upgrade.

    GET http://<server>:8080/api/v1/clusters/<cluster_name>/rolling_upgrades_check/
    
    {
      href: "http://<server>:8080/api/v1/clusters/<cluster_name>/rolling_upgrades_check/",
      items: [
        {
          href: "http://<server>:8080/api/v1/clusters/<cluster_name>/rolling_upgrades_check/HOSTS_HEARTBEAT",
          UpgradeChecks: {
            cluster_name: "<cluster_name>",
            id: "HOSTS_HEARTBEAT"
          }
        },
        {
          href: "http://<server>:8080/api/v1/clusters/<cluster_name>/rolling_upgrades_check/SECONDARY_NAMENODE_MUST_BE_DELETED",
          UpgradeChecks: {
            cluster_name: "<cluster_name>",
            id: "SECONDARY_NAMENODE_MUST_BE_DELETED"
          }
        }
      ]
    }

##### Example of checks execution
Successful and unsuccessful check.

    {
      href: "http://<server>:8080/api/v1/clusters/<cluster_name>/rolling_upgrades_check/?fields=*",
      items: [
        {
          href: "http://<server>:8080/api/v1/clusters/<cluster_name>/rolling_upgrades_check/HOSTS_HEARTBEAT",
          UpgradeChecks: {
            check: "All hosts must be heartbeating with the server unless they are in Maintenance Mode",
            check_type: "HOST",
            cluster_name: "1",
            failed_on: [
              
            ],
            id: "HOSTS_HEARTBEAT",
            reason: "",
            status: "PASS"
          }
        },
        {
          href: "http://<server>:8080/api/v1/clusters/<cluster_name>/rolling_upgrades_check/SECONDARY_NAMENODE_MUST_BE_DELETED",
          UpgradeChecks: {
            check: "The SECONDARY_NAMENODE component must be deleted from all hosts",
            check_type: "SERVICE",
            cluster_name: "<cluster_name>",
            failed_on: [
              "SECONDARY_NAMENODE"
            ],
            id: "SECONDARY_NAMENODE_MUST_BE_DELETED",
            reason: "The SECONDARY_NAMENODE component must be deleted from host(s): c6401.ambari.apache.org. Please use the REST API to delete it.",
            status: "FAIL"
          }
        }
      ]
    }
