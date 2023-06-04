<!--

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

## Apache InLong dev toolkit

### Overview

The Apache InLong project includes modules such as "Dashboard", "DataProxy", "Manager" and "TubeMQ".
When debugging in a development environment such as `Intellij IDEA`,
The environment is difficult to build.

Take the `inlong-manager` as an example, it depends on too many configurations and packages.
When debugging locally in the IDE, multiple default directories need to be created, which is more complicated.
So a script is urgently needed to quickly support local debugging.

Also, similar tools are required for local debugging of other modules. 
Temporarily named `InLong dev toolkit`, looking forward to adding more features.

### Use case

#### Help

```shell
❯ inlong-tools/dev/inlong-dev-toolkit.sh
####################################################################################
#                 Welcome to use the Apache InLong dev toolkit!                    #
#                                          @2022-12-23 20:38:34                    #
####################################################################################


inlong-tools/dev/inlong-dev-toolkit.sh help | h
      :help

inlong-tools/dev/inlong-dev-toolkit.sh manager | m
      :build manager local debugging environment

Have a nice day, bye!

```

#### Manager

Rely of Manager:

| Directory/File                                                           | link target                                                                                                    | Rely modules                    | 
|--------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|---------------------------------|
| `./plugins`                                                              | `inlong-manager/manger-plugins/target/plugins`                                                                 | `inlong-sort/sort-plugin`       |
| `./inlong-sort/connectors`                                               | -                                                                                                              | `inlong-sort/sort-connectors/*` |                                 |
| `./inlong-sort/connectors/sort-connector-${name}-${project.version}.jar` | `inlong-sort/sort-connectors/${connector.name}/target/sort-connector-${connector.name}-${project.version}.jar` | `inlong-sort/sort-connectors/*` |
| `./sort-dist.jar`                                                        | `sort-dist/target/sort-dist-${project.version}.jar`                                                            | `inlong-sort/sort-dist`         |

```shell
❯ inlong-tools/dev/inlong-dev-toolkit.sh m
Execute action: manager
# start build manager local debugging environment ...
current_version: 1.5.0-SNAPSHOT
associate plugins directory: inlong-manager/manager-plugins/target/plugins
associate sort dist: inlong-sort/sort-dist/target/sort-dist
recreate connector dir: inlong-sort/connectors
All connector names: 
hive mysql-cdc kafka jdbc pulsar iceberg postgres-cdc mongodb-cdc sqlserver-cdc oracle-cdc elasticsearch-6 elasticsearch-7 redis tubemq filesystem doris starrocks hudi
associate connector: hive
associate connector: mysql-cdc
associate connector: kafka
associate connector: jdbc
associate connector: pulsar
associate connector: iceberg
associate connector: postgres-cdc
associate connector: mongodb-cdc
associate connector: sqlserver-cdc
associate connector: oracle-cdc
associate connector: elasticsearch-6
associate connector: elasticsearch-7
associate connector: redis
associate connector: tubemq
associate connector: filesystem
associate connector: doris
associate connector: starrocks
associate connector: hudi
build dev env of manager finished.
Have a nice day, bye!
```

### Development: add more function

#### Public variables

| inner variable     | implication                       |
|--------------------|-----------------------------------|
| `script_dir`       | the directory of this script file |
| `script_file_name` | the script file name              |
| `base_dir`         | the root directory of project     |

#### Step 1. implement function

#### Step 2. register function

Create a global array variable:

- It is recommended to end with `_action`, such as `manager_action`.
- It must have 4 elements.
- The 1st element is the long opt command.
- The 2nd element is the sort opt command.
- The 3rd element is the details of the function.
- The 4th element is the function name, which implements in Step 1.

Add the variable to the `actions` array, like:

```shell
actions=(
    help_action
    manger_action
)
```

#### Step 3. ignore the temporary file

Add temporary files to `.gitignore`

> Notice: must base on project base directory
