<!---
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Performance and Scalability Analyzer for Apache Ranger

## Primary Config Description
1) List of apis to be tested for performance and scalability. Currently only below API's supported.

`"api_list": [
        "create_policy",
        "update_policy_by_id",
        "get_policy_by_id",
        "delete_policy_by_id"
    ]`

2) SSH username and password for the machine where Ranger is running: 
`user` and `password`

3) `client_ip`: for access log parsing
4) Config for system metrics collection. Runs `vmstat` command in the backend machine and parses the output.
`"system_logger": {
        "enabled": true,
        "align_with_access_logs": false,
        "metrics": [
            "r",
            "free",
            "id",
            "UTC"
        ],
        "sleep_seconds": 5,
        "num_calls": "inf",
        "remote_log_file_location": "xxxxxxxx/system_logs.txt",
        "secondary_log_file_location": "xxxxxxxxx/secondary_system_logs.txt"
    }`

```enabled:false``` may break the currently implemented system

```align_with_access_logs:true``` aligns the access logs and system logs by timestamp

```sleep_seconds:5``` sleep time between system metrics collection

```num_calls:inf``` number of system metrics collection. inf means infinite collection till the end of the performance_analyzer.py script execution (if the script crashes, then manual killing of vmstat may be required).

```remote_log_file_location: xxxxxxx/system_logs.txt``` location of system logs. default ok for most cases.

```secondary_log_file_location: xxxxxxxxxx/secondary_system_logs.txt``` location of secondary system logs. default ok for most cases.

```metrics: "r","free","id","UTC"]```metrics to be collected. default ok for most cases. Full list of supported metrics: keys of dict header_mapping_system_logs in  ```ranger_performance_tool/ranger_perf_utils/logging_utils.py```

5) API's currently supported. Change num_calls and sleep_seconds as per the load and benchmarking needs.
```     
"api": {
        "create_policy": {
            "num_calls": 3,
            "sleep_seconds": 1
        },
        "delete_policy_by_id": {
            "num_calls": 3,
            "sleep_seconds": 1
        },
        "get_policy_by_id": {
            "num_calls": 3,
            "sleep_seconds": 1
        },
        "update_policy_by_id": {
            "num_calls": 3,
            "sleep_seconds": 1
        }
    } 
```

## Secondary Config Description
1) "random_type" is a random number generator type for differeny generators implemented in ```ranger_performance_tool/ranger_perf_object_stores/random_generators.py```.
Currently implemented and supported "random_type" is one of `["random", "increasing", "incremental"]`.
Check ```ranger_performance_tool/ranger_perf_object_stores/random_generators.py``` for more details or add new custom generators.

2) "enabled_services" is a list of services to be enabled for the tests.
3) "service_type_mapping" is a mapping of service name to service type.