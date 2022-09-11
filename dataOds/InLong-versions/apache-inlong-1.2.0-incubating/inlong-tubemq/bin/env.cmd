@rem
@rem Licensed to the Apache Software Foundation (ASF) under one
@rem or more contributor license agreements.  See the NOTICE file
@rem distributed with this work for additional information
@rem regarding copyright ownership.  The ASF licenses this file
@rem to you under the Apache License, Version 2.0 (the
@rem "License"); you may not use this file except in compliance
@rem with the License.  You may obtain a copy of the License at
@rem
@rem   http://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing,
@rem software distributed under the License is distributed on an
@rem "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@rem KIND, either express or implied.  See the License for the
@rem specific language governing permissions and limitations
@rem under the License.
@rem

REM Windows Startup Script about Environment Settings
REM Java runtime evironment could be specified here.

set BASE_DIR=%~dp0..
set CLASSPATH=%BASE_DIR%\lib\*;%BASE_DIR%\tubemq-server\target\*;%CLASSPATH%
set GENERIC_ARGS="-Dtubemq.home=%BASE_DIR%" -cp "%CLASSPATH%" "-Dtubemq.log.path=%BASE_DIR%\logs" "-Dlog4j.configurationFile=%BASE_DIR%\conf\log4j2.xml"

REM If there's no system-wide JAVA_HOME or there's need to run on specific Java,
REM please uncomment the following JAVA_HOME line, and specify the java home path.
REM set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_241

set JAVA="%JAVA_HOME%\bin\java"

REM One may add extra Java runtime flags in addition to each role: Master or Broker
set MASTER_JVM_OPTS=-Xmx1g -Xms256m -server "-Dtubemq.log.prefix=master"
set BROKER_JVM_OPTS=-Xmx1g -Xms512m -server "-Dtubemq.log.prefix=broker"