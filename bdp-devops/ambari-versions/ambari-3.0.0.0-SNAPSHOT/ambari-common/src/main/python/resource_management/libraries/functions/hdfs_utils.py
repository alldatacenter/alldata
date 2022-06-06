#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Ambari Agent

"""
from resource_management.libraries.functions.is_empty import is_empty

"""
Check both dfs.http.policy and deprecated dfs.https.enable
"""
def is_https_enabled_in_hdfs(dfs_http_policy, dfs_https_enable):
    https_enabled = False
    if not is_empty(dfs_http_policy):
        https_enabled = dfs_http_policy.lower() == "https_only"
    elif not is_empty(dfs_https_enable):
        https_enabled = dfs_https_enable
    return https_enabled