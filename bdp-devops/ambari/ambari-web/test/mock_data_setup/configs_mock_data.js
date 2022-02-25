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

module.exports = {
  setupConfigGroupsObject: function(serviceName) {
    var serviceGroups = this.setupServiceConfigTagsObject(serviceName).mapProperty('siteName');
    var configGroups = [
      {
        "tag":"version1",
        "type":"core-site",
        "properties": {
          "fs.defaultFS" : "hdfs://c6401.ambari.apache.org:8020",
          "fs.trash.interval" : "360"
        }
      },
      {
        "tag":"version1",
        "type":"hadoop-env",
        "properties":{
          "hadoop_heapsize":"1024",
          "hdfs_user": "hdfs"
        }
      },
      {
        "tag":"version1",
        "type":"hdfs-site",
        "properties": {
          "dfs.datanode.data.dir": "/b,/a",
          "dfs.namenode.name.dir": "/b,/a,/c",
          "dfs.namenode.checkpoint.dir": "/b,/d,/a,/c",
          "dfs.datanode.failed.volumes.tolerated": "2",
          "content": "custom mock property"
        }
      },
      {
        "tag":"version1",
        "type":"hdfs-log4j",
        "properties": {
          "content": "hdfs log4j content"
        }
      },
      {
        "tag":"version1",
        "type":"zoo.cfg",
        "properties": {
          "custom.zoo.cfg": "zoo cfg content"
        }
      },
      {
        "tag":"version1",
        "type":"storm-site",
        "properties": {
          "storm.zookeeper.servers": "['c6401.ambari.apache.org','c6402.ambari.apache.org']",
          "single_line_property": "value",
          "multi_line_property": "value \n value"
        }
      },
      {
        "tag": "version1",
        "type": "storm-env",
        "properties": {
          "nonexistent_property": "some value",
          "storm_log_dir": "/var/log/storm",
          "stormuiserver_host": "c6401.ambari.apache.org"
        }
      },
      {
        "tag":"version1",
        "type":"zoo.cfg",
        "properties": {
          "custom.zoo.cfg": "value"
        }
      }
    ];
    return configGroups.filter(function(configGroup) {
      return serviceGroups.contains(configGroup.type);
    });
  },
  setupServiceConfigTagsObject: function(serviceName) {
    var configTags = {
      STORM: ['storm-env','storm-site'],
      HDFS: ['hadoop-env','hdfs-site','core-site','hdfs-log4j'],
      ZOOKEEPER: ['hadoop-env', 'zoo.cfg']
    };
    var configTagsObject = [];
    if (serviceName) {
      configTags[serviceName].forEach(function(tag) {
        configTagsObject.push({
          siteName: tag,
          tagName: "version1",
          newTagName: null
        });
      });
    } else {
      Object.keys(configTags).forEach(function (sName) {
        configTags[sName].forEach(function(tag) {
          configTagsObject.push({
            siteName: tag,
            tagName: "version1",
            newTagName: null
          });
        });
      });
    }
    return configTagsObject.uniq();
  },
  setupAdvancedConfigsObject: function() {
    return [
      {
        "serviceName": "HDFS",
        "name": "fs.defaultFS",
        "value": "hdfs://c6401.ambari.apache.org:8020",
        "description": "fs.defaultFS",
        "filename": "core-site.xml"
      },
      {
        "serviceName": "STORM",
        "name": "storm.zookeeper.servers",
        "value": "['localhost']",
        "description": "desc",
        "filename": "storm-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "dfs.datanode.data.dir",
        "value": "/hadoop/hdfs/data",
        "description": "desc",
        "filename": "hdfs-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "dfs.namenode.name.dir",
        "value": "/hadoop/hdfs/namenode",
        "description": "desc",
        "filename": "hdfs-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "dfs.namenode.checkpoint.dir",
        "value": "/hadoop/hdfs/namesecondary",
        "description": "desc",
        "filename": "hdfs-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "dfs.datanode.failed.volumes.tolerated",
        "value": "2",
        "description": "desc",
        "filename": "hdfs-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "content",
        "value": "custom mock property",
        "description": "desc",
        "filename": "hdfs-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "content",
        "value": "hdfs log4j content",
        "description": "desc",
        "filename": "hdfs-log4j.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "content",
        "value": "custom hdfs log4j content",
        "description": "desc",
        "filename": "custom-hdfs-log4j.xml"
      },
      {
        "serviceName": "ZOOKEEPER",
        "name": "content",
        "value": "zookeeper log4j.xml content",
        "description": "desc",
        "filename": "zookeeper-log4j.xml"
      },
      {
        "serviceName": "ZOOKEEPER",
        "name": "custom.zoo.cfg",
        "value": "zoo cfg content",
        "description": "zoo.cfg config",
        "filename": "zoo.cfg"
      },
      {
        "serviceName": "YARN",
        "name": "content",
        "value": " value \n value",
        "filename": "capacity-scheduler.xml"
      },
      {
        "name": "hbase_log_dir",
        "value": "/hadoop/hbase",
        "serviceName": "HBASE",
        "filename": "hbase-env.xml"
      },
      {
        "name": "hbase_log_dir",
        "value": "/hadoop/ams-hbase",
        "serviceName": "AMBARI_METRICS",
        "filename": "ams-hbase-env.xml"
      }
    ];
  },
  setupStoredConfigsObject: function() {
    return [
      {
        "name":"storm.zookeeper.servers",
        "value":[
          "c6401.ambari.apache.org",
          "c6402.ambari.apache.org"
        ],
        "recommendedValue":"['c6401.ambari.apache.org','c6402.ambari.apache.org']",
        "filename":"storm-site.xml",
        "isUserProperty":false,
        "isOverridable":false,
        "showLabel":true,
        "serviceName":"STORM",
        "displayType":"componentHosts",
        "isVisible":true,
        "description":"desc",
        "isSecureConfig":false,
        "category":"General",
        "displayName":"storm.zookeeper.servers"
      },
      {
        "name":"single_line_property",
        "value":"value",
        "recommendedValue":"value",
        "filename":"storm-site.xml",
        "isUserProperty":true,
        "isOverridable":true,
        "showLabel":true,
        "serviceName":"STORM",
        "displayType":"string",
        "displayName":"single_line_property",
        "category":"AdvancedStormSite"
      },
      {
        "name":"multi_line_property",
        "value":"value \n value",
        "recommendedValue":"value \n value",
        "filename":"storm-site.xml",
        "isUserProperty":true,
        "isOverridable":true,
        "showLabel":true,
        "serviceName":"STORM",
        "displayType":"multiLine",
        "displayName":"multi_line_property",
        "category":"AdvancedStormSite"
      },
      {
        "name":"nonexistent_property",
        "value":"some value",
        "recommendedValue":"some value",
        "filename":"storm-env.xml",
        "isUserProperty":false,
        "isOverridable":true,
        "showLabel":true,
        "serviceName":"STORM",
        "isVisible":false,
        "displayName":null,
        "options":null
      },
      {
        "name":"dfs.datanode.data.dir",
        "value":"/a,/b",
        "recommendedValue":"/a,/b",
        "filename":"hdfs-site.xml",
        "isUserProperty":false,
        "isOverridable":true,
        "showLabel":true,
        "serviceName":"HDFS",
        "displayType":"directories",
        "isRequired":true,
        "isReconfigurable":true,
        "isVisible":true,
        "description":"desc",
        "index":1,
        "isSecureConfig":false,
        "category":"DataNode",
        "displayName":"DataNode directories"
      },
      {
        "name":"content",
        "value":"custom mock property",
        "recommendedValue":"custom mock property",
        "filename":"hdfs-site.xml",
        "isUserProperty":false,
        "isOverridable":true,
        "showLabel":false,
        "serviceName":"HDFS",
        "displayType":"content",
        "isRequired":true,
        "isRequiredByAgent":true,
        "isReconfigurable":true,
        "isVisible":true,
        "description":"desc",
        "isSecureConfig":false,
        "category":"AdvancedHDFSLog4j",
        "displayName":"content"
      },
      {
        "name":"content",
        "value":"hdfs log4j content",
        "recommendedValue":"hdfs log4j content",
        "filename":"hdfs-log4j.xml",
        "isUserProperty":false,
        "isOverridable":true,
        "showLabel":false,
        "serviceName":"HDFS",
        "displayType":"content",
        "isRequired":true,
        "isRequiredByAgent":true,
        "isReconfigurable":true,
        "isVisible":true,
        "description":"desc",
        "isSecureConfig":false,
        "category":"AdvancedHDFSLog4j",
        "displayName":"content"
      },
      {
        "name":"storm_log_dir",
        "value":"/var/log/storm",
        "recommendedValue":"/var/log/storm",
        "filename":"storm-env.xml",
        "isUserProperty":false,
        "isOverridable":true,
        "showLabel":true,
        "serviceName":"STORM",
        "displayType":"directory",
        "isRequired":true,
        "isRequiredByAgent":true,
        "isReconfigurable":true,
        "isVisible":true,
        "description":"Storm log directory",
        "isSecureConfig":false,
        "category":"General",
        "displayName":"storm_log_dir"
      }
    ];
  },

  advancedConfigs: {
    items: [
      {
        "StackConfigurations" : {
          "final" : "false",
          "property_description" : "Proxy user group.",
          "property_name" : "proxyuser_group",
          "property_type" : [
            "GROUP"
          ],
          "property_value" : "users",
          "service_name" : "HDFS",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "hadoop-env.xml"
        }
      },
      {
        "StackConfigurations" : {
          "final" : "true",
          "property_description" : "dfs.datanode.data.dir description",
          "property_name" : "dfs.datanode.data.dir",
          "property_type" : [ ],
          "property_value" : "/hadoop/hdfs/data",
          "service_name" : "HDFS",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "hdfs-site.xml"
        }
      },
      {
        "StackConfigurations" : {
          "final" : "true",
          "property_description" : "to enable dfs append",
          "property_name" : "dfs.support.append",
          "property_type" : [ ],
          "property_value" : "true",
          "service_name" : "HDFS",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "hdfs-site.xml"
        }
      },
      {
        "StackConfigurations" : {
          "final" : "false",
          "property_description" : "User to run HDFS as",
          "property_name" : "hdfs_user",
          "property_type" : [
            "USER"
          ],
          "property_value" : "hdfs",
          "service_name" : "HDFS",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "hadoop-env.xml"
        }
      },
      {
        "StackConfigurations" : {
          "final" : "false",
          "property_description" : "The permissions that should be there on dfs.datanode.data.dir\n      directories. The datanode will not come up if the permissions are\n      different on existing dfs.datanode.data.dir directories. If the directories\n      don't exist, they will be created with this permission.",
          "property_name" : "dfs.datanode.data.dir.perm",
          "property_type" : [ ],
          "property_value" : "750",
          "service_name" : "HDFS",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "hdfs-site.xml"
        }
      },
      {
        "StackConfigurations" : {
          "final" : "false",
          "property_description" : "\n      DB user password.\n\n      IMPORTANT: if password is emtpy leave a 1 space string, the service trims the value,\n      if empty Configuration assumes it is NULL.\n    ",
          "property_name" : "oozie.service.JPAService.jdbc.password",
          "property_type" : [
            "PASSWORD"
          ],
          "property_value" : " ",
          "service_name" : "OOZIE",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "oozie-site.xml"
        }
      },
      {
        "StackConfigurations" : {
          "final" : "false",
          "property_description" : "prop description",
          "property_name" : "storm_log_dir",
          "property_type" : [],
          "property_value" : " ",
          "service_name" : "STORM",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "storm-env.xml"
        }
      }

    ]
  },
  
  advancedClusterConfigs: {
    items: [
      {
        "StackLevelConfigurations" : {
          "final" : "false",
          "property_description" : "Whether to ignore failures on users and group creation",
          "property_name" : "ignore_groupsusers_create",
          "property_type" : [ ],
          "property_value" : "false",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "cluster-env.xml"
        }
      },
      {
        "StackLevelConfigurations" : {
          "final" : "false",
          "property_description" : "Hadoop user group.",
          "property_name" : "user_group",
          "property_type" : [
            "GROUP"
          ],
          "property_value" : "hadoop",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "cluster-env.xml"
        }
      },
      {
        "StackLevelConfigurations" : {
          "final" : "false",
          "property_description" : "",
          "property_name" : "smokeuser",
          "property_type" : [
            "USER"
          ],
          "property_value" : "ambari-qa",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "cluster-env.xml"
        }
      },
      {
        "StackLevelConfigurations" : {
          "final" : "false",
          "property_description" : "",
          "property_name" : "zk_user",
          "property_type" : [
            "USER"
          ],
          "property_value" : "zookeeper",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "cluster-env.xml"
        }
      },
      {
        "StackLevelConfigurations" : {
          "final" : "false",
          "property_description" : "",
          "property_name" : "mapred_user",
          "property_type" : [
            "USER"
          ],
          "property_value" : "mapreduce",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "type" : "cluster-env.xml"
        }
      }
    ]
  }
  
}
