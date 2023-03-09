/*
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
*/

INSERT INTO `QRTZ_FIRED_TRIGGERS` VALUES ('schedulerFactoryBean','LM-SHC-0095021215027875413551502787541380','measure-test-BA-test-1502789449000','BA','LM-SHC-009502121502787541355',1502789505307,1502789520000,5,'ACQUIRED',NULL,NULL,'0','0');

INSERT INTO `QRTZ_JOB_DETAILS` VALUES ('schedulerFactoryBean','measure-test-BA-test-1502789449000','BA',NULL,'org.apache.griffin.core.job.SparkSubmitJob','1','1','1','0','#\n#Tue Aug 15 17:31:20 CST 2017\ninterval=40\njobStartTime=1502640000000\nsourcePattern=YYYYMMdd-HH\nmeasureName=measure-test\njobName=measure-test-BA-test-1502789449000\nblockStartTimestamp=\nlastBlockStartTimestamp=1502789480023\ntargetPattern=YYYYMMdd-HH\ngroupName=BA\n');


INSERT INTO `QRTZ_LOCKS` VALUES ('schedulerFactoryBean','STATE_ACCESS'),('schedulerFactoryBean','TRIGGER_ACCESS');


INSERT INTO `QRTZ_SCHEDULER_STATE` VALUES ('schedulerFactoryBean','LM-SHC-009502121502787541355',1502789504423,20000);

INSERT INTO `QRTZ_SIMPLE_TRIGGERS` VALUES ('schedulerFactoryBean','measure-test-BA-test-1502789449000','BA',-1,40000,1);


INSERT INTO `QRTZ_TRIGGERS` VALUES ('schedulerFactoryBean','measure-test-BA-test-1502789449000','BA','measure-test-BA-test-1502789449000','BA',NULL,1502789520000,1502789480000,5,'ACQUIRED','SIMPLE',1502789480000,0,NULL,0,'');


INSERT INTO `data_connector` VALUES (9,'2017-08-15 17:30:32',NULL,'{\"database\":\"default\",\"table.name\":\"demo_src\"}','HIVE','1.2'),(10,'2017-08-15 17:30:32',NULL,'{\"database\":\"default\",\"table.name\":\"demo_tgt\"}','HIVE','1.2');


INSERT INTO `evaluate_rule` VALUES (5,'2017-08-15 17:30:32',NULL,'$source[''id''] == $target[''id''] AND $source[''age''] == $target[''age''] AND $source[''desc''] == $target[''desc'']',0);


INSERT INTO `job_instance` VALUES (103,'2017-08-15 17:31:21',NULL,'application_1500359928600_0025','BA','measure-test-BA-test-1502789449000',24,'success',1502789480660,'http://10.149.247.156:28088/cluster/app/application_1500359928600_0025');


INSERT INTO `measure` VALUES (5,'2017-08-15 17:30:32',NULL,'Description of measure-test','measure-test','eBay','test','accuracy',5,9,10);
