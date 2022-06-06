/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
  'use strict';

  function getJobs(){
	return jobs;  
  };
  
  function getJobById(id){
	 /* jobs.find(function(job){
		 return job.id == id;
	  });*/
	  return jobDetail;
  }
 
  var jobs = {
   "total":1127,
   "workflows":[
      {
         "appPath":null,
         "acl":null,
         "status":"FAILED",
         "createdTime":"Mon, 18 Jul 2016 17:18:19 GMT",
         "conf":null,
         "lastModTime":"Mon, 18 Jul 2016 17:18:27 GMT",
         "run":0,
         "endTime":"Mon, 18 Jul 2016 17:18:27 GMT",
         "externalId":"0001250-160321130408525-oozie-oozi-W@user-action@0",
         "appName":"falcon-dr-hive-workflow",
         "id":"0001251-160321130408525-oozie-oozi-W",
         "startTime":"Mon, 18 Jul 2016 17:18:19 GMT",
         "parentId":"0001250-160321130408525-oozie-oozi-W",
         "toString":"Workflow id[0001251-160321130408525-oozie-oozi-W] status[FAILED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001251-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      },
      {
         "appPath":null,
         "acl":null,
         "status":"KILLED",
         "createdTime":"Mon, 18 Jul 2016 17:18:06 GMT",
         "conf":null,
         "lastModTime":"Tue, 19 Jul 2016 17:18:17 GMT",
         "run":0,
         "endTime":"Tue, 19 Jul 2016 17:18:17 GMT",
         "externalId":"hiveMirror11463038709928\/DEFAULT\/2016-05-19T16:02Z",
         "appName":"FALCON_PROCESS_DEFAULT_hiveMirror11463038709928",
         "id":"0001250-160321130408525-oozie-oozi-W",
         "startTime":"Mon, 18 Jul 2016 17:18:07 GMT",
         "parentId":"0000735-160321130408525-oozie-oozi-C@2077",
         "toString":"Workflow id[0001250-160321130408525-oozie-oozi-W] status[KILLED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001250-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      },
      {
         "appPath":null,
         "acl":null,
         "status":"FAILED",
         "createdTime":"Thu, 19 May 2016 16:01:47 GMT",
         "conf":null,
         "lastModTime":"Thu, 19 May 2016 16:01:50 GMT",
         "run":0,
         "endTime":"Thu, 19 May 2016 16:01:50 GMT",
         "externalId":"0001248-160321130408525-oozie-oozi-W@user-action@0",
         "appName":"falcon-dr-hive-workflow",
         "id":"0001249-160321130408525-oozie-oozi-W",
         "startTime":"Thu, 19 May 2016 16:01:47 GMT",
         "parentId":"0001248-160321130408525-oozie-oozi-W",
         "toString":"Workflow id[0001249-160321130408525-oozie-oozi-W] status[FAILED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001249-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      },
      {
         "appPath":null,
         "acl":null,
         "status":"KILLED",
         "createdTime":"Thu, 19 May 2016 16:01:43 GMT",
         "conf":null,
         "lastModTime":"Tue, 19 Jul 2016 17:18:16 GMT",
         "run":0,
         "endTime":"Tue, 19 Jul 2016 17:18:15 GMT",
         "externalId":"hiveMirror11463038709928\/DEFAULT\/2016-05-19T15:57Z",
         "appName":"FALCON_PROCESS_DEFAULT_hiveMirror11463038709928",
         "id":"0001248-160321130408525-oozie-oozi-W",
         "startTime":"Thu, 19 May 2016 16:01:43 GMT",
         "parentId":"0000735-160321130408525-oozie-oozi-C@2076",
         "toString":"Workflow id[0001248-160321130408525-oozie-oozi-W] status[KILLED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001248-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      },
      {
         "appPath":null,
         "acl":null,
         "status":"FAILED",
         "createdTime":"Thu, 19 May 2016 15:56:47 GMT",
         "conf":null,
         "lastModTime":"Thu, 19 May 2016 15:56:48 GMT",
         "run":0,
         "endTime":"Thu, 19 May 2016 15:56:48 GMT",
         "externalId":"0001246-160321130408525-oozie-oozi-W@user-action@0",
         "appName":"falcon-dr-hive-workflow",
         "id":"0001247-160321130408525-oozie-oozi-W",
         "startTime":"Thu, 19 May 2016 15:56:47 GMT",
         "parentId":"0001246-160321130408525-oozie-oozi-W",
         "toString":"Workflow id[0001247-160321130408525-oozie-oozi-W] status[FAILED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001247-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      },
      {
         "appPath":null,
         "acl":null,
         "status":"KILLED",
         "createdTime":"Thu, 19 May 2016 15:56:45 GMT",
         "conf":null,
         "lastModTime":"Tue, 19 Jul 2016 17:18:15 GMT",
         "run":0,
         "endTime":"Tue, 19 Jul 2016 17:18:15 GMT",
         "externalId":"hiveMirror11463038709928\/DEFAULT\/2016-05-19T15:52Z",
         "appName":"FALCON_PROCESS_DEFAULT_hiveMirror11463038709928",
         "id":"0001246-160321130408525-oozie-oozi-W",
         "startTime":"Thu, 19 May 2016 15:56:45 GMT",
         "parentId":"0000735-160321130408525-oozie-oozi-C@2075",
         "toString":"Workflow id[0001246-160321130408525-oozie-oozi-W] status[KILLED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001246-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      },
      {
         "appPath":null,
         "acl":null,
         "status":"FAILED",
         "createdTime":"Thu, 19 May 2016 13:12:02 GMT",
         "conf":null,
         "lastModTime":"Thu, 19 May 2016 13:12:03 GMT",
         "run":0,
         "endTime":"Thu, 19 May 2016 13:12:03 GMT",
         "externalId":"0001244-160321130408525-oozie-oozi-W@user-action@0",
         "appName":"falcon-dr-hive-workflow",
         "id":"0001245-160321130408525-oozie-oozi-W",
         "startTime":"Thu, 19 May 2016 13:12:02 GMT",
         "parentId":"0001244-160321130408525-oozie-oozi-W",
         "toString":"Workflow id[0001245-160321130408525-oozie-oozi-W] status[FAILED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001245-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      },
      {
         "appPath":null,
         "acl":null,
         "status":"KILLED",
         "createdTime":"Thu, 19 May 2016 13:12:00 GMT",
         "conf":null,
         "lastModTime":"Tue, 19 Jul 2016 17:18:17 GMT",
         "run":0,
         "endTime":"Tue, 19 Jul 2016 17:18:17 GMT",
         "externalId":"hiveMirror11463038709928\/DEFAULT\/2016-05-19T13:12Z",
         "appName":"FALCON_PROCESS_DEFAULT_hiveMirror11463038709928",
         "id":"0001244-160321130408525-oozie-oozi-W",
         "startTime":"Thu, 19 May 2016 13:12:01 GMT",
         "parentId":"0000735-160321130408525-oozie-oozi-C@2043",
         "toString":"Workflow id[0001244-160321130408525-oozie-oozi-W] status[KILLED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001244-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      },
      {
         "appPath":null,
         "acl":null,
         "status":"FAILED",
         "createdTime":"Thu, 19 May 2016 13:07:02 GMT",
         "conf":null,
         "lastModTime":"Thu, 19 May 2016 13:07:03 GMT",
         "run":0,
         "endTime":"Thu, 19 May 2016 13:07:03 GMT",
         "externalId":"0001242-160321130408525-oozie-oozi-W@user-action@0",
         "appName":"falcon-dr-hive-workflow",
         "id":"0001243-160321130408525-oozie-oozi-W",
         "startTime":"Thu, 19 May 2016 13:07:02 GMT",
         "parentId":"0001242-160321130408525-oozie-oozi-W",
         "toString":"Workflow id[0001243-160321130408525-oozie-oozi-W] status[FAILED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001243-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      },
      {
         "appPath":null,
         "acl":null,
         "status":"KILLED",
         "createdTime":"Thu, 19 May 2016 13:07:01 GMT",
         "conf":null,
         "lastModTime":"Tue, 19 Jul 2016 17:18:16 GMT",
         "run":0,
         "endTime":"Tue, 19 Jul 2016 17:18:16 GMT",
         "externalId":"hiveMirror11463038709928\/DEFAULT\/2016-05-19T13:07Z",
         "appName":"FALCON_PROCESS_DEFAULT_hiveMirror11463038709928",
         "id":"0001242-160321130408525-oozie-oozi-W",
         "startTime":"Thu, 19 May 2016 13:07:01 GMT",
         "parentId":"0000735-160321130408525-oozie-oozi-C@2042",
         "toString":"Workflow id[0001242-160321130408525-oozie-oozi-W] status[KILLED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001242-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      },
      {
         "appPath":null,
         "acl":null,
         "status":"FAILED",
         "createdTime":"Thu, 19 May 2016 13:02:02 GMT",
         "conf":null,
         "lastModTime":"Thu, 19 May 2016 13:02:03 GMT",
         "run":0,
         "endTime":"Thu, 19 May 2016 13:02:03 GMT",
         "externalId":"0001240-160321130408525-oozie-oozi-W@user-action@0",
         "appName":"falcon-dr-hive-workflow",
         "id":"0001241-160321130408525-oozie-oozi-W",
         "startTime":"Thu, 19 May 2016 13:02:02 GMT",
         "parentId":"0001240-160321130408525-oozie-oozi-W",
         "toString":"Workflow id[0001241-160321130408525-oozie-oozi-W] status[FAILED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001241-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      },
      {
         "appPath":null,
         "acl":null,
         "status":"KILLED",
         "createdTime":"Thu, 19 May 2016 13:02:00 GMT",
         "conf":null,
         "lastModTime":"Tue, 19 Jul 2016 17:18:15 GMT",
         "run":0,
         "endTime":"Tue, 19 Jul 2016 17:18:15 GMT",
         "externalId":"hiveMirror11463038709928\/DEFAULT\/2016-05-19T13:02Z",
         "appName":"FALCON_PROCESS_DEFAULT_hiveMirror11463038709928",
         "id":"0001240-160321130408525-oozie-oozi-W",
         "startTime":"Thu, 19 May 2016 13:02:00 GMT",
         "parentId":"0000735-160321130408525-oozie-oozi-C@2041",
         "toString":"Workflow id[0001240-160321130408525-oozie-oozi-W] status[KILLED]",
         "group":null,
         "consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001240-160321130408525-oozie-oozi-W",
         "user":"root",
         "actions":[

         ]
      }
   ],
   "len":12,
   "offset":1
}


var jobDetail = {"appPath":"hdfs:\/\/sandbox.hortonworks.com:8020\/tmp\/extensions\/hive-mirroring\/resources\/runtime\/hive-mirroring-workflow.xml","acl":null,"status":"FAILED","createdTime":"Mon, 18 Jul 2016 17:18:19 GMT","conf":"<configuration>\r\n  <property>\r\n    <name>falconInputNames<\/name>\r\n    <value>NONE<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>mapreduce.job.user.name<\/name>\r\n    <value>root<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>distcpMapBandwidth<\/name>\r\n    <value>100<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>falconInPaths<\/name>\r\n    <value>NONE<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>feedNames<\/name>\r\n    <value>NONE<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>falcon.libpath<\/name>\r\n    <value>\/apps\/falcon\/primaryCluster\/working\/lib<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.wf.application.lib<\/name>\r\n    <value>hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-client-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-common-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-distcp-replication-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-extensions-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-feed-lifecycle-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-hadoop-dependencies-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-hive-replication-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-messaging-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-metrics-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-oozie-adaptor-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-prism-0.9.2.5.0.0-357-classes.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-rerun-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-retention-0.9.2.5.0.0-357.jar,hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib\/falcon-scheduler-0.9.2.5.0.0-357.jar<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>sourceTables<\/name>\r\n    <value>*<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>entityType<\/name>\r\n    <value>PROCESS<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>sourceDatabases<\/name>\r\n    <value>xademo,default<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>feedInstancePaths<\/name>\r\n    <value>NONE<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.bundle.application.path<\/name>\r\n    <value>hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>logDir<\/name>\r\n    <value>hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/logs<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.use.system.libpath<\/name>\r\n    <value>true<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>userJMSNotificationEnabled<\/name>\r\n    <value>true<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.wf.external.id<\/name>\r\n    <value>0001250-160321130408525-oozie-oozi-W@user-action@0<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.wf.workflow.notification.url<\/name>\r\n    <value>http:\/\/sandbox.hortonworks.com:11000\/oozie\/callback?id=0001250-160321130408525-oozie-oozi-W@user-action&amp;status=$status<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>datasource<\/name>\r\n    <value>NA<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>brokerUrl<\/name>\r\n    <value>tcp:\/\/localhost:61616<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>sourceCluster<\/name>\r\n    <value>testCluster1<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>distcpMaxMaps<\/name>\r\n    <value>1<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.wf.subworkflow.classpath.inheritance<\/name>\r\n    <value>true<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>targetNN<\/name>\r\n    <value>hdfs:\/\/sandbox.hortonworks.com:8020<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>brokerTTL<\/name>\r\n    <value>4320<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>userWorkflowName<\/name>\r\n    <value>hive-mirroring-workflow<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>clusterForJobRun<\/name>\r\n    <value>testCluster1<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>srcClusterName<\/name>\r\n    <value>NA<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>userBrokerUrl<\/name>\r\n    <value>tcp:\/\/sandbox.hortonworks.com:61616?daemon=true<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>replicationMaxMaps<\/name>\r\n    <value>5<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>user.name<\/name>\r\n    <value>root<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.libpath<\/name>\r\n    <value>\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/lib<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.bundle.id<\/name>\r\n    <value>0000734-160321130408525-oozie-oozi-B<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>jobPriority<\/name>\r\n    <value>NORMAL<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.action.yarn.tag<\/name>\r\n    <value>0000735-160321130408525-oozie-oozi-C@2077@user-action<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.action.subworkflow.depth<\/name>\r\n    <value>1<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.wf.application.path<\/name>\r\n    <value>hdfs:\/\/sandbox.hortonworks.com:8020\/tmp\/extensions\/hive-mirroring\/resources\/runtime\/hive-mirroring-workflow.xml<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>targetMetastoreUri<\/name>\r\n    <value>thrift:\/\/sandbox.hortonworks.com:9083<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.coord.application.path<\/name>\r\n    <value>hdfs:\/\/sandbox.hortonworks.com:8020\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/coordinator.xml<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>sourceMetastoreUri<\/name>\r\n    <value>thrift:\/\/sandbox.hortonworks.com:9083<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>shouldRecord<\/name>\r\n    <value>false<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>timeStamp<\/name>\r\n    <value>2016-05-19-16-01<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>clusterForJobRunWriteEP<\/name>\r\n    <value>hdfs:\/\/sandbox.hortonworks.com:8020<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>ENTITY_PATH<\/name>\r\n    <value>\/apps\/falcon\/primaryCluster\/staging\/falcon\/workflows\/process\/hiveMirror11463038709928\/f8319c1e7fce6107b9facd0f5360b176_1463394572377\/DEFAULT\/coordinator.xml<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>nominalTime<\/name>\r\n    <value>2016-05-19-16-02<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>systemJMSNotificationEnabled<\/name>\r\n    <value>true<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>userWorkflowEngine<\/name>\r\n    <value>oozie<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>oozie.wf.parent.id<\/name>\r\n    <value>0001250-160321130408525-oozie-oozi-W<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>sourceNN<\/name>\r\n    <value>hdfs:\/\/sandbox.hortonworks.com:8020<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>queueName<\/name>\r\n    <value>default<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>falconDataOperation<\/name>\r\n    <value>GENERATE<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>workflowEngineUrl<\/name>\r\n    <value>http:\/\/sandbox.hortonworks.com:11000\/oozie\/<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>brokerImplClass<\/name>\r\n    <value>org.apache.activemq.ActiveMQConnectionFactory<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>userBrokerImplClass<\/name>\r\n    <value>org.apache.activemq.ActiveMQConnectionFactory<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>maxEvents<\/name>\r\n    <value>-1<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>ENTITY_NAME<\/name>\r\n    <value>FALCON_PROCESS_DEFAULT_hiveMirror11463038709928<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>availabilityFlag<\/name>\r\n    <value>NA<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>tdeEncryptionEnabled<\/name>\r\n    <value>false<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>targetCluster<\/name>\r\n    <value>testCluster1<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>entityName<\/name>\r\n    <value>hiveMirror11463038709928<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>falconInputFeeds<\/name>\r\n    <value>NONE<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>userWorkflowVersion<\/name>\r\n    <value>1.0<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>cluster<\/name>\r\n    <value>testCluster1<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>colo.name<\/name>\r\n    <value>testColo<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>nameNode<\/name>\r\n    <value>hdfs:\/\/sandbox.hortonworks.com:8020<\/value>\r\n  <\/property>\r\n  <property>\r\n    <name>jobTracker<\/name>\r\n    <value>sandbox.hortonworks.com:8050<\/value>\r\n  <\/property>\r\n<\/configuration>","lastModTime":"Mon, 18 Jul 2016 17:18:27 GMT","run":0,"endTime":"Mon, 18 Jul 2016 17:18:27 GMT","externalId":"0001250-160321130408525-oozie-oozi-W@user-action@0","appName":"falcon-dr-hive-workflow","id":"0001251-160321130408525-oozie-oozi-W","startTime":"Mon, 18 Jul 2016 17:18:19 GMT","parentId":"0001250-160321130408525-oozie-oozi-W","toString":"Workflow id[0001251-160321130408525-oozie-oozi-W] status[FAILED]","group":null,"consoleUrl":"http:\/\/sandbox.hortonworks.com:11000\/oozie?job=0001251-160321130408525-oozie-oozi-W","user":"root","actions":[{"errorMessage":null,"status":"OK","stats":null,"data":null,"transition":"last-event","externalStatus":"OK","cred":"null","conf":"","type":":START:","endTime":"Mon, 18 Jul 2016 17:18:21 GMT","externalId":"-","id":"0001251-160321130408525-oozie-oozi-W@:start:","startTime":"Mon, 18 Jul 2016 17:18:20 GMT","userRetryCount":0,"externalChildIDs":null,"name":":start:","errorCode":null,"trackerUri":"-","retries":0,"userRetryInterval":10,"toString":"Action name[:start:] status[OK]","consoleUrl":"-","userRetryMax":0},{"errorMessage":"variable [sourceHiveServer2Uri] cannot be resolved","status":"FAILED","stats":null,"data":null,"transition":null,"externalStatus":null,"cred":"null","conf":"<java xmlns=\"uri:oozie:workflow:0.3\">\r\n  <job-tracker>${jobTracker}<\/job-tracker>\r\n  <name-node>${nameNode}<\/name-node>\r\n  <configuration>\r\n    <property>\r\n      <!-- hadoop 2 parameter -->\r\n      <name>oozie.launcher.mapreduce.job.user.classpath.first<\/name>\r\n      <value>true<\/value>\r\n    <\/property>\r\n    <property>\r\n      <name>mapred.job.queue.name<\/name>\r\n      <value>${queueName}<\/value>\r\n    <\/property>\r\n    <property>\r\n      <name>oozie.launcher.mapred.job.priority<\/name>\r\n      <value>${jobPriority}<\/value>\r\n    <\/property>\r\n    <property>\r\n      <name>oozie.use.system.libpath<\/name>\r\n      <value>true<\/value>\r\n    <\/property>\r\n    <property>\r\n      <name>oozie.action.sharelib.for.java<\/name>\r\n      <value>distcp,hive,hive2,hcatalog<\/value>\r\n    <\/property>\r\n  <\/configuration>\r\n  <main-class>org.apache.falcon.hive.HiveDRTool<\/main-class>\r\n  <arg>-Dmapred.job.queue.name=${queueName}<\/arg>\r\n  <arg>-Dmapred.job.priority=${jobPriority}<\/arg>\r\n  <arg>-falconLibPath<\/arg>\r\n  <arg>${wf:conf(\"falcon.libpath\")}<\/arg>\r\n  <arg>-sourceCluster<\/arg>\r\n  <arg>${sourceCluster}<\/arg>\r\n  <arg>-sourceMetastoreUri<\/arg>\r\n  <arg>${sourceMetastoreUri}<\/arg>\r\n  <arg>-sourceHiveServer2Uri<\/arg>\r\n  <arg>${sourceHiveServer2Uri}<\/arg>\r\n  <arg>-sourceDatabase<\/arg>\r\n  <arg>${sourceDatabase}<\/arg>\r\n  <arg>-sourceTable<\/arg>\r\n  <arg>${sourceTable}<\/arg>\r\n  <arg>-sourceStagingPath<\/arg>\r\n  <arg>${sourceStagingPath}<\/arg>\r\n  <arg>-sourceNN<\/arg>\r\n  <arg>${sourceNN}<\/arg>\r\n  <arg>-targetCluster<\/arg>\r\n  <arg>${targetCluster}<\/arg>\r\n  <arg>-targetMetastoreUri<\/arg>\r\n  <arg>${targetMetastoreUri}<\/arg>\r\n  <arg>-targetHiveServer2Uri<\/arg>\r\n  <arg>${targetHiveServer2Uri}<\/arg>\r\n  <arg>-targetStagingPath<\/arg>\r\n  <arg>${targetStagingPath}<\/arg>\r\n  <arg>-targetNN<\/arg>\r\n  <arg>${targetNN}<\/arg>\r\n  <arg>-maxEvents<\/arg>\r\n  <arg>${maxEvents}<\/arg>\r\n  <arg>-clusterForJobRun<\/arg>\r\n  <arg>${clusterForJobRun}<\/arg>\r\n  <arg>-clusterForJobRunWriteEP<\/arg>\r\n  <arg>${clusterForJobRunWriteEP}<\/arg>\r\n  <arg>-tdeEncryptionEnabled<\/arg>\r\n  <arg>${tdeEncryptionEnabled}<\/arg>\r\n  <arg>-jobName<\/arg>\r\n  <arg>${jobName}-${nominalTime}<\/arg>\r\n  <arg>-executionStage<\/arg>\r\n  <arg>lastevents<\/arg>\r\n<\/java>","type":"java","endTime":null,"externalId":null,"id":"0001251-160321130408525-oozie-oozi-W@last-event","startTime":null,"userRetryCount":0,"externalChildIDs":null,"name":"last-event","errorCode":"EL_ERROR","trackerUri":null,"retries":0,"userRetryInterval":10,"toString":"Action name[last-event] status[FAILED]","consoleUrl":null,"userRetryMax":0}]}

 var server = {
      //"properties":[{key: "authentication", value: "kerberos"}]
      "properties":[{key: "authentication", value: "simple"}]
    };

  exports.getJobs = getJobs;
  exports.getJobById = getJobById;
  exports.server = server;

})();