#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Creates an idempotent SQL script for AzureDB from the SQLServer "create" script.

sql_dir="$1"/src/main/resources
script_dir="$1"/src/main/python

[[ -e "$sql_dir"/Ambari-DDL-SQLServer-CREATE.sql ]] || exit 1
[[ -x "$script_dir"/azuredb_create_generator.py ]] || exit 2

cat "$sql_dir"/Ambari-DDL-SQLServer-CREATE.sql | "$script_dir"/azuredb_create_generator.py > "$sql_dir"/Ambari-DDL-AzureDB-CREATE.sql
