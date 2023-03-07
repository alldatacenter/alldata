<!--
{% comment %}
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to you under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
{% endcomment %}
-->

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.calcite/calcite-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.apache.calcite/calcite-core)
[![CI Status](https://github.com/apache/calcite/workflows/CI/badge.svg?branch=main)](https://github.com/apache/calcite/actions?query=branch%3Amain)

# Apache Calcite

Apache Calcite is a dynamic data management framework.

It contains many of the pieces that comprise a typical
database management system but omits the storage primitives.
It provides an industry standard SQL parser and validator,
a customisable optimizer with pluggable rules and cost functions,
logical and physical algebraic operators, various transformation
algorithms from SQL to algebra (and the opposite), and many
adapters for executing SQL queries over Cassandra, Druid,
Elasticsearch, MongoDB, Kafka, and others, with minimal
configuration.

For more details, see the [home page](http://calcite.apache.org).

The project uses [JIRA](https://issues.apache.org/jira/browse/CALCITE)
for issue tracking. For further information, please see the [JIRA accounts guide](https://calcite.apache.org/develop/#jira-accounts).

## 官方项目文档
https://github.com/apache/calcite

## 本地安装部署
> 1. 配置Gradle,在USER_HOME/.gradle/下创建init.gradle文件
> 2. init.gradle文件
```markdown
allprojects{
    repositories {
        def ALIYUN_REPOSITORY_URL = 'http://maven.aliyun.com/nexus/content/groups/public'
        def ALIYUN_JCENTER_URL = 'http://maven.aliyun.com/nexus/content/repositories/jcenter'
        def GRADLE_LOCAL_RELEASE_URL = 'https://repo.gradle.org/gradle/libs-releases-local'
        def ALIYUN_SPRING_RELEASE_URL = 'https://maven.aliyun.com/repository/spring-plugin'

        all { ArtifactRepository repo ->
            if(repo instanceof MavenArtifactRepository){
                def url = repo.url.toString()
                if (url.startsWith('https://repo1.maven.org/maven2')) {
                    project.logger.lifecycle "Repository ${repo.url} replaced by $ALIYUN_REPOSITORY_URL."
                    remove repo
                }
                if (url.startsWith('https://jcenter.bintray.com/')) {
                    project.logger.lifecycle "Repository ${repo.url} replaced by $ALIYUN_JCENTER_URL."
                    remove repo
                }
                if (url.startsWith('http://repo.spring.io/plugins-release')) {
                    project.logger.lifecycle "Repository ${repo.url} replaced by $ALIYUN_SPRING_RELEASE_URL."
                    remove repo
                }

            }
        }
        maven {
		    allowInsecureProtocol = true
            url ALIYUN_REPOSITORY_URL
        }

        maven {
            allowInsecureProtocol = true
            url ALIYUN_JCENTER_URL
        }
        maven {
            allowInsecureProtocol = true
            url ALIYUN_SPRING_RELEASE_URL
        }
        maven {
            allowInsecureProtocol = true
            url GRADLE_LOCAL_RELEASE_URL
        }

    }
}
```
> 3. ./gradlew build 如果跳过测试使用./gradlew build -x test
> 4. 构建成功获取构建包
> 5. In the release/build/distributions
> apache-calcite-1.33.0-SNAPSHOT-src.tar.gz
> apache-calcite-1.33.0-SNAPSHOT-src.tar.gz.sha512
> 6. tar -zxvf apache-calcite-1.33.0-SNAPSHOT-src.tar.gz
> 7. cd apache-calcite-1.33.0-SNAPSHOT-src
> 8. cd example/csv/ && cp -r /mnt/poc/alldatadc/calcite/calcite-1.33.0/build .
> 9. 安装配置gradle7.4.2, cd /opt/gradle
> 10. wget https://services.gradle.org/distributions/gradle-7.4.2-all.zip
> 11. 解压 unzip gradle-7.4.2-all.zip
> 12. 配置环境变量：export PATH=$PATH:/opt/gradle/gradle-7.4.2/bin
```markdown
[root@16gdata apache-calcite-1.33.0-SNAPSHOT-src]# cd example/csv/
[root@16gdata csv]# ll
total 24
drwxr-xr-x 10 root root 4096 Mar  7 18:45 build
-rw-rw-r--  1 root root 3577 Jan  2  1970 build.gradle.kts
-rw-rw-r--  1 root root  876 Jan  2  1970 gradle.properties
-rwxr-xr-x  1 root root 1793 Mar  7 18:44 sqlline
-rw-rw-r--  1 root root 1537 Jan  2  1970 sqlline.bat
drwxrwxr-x  4 root root 4096 Jan  2  1970 src
```
> 13. 运行./sqlline
> 14. 进入命令行测试sqlline
```markdown
[root@16gdata csv]# !connect jdbc:calcite:model=src/test/resources/model.json admin admin
-bash: !connect: event not found
[root@16gdata csv]# ./sqlline
Building Apache Calcite 1.33.0-SNAPSHOT
sqlline version 1.12.0
sqlline> !connect jdbc:calcite:model=src/test/resources/model.json admin admin
Transaction isolation level TRANSACTION_REPEATABLE_READ is not supported. Default (TRANSACTION_NONE) will be used instead.
0: jdbc:calcite:model=src/test/resources/mode> !tables
+-----------+-------------+------------+--------------+---------+----------+------------+-----------+---------------------------+----------------+
| TABLE_CAT | TABLE_SCHEM | TABLE_NAME |  TABLE_TYPE  | REMARKS | TYPE_CAT | TYPE_SCHEM | TYPE_NAME | SELF_REFERENCING_COL_NAME | REF_GENERATION |
+-----------+-------------+------------+--------------+---------+----------+------------+-----------+---------------------------+----------------+
|           | SALES       | DEPTS      | TABLE        |         |          |            |           |                           |                |
|           | SALES       | EMPS       | TABLE        |         |          |            |           |                           |                |
|           | SALES       | SDEPTS     | TABLE        |         |          |            |           |                           |                |
|           | metadata    | COLUMNS    | SYSTEM TABLE |         |          |            |           |                           |                |
|           | metadata    | TABLES     | SYSTEM TABLE |         |          |            |           |                           |                |
+-----------+-------------+------------+--------------+---------+----------+------------+-----------+---------------------------+----------------+
0: jdbc:calcite:model=src/test/resources/mode> select * from SALES.SDEPTS;
+--------+-----------+
| DEPTNO |   NAME    |
+--------+-----------+
| 10     | Sales     |
| 20     | Marketing |
| 30     | Accounts  |
| 40     | 40        |
| 50     | 50        |
| 60     | 60        |
+--------+-----------+
6 rows selected (1.336 seconds)
0: jdbc:calcite:model=src/test/resources/mode>

```
