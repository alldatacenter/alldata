#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

echo_stderr ()
{
    echo "$@" >&2
}

if [ $# -ne 2 ]
then
	echo_stderr "usage: $0 <service_name> <num_of_resource_policies>"
fi

service_name=cm_hive
num_of_resource_policies=1

if [ $# -eq 1 ]
then
	service_name=$1
		echo_stderr "service_name=${service_name}, num_of_resource_policies=${num_of_resource_policies}"
fi
if [ $# -eq 2 ]
	then
		num_of_resource_policies=$2
	  echo_stderr "service_name=${service_name}, num_of_resource_policies=${num_of_resource_policies}"
	else
	  echo_stderr "Assuming service_name=${service_name}, num_of_resource_policies=${num_of_resource_policies}"
fi

echo "{
  \"serviceName\": \"${service_name}\",
  \"serviceId\": 2,
  \"policies\": [
  "
for ((i = 1; i <= $num_of_resource_policies; i++)); do
    if [ $i -ne 1 ]
    then
         echo "  ,"
    fi

    echo " {
            \"name\": \"${service_name}-${i}\",
            \"id\": ${i},
            \"isEnabled\": true,
            \"isAuditEnabled\": true,
            \"resources\": {
                    \"database\": { \"values\": [ \"finance_${i}\" ], \"isExcludes\": false, \"isRecursive\": false },
                    \"table\": { \"values\": [ \"tax_2020_${i}\" ], \"isExcludes\": false, \"isRecursive\": false },
                    \"column\": { \"values\": [ \"*\" ], \"isExcludes\": false, \"isRecursive\": false }
            },
            \"policyItems\": [],
            \"denyPolicyItems\": [
                { \"accesses\": [ { \"type\": \"all\", \"isAllowed\": true } ], \"users\": [ \"hrt_1\" ] }
            ]
          }"
done
echo "  ],"
echo "
    \"serviceDef\": {
    \"name\": \"hive\",
    \"implClass\": \"org.apache.ranger.services.hive.RangerServiceHive\",
    \"label\": \"Hive Server2\",
    \"options\": {},
    \"configs\": [
      {
        \"itemId\": 1,
        \"name\": \"username\",
        \"type\": \"string\",
        \"mandatory\": true,
        \"validationRegEx\": \"\",
        \"validationMessage\": \"\",
        \"uiHint\": \"\",
        \"label\": \"Username\"
      },
      {
        \"itemId\": 2,
        \"name\": \"password\",
        \"type\": \"password\",
        \"mandatory\": true,
        \"validationRegEx\": \"\",
        \"validationMessage\": \"\",
        \"uiHint\": \"\",
        \"label\": \"Password\"
      },
      {
        \"itemId\": 3,
        \"name\": \"jdbc.driverClassName\",
        \"type\": \"string\",
        \"mandatory\": true,
        \"defaultValue\": \"org.apache.hive.jdbc.HiveDriver\",
        \"validationRegEx\": \"\",
        \"validationMessage\": \"\",
        \"uiHint\": \"\"
      },
      {
        \"itemId\": 4,
        \"name\": \"jdbc.url\",
        \"type\": \"string\",
        \"mandatory\": true,
        \"defaultValue\": \"\",
        \"validationRegEx\": \"\",
        \"validationMessage\": \"\",
        \"uiHint\": \"\"
      },
      {
        \"itemId\": 5,
        \"name\": \"commonNameForCertificate\",
        \"type\": \"string\",
        \"mandatory\": false,
        \"validationRegEx\": \"\",
        \"validationMessage\": \"\",
        \"uiHint\": \"\",
        \"label\": \"Common Name for Certificate\"
      }
    ],
    \"resources\": [
      {
        \"itemId\": 1,
        \"name\": \"database\",
        \"type\": \"string\",
        \"level\": 10,
        \"mandatory\": true,
        \"lookupSupported\": true,
        \"recursiveSupported\": false,
        \"excludesSupported\": true,
        \"matcher\": \"org.apache.ranger.plugin.resourcematcher.RangerDefaultResourceMatcher\",
        \"matcherOptions\": {
          \"wildCard\": \"true\",
          \"ignoreCase\": \"true\"
        },
        \"validationRegEx\": \"\",
        \"validationMessage\": \"\",
        \"uiHint\": \"\",
        \"label\": \"Hive Database\"
      },
      {
        \"itemId\": 2,
        \"name\": \"table\",
        \"type\": \"string\",
        \"level\": 20,
        \"parent\": \"database\",
        \"mandatory\": true,
        \"lookupSupported\": true,
        \"recursiveSupported\": false,
        \"excludesSupported\": true,
        \"matcher\": \"org.apache.ranger.plugin.resourcematcher.RangerDefaultResourceMatcher\",
        \"matcherOptions\": {
          \"wildCard\": \"true\",
          \"ignoreCase\": \"true\"
        },
        \"validationRegEx\": \"\",
        \"validationMessage\": \"\",
        \"uiHint\": \"\",
        \"label\": \"Hive Table\"
      },
      {
        \"itemId\": 3,
        \"name\": \"udf\",
        \"type\": \"string\",
        \"level\": 20,
        \"parent\": \"database\",
        \"mandatory\": true,
        \"lookupSupported\": true,
        \"recursiveSupported\": false,
        \"excludesSupported\": true,
        \"matcher\": \"org.apache.ranger.plugin.resourcematcher.RangerDefaultResourceMatcher\",
        \"matcherOptions\": {
          \"wildCard\": \"true\",
          \"ignoreCase\": \"true\"
        },
        \"validationRegEx\": \"\",
        \"validationMessage\": \"\",
        \"uiHint\": \"\",
        \"label\": \"Hive UDF\"
      },
      {
        \"itemId\": 4,
        \"name\": \"column\",
        \"type\": \"string\",
        \"level\": 30,
        \"parent\": \"table\",
        \"mandatory\": true,
        \"lookupSupported\": true,
        \"recursiveSupported\": false,
        \"excludesSupported\": true,
        \"matcher\": \"org.apache.ranger.plugin.resourcematcher.RangerDefaultResourceMatcher\",
        \"matcherOptions\": {
          \"wildCard\": \"true\",
          \"ignoreCase\": \"true\"
        },
        \"validationRegEx\": \"\",
        \"validationMessage\": \"\",
        \"uiHint\": \"\",
        \"label\": \"Hive Column\"
      }
    ],
    \"accessTypes\": [
      {
        \"itemId\": 1,
        \"name\": \"select\",
        \"label\": \"select\",
        \"impliedGrants\": []
      },
      {
        \"itemId\": 2,
        \"name\": \"update\",
        \"label\": \"update\",
        \"impliedGrants\": []
      },
      {
        \"itemId\": 3,
        \"name\": \"create\",
        \"label\": \"Create\",
        \"impliedGrants\": []
      },
      {
        \"itemId\": 4,
        \"name\": \"drop\",
        \"label\": \"Drop\",
        \"impliedGrants\": []
      },
      {
        \"itemId\": 5,
        \"name\": \"alter\",
        \"label\": \"Alter\",
        \"impliedGrants\": []
      },
      {
        \"itemId\": 6,
        \"name\": \"index\",
        \"label\": \"Index\",
        \"impliedGrants\": []
      },
      {
        \"itemId\": 7,
        \"name\": \"lock\",
        \"label\": \"Lock\",
        \"impliedGrants\": []
      },
      {
        \"itemId\": 8,
        \"name\": \"all\",
        \"label\": \"All\",
        \"impliedGrants\": [
          \"select\",
          \"update\",
          \"create\",
          \"drop\",
          \"alter\",
          \"index\",
          \"lock\"
        ]
      }
    ],
    \"policyConditions\": [
      {
        \"itemId\": 1,
        \"name\": \"resources-accessed-together\",
        \"evaluator\": \"org.apache.ranger.plugin.conditionevaluator.RangerHiveResourcesAccessedTogetherCondition\",
        \"evaluatorOptions\": {},
        \"label\": \"Hive Resources Accessed Together?\"
      }
    ],
    \"contextEnrichers\": [],
    \"enums\": [],
    \"id\": 3,
    \"isEnabled\": true
  },
  \"tagPolicies\": {
    \"serviceName\": \"tagdev\",
    \"serviceId\": 3,
    \"policyVersion\": 1,
    \"policies\": [
      {
        \"service\": \"tagdev\",
        \"name\": \"tagdev-EXPIRES_ON\",
        \"isAuditEnabled\": true,
        \"resources\": {
          \"tag\": {
            \"values\": [
              \"EXPIRES_ON\"
            ],
            \"isExcludes\": false,
            \"isRecursive\": false
          }
        },
        \"policyItems\": [],
        \"denyPolicyItems\": [
          {
            \"accesses\": [
              {
                \"type\": \"hive:select\",
                \"isAllowed\": true
              },
              {
                \"type\": \"hive:update\",
                \"isAllowed\": true
              },
              {
                \"type\": \"hive:create\",
                \"isAllowed\": true
              },
              {
                \"type\": \"hive:drop\",
                \"isAllowed\": true
              },
              {
                \"type\": \"hive:alter\",
                \"isAllowed\": true
              },
              {
                \"type\": \"hive:index\",
                \"isAllowed\": true
              },
              {
                \"type\": \"hive:lock\",
                \"isAllowed\": true
              },
              {
                \"type\": \"hive:all\",
                \"isAllowed\": true
              }
            ],
            \"users\": [],
            \"groups\": [
              \"public\"
            ],
            \"conditions\": [
              {
                \"type\": \"accessed-after-expiry\",
                \"values\": [
                  \"yes\"
                ]
              }
            ],
            \"isEnabled\": true
          }
        ],
        \"allowExceptions\": [],
        \"denyExceptions\": [],
        \"id\": 4,
        \"isEnabled\": true
      }
    ],
    \"serviceDef\": {
      \"name\": \"tag\",
      \"implClass\": \"org.apache.ranger.services.tag.RangerServiceTag\",
      \"label\": \"TAG\",
      \"options\": {
        \"ui.pages\": \"tag-based-policies\"
      },
      \"configs\": [],
      \"resources\": [
        {
          \"itemId\": 1,
          \"name\": \"tag\",
          \"type\": \"string\",
          \"level\": 1,
          \"mandatory\": true,
          \"lookupSupported\": true,
          \"recursiveSupported\": false,
          \"excludesSupported\": false,
          \"matcher\": \"org.apache.ranger.plugin.resourcematcher.RangerDefaultResourceMatcher\",
          \"matcherOptions\": {
            \"wildCard\": \"false\",
            \"ignoreCase\": \"false\"
          },
          \"validationRegEx\": \"\",
          \"validationMessage\": \"\",
          \"uiHint\": \"{ \\\"singleValue\\\":true }\",
          \"label\": \"TAG\"
        }
      ],
      \"accessTypes\": [
        {
          \"itemId\": 3004,
          \"name\": \"hive:select\",
          \"label\": \"select\",
          \"impliedGrants\": []
        },
        {
          \"itemId\": 3005,
          \"name\": \"hive:update\",
          \"label\": \"update\",
          \"impliedGrants\": []
        },
        {
          \"itemId\": 3006,
          \"name\": \"hive:create\",
          \"label\": \"Create\",
          \"impliedGrants\": []
        },
        {
          \"itemId\": 3007,
          \"name\": \"hive:drop\",
          \"label\": \"Drop\",
          \"impliedGrants\": []
        },
        {
          \"itemId\": 3008,
          \"name\": \"hive:alter\",
          \"label\": \"Alter\",
          \"impliedGrants\": []
        },
        {
          \"itemId\": 3009,
          \"name\": \"hive:index\",
          \"label\": \"Index\",
          \"impliedGrants\": []
        },
        {
          \"itemId\": 3010,
          \"name\": \"hive:lock\",
          \"label\": \"Lock\",
          \"impliedGrants\": []
        },
        {
          \"itemId\": 3011,
          \"name\": \"hive:all\",
          \"label\": \"All\",
          \"impliedGrants\": [
            \"hive:select\",
            \"hive:update\",
            \"hive:create\",
            \"hive:drop\",
            \"hive:alter\",
            \"hive:index\",
            \"hive:lock\"
          ]
        }
      ],
      \"policyConditions\": [
        {
          \"itemId\": 1,
          \"name\": \"accessed-after-expiry\",
          \"evaluator\": \"org.apache.ranger.plugin.conditionevaluator.RangerScriptTemplateConditionEvaluator\",
          \"evaluatorOptions\": {
            \"scriptTemplate\": \"ctx.isAccessedAfter(\u0027expiry_date\u0027);\"
          },
          \"uiHint\": \"{ \\\"singleValue\\\":true }\",
          \"label\": \"Accessed after expiry_date (yes/no)?\"
        }
      ],
      \"contextEnrichers\": [
        {
          \"itemId\": 1,
          \"name\": \"TagEnricher\",
          \"enricher\": \"org.apache.ranger.plugin.contextenricher.RangerTagEnricher\",
          \"enricherOptions\": {
            \"tagRetrieverClassName\": \"org.apache.ranger.plugin.contextenricher.RangerFileBasedTagRetriever\",
            \"tagRefresherPollingInterval\": \"60000\",
            \"serviceTagsFileName\":\"/testdata/test_servicetags_hive.json\"
          }
        }
      ],
      \"enums\": [],
      \"id\": 100,
      \"isEnabled\": true
    }
  }
}"

