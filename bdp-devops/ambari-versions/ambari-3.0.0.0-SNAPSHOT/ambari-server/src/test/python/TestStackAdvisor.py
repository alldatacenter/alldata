'''
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
'''

from unittest import TestCase
import os

class TestStackAdvisorInitialization(TestCase):

  def setUp(self):
    import imp

    self.test_directory = os.path.dirname(os.path.abspath(__file__))
    resources_path = os.path.join(self.test_directory, '../../main/resources')
    stack_advisor_path = os.path.abspath(os.path.join(resources_path, 'scripts/stack_advisor.py'))

    ambari_configuration_path = os.path.abspath(os.path.join(resources_path, 'stacks/ambari_configuration.py'))
    with open(ambari_configuration_path, 'rb') as fp:
      imp.load_module('ambari_configuration', fp, ambari_configuration_path, ('.py', 'rb', imp.PY_SOURCE))

    with open(stack_advisor_path, 'rb') as fp:
      self.stack_advisor = imp.load_module( 'stack_advisor', fp, stack_advisor_path, ('.py', 'rb', imp.PY_SOURCE) )

  def test_stackAdvisorLoadedForNotHDPStack(self):
    path_template = os.path.join(self.test_directory, '../resources/stacks/{0}/{1}/services/stack_advisor.py')
    path_template_name = "STACK_ADVISOR_IMPL_PATH_TEMPLATE"
    setattr(self.stack_advisor, path_template_name, path_template)
    self.assertEquals(path_template, getattr(self.stack_advisor, path_template_name))
    instantiate_stack_advisor_method_name = 'instantiateStackAdvisor'
    instantiate_stack_advisor_method = getattr(self.stack_advisor, instantiate_stack_advisor_method_name)
    stack_advisor = instantiate_stack_advisor_method("XYZ", "1.0.1", ["1.0.0"])
    self.assertEquals("XYZ101StackAdvisor", stack_advisor.__class__.__name__)
    services = {
      "Versions":
        {
          "stack_name":"XYZ",
          "stack_version":"1.0.1"
        },
      "services":[
        {
          "StackServices":{
            "service_name":"YARN"
          },
          "components":[
            {
              "StackServiceComponents": {
                "component_name": "RESOURCEMANAGER"
              }
            },
            {
              "StackServiceComponents": {
                "component_name": "APP_TIMELINE_SERVER"
              }
            },
            {
              "StackServiceComponents": {
                "component_name":"YARN_CLIENT"
              }
            },
            {
              "StackServiceComponents": {
                "component_name": "NODEMANAGER"
              }
            }
          ]
        }
      ]
    }
    hosts= {
      "items": [
        {"Hosts": {"host_name": "host1"}},
        {"Hosts": {"host_name": "host2"}}
      ]
    }
    config_recommendations = stack_advisor.recommendConfigurations(services, hosts)
    yarn_configs = config_recommendations["recommendations"]["blueprint"]["configurations"]["yarn-site"]["properties"]
    '''Check that value is populated from child class, not parent'''
    self.assertEquals("-Xmx101m", yarn_configs["yarn.nodemanager.resource.memory-mb"])

  def test_stackAdvisorDefaultImpl(self):
    instantiate_stack_advisor_method_name = 'instantiateStackAdvisor'
    instantiate_stack_advisor_method = getattr(self.stack_advisor, instantiate_stack_advisor_method_name)
    '''Not existent stack - to return default implementation'''
    default_stack_advisor = instantiate_stack_advisor_method("HDP1", "2.0.6", [])
    self.assertEquals("DefaultStackAdvisor", default_stack_advisor.__class__.__name__)
    services = {
      "Versions":
        {
          "stack_name":"HDP1",
          "stack_version":"2.0.6"
        },
      "services" : [
        {
          "StackServices" : {
            "service_name" : "GANGLIA",
            "service_version" : "3.5.0",
          },
          "components" : [
            {
              "StackServiceComponents" : {
                "cardinality" : "ALL",
                "component_name" : "GANGLIA_MONITOR",
                "is_master" : False,
                "hostnames" : [ ]
              }
            },
            {
              "StackServiceComponents" : {
                "cardinality" : "1",
                "component_name" : "GANGLIA_SERVER",
                "is_master" : True,
                "hostnames" : [ ]
              }
            }
          ]
        },
        {
          "StackServices" : {
            "service_name" : "HBASE",
            "service_version" : "0.98.0.2.1"
          },
          "components" : [
            {
              "StackServiceComponents" : {
                "cardinality" : "1+",
                "component_name" : "HBASE_CLIENT",
                "is_master" : False,
                "hostnames" : [ ]
              }
            },
            {
              "StackServiceComponents" : {
                "cardinality" : "1+",
                "component_name" : "HBASE_MASTER",
                "is_master" : True,
                "hostnames" : [ ]
              }
            },
            {
              "StackServiceComponents" : {
                "cardinality" : "1+",
                "component_name" : "HBASE_REGIONSERVER",
                "is_master" : False,
                "hostnames" : [ ]
              }
            }
          ]
        },
        {
          "StackServices" : {
            "service_name" : "HDFS",
            "service_version" : "2.4.0.2.1"
          },
          "components" : [
            {
              "StackServiceComponents" : {
                "cardinality" : "1+",
                "component_name" : "DATANODE",
                "is_master" : False,
                "hostnames" : [ ]
              }
            }, {
              "StackServiceComponents" : {
                "cardinality" : "1+",
                "component_name" : "HDFS_CLIENT",
                "is_master" : False,
                "hostnames" : [ ]
              }
            }, {
              "StackServiceComponents" : {
                "cardinality" : "0+",
                "component_name" : "JOURNALNODE",
                "is_master" : False,
                "hostnames" : [ ]
              }
            },
            {
              "StackServiceComponents" : {
                "cardinality" : "1-2",
                "component_name" : "NAMENODE",
                "is_master" : True,
                "hostnames" : [ ]
              }
            },
            {
              "StackServiceComponents" : {
                "cardinality" : "1",
                "component_name" : "SECONDARY_NAMENODE",
                "is_master" : True,
                "hostnames" : [ ]
              }
            },
            {
              "StackServiceComponents" : {
                "cardinality" : "0+",
                "component_name" : "ZKFC",
                "is_master" : False,
                "hostnames" : [ ]
              }
            }
          ]
        },
        {
          "StackServices" : {
            "service_name" : "PIG",
            "service_version" : "0.12.1.2.1"
          },
          "components" : [
            {
              "StackServiceComponents" : {
                "cardinality" : "0+",
                "component_name" : "PIG",
                "is_master" : False,
                "hostnames" : [ ]
              }
            }
          ]
        },
        {
          "StackServices" : {
            "service_name" : "TEZ",
            "service_version" : "0.4.0.2.1"
          },
          "components" : [
            {
              "StackServiceComponents" : {
                "cardinality" : "0+",
                "component_name" : "TEZ_CLIENT",
                "is_master" : False,
                "hostnames" : [ ]
              }
            }
          ]
        },
        {
          "StackServices" : {
            "service_name" : "ZOOKEEPER",
            "service_version" : "3.4.5.2.1",
          },
          "components" : [
            {
              "StackServiceComponents" : {
                "cardinality" : "1+",
                "component_category" : "CLIENT",
                "component_name" : "ZOOKEEPER_CLIENT",
                "is_master" : False,
                "hostnames" : [ ]
              }
            },
            {
              "StackServiceComponents" : {
                "cardinality" : "1+",
                "component_name" : "ZOOKEEPER_SERVER",
                "is_master" : True,
                "hostnames" : [ ]
             }
           }
          ]
        }
      ],
      "configurations" : {}
    }
    hosts= {
      "items": [
        {"Hosts": {"host_name": "host1",
                   "cpu_count": 1,
                   "total_mem": 2097152,
                   "disk_info": [{
                     "size": '80000000',
                     "mountpoint": "/"
                   }]
                   }
         },
        {"Hosts": {"host_name": "host2",
                   "cpu_count": 1,
                   "total_mem": 2097152,
                   "disk_info": [{
                     "size": '80000000',
                     "mountpoint": "/"
                   }]
                   }
         }
      ]
    }
    actualValidateConfigResponse = default_stack_advisor.validateConfigurations(services, hosts)
    actualValidateLayoutResponse = default_stack_advisor.validateComponentLayout(services, hosts)
    expectedValidationResponse = {
      "Versions": {"stack_name": "HDP1", "stack_version": "2.0.6"},
      "items": []
    }
    self.assertEquals(actualValidateConfigResponse, expectedValidationResponse)
    self.assertEquals(actualValidateLayoutResponse, expectedValidationResponse)
    actualRecommendConfigResponse = default_stack_advisor.recommendConfigurations(services, hosts)
    expectedRecommendConfigResponse = {
      "Versions": {"stack_name": "HDP1", "stack_version": "2.0.6"},
      "hosts": ["host1", "host2"],
      "services": ['GANGLIA', 'HBASE', 'HDFS', 'PIG', 'TEZ', 'ZOOKEEPER'],
      "recommendations": {
        "blueprint": {
          "configurations": {},
          "host_groups": []
        },
        "blueprint_cluster_binding": {
          "host_groups": []
        }
      }
    }
    self.assertEquals(actualRecommendConfigResponse, expectedRecommendConfigResponse)
    actualRecommendLayoutResponse = default_stack_advisor.recommendComponentLayout(services, hosts)
    expectedRecommendLayoutResponse = {
      "Versions": {"stack_name": "HDP1", "stack_version": "2.0.6"},
      "hosts": ["host1", "host2"],
      "services": ['GANGLIA', 'HBASE', 'HDFS', 'PIG', 'TEZ', 'ZOOKEEPER'],
      "recommendations": {
        "blueprint": {
          "host_groups": [
            {
              "name": "host-group-1",
              "components": []
            },
            {
              "name": "host-group-2",
              "components": [
                {"name": "GANGLIA_SERVER"},
                {"name": "HBASE_MASTER"},
                {"name": "NAMENODE"},
                {"name": "SECONDARY_NAMENODE"},
                {"name": "ZOOKEEPER_SERVER"},
                {"name": "ZOOKEEPER_CLIENT"}
              ]
            }
          ]
        },
        "blueprint_cluster_binding":
          {
            "host_groups": [
              {
                "name": "host-group-1",
                "hosts": [{"fqdn": "host2"}]
              },
              {
                "name": "host-group-2",
                "hosts": [{"fqdn": "host1"}]
              }
            ]
          }
      }
    }
    self.assertEquals(actualRecommendLayoutResponse, expectedRecommendLayoutResponse)

    # Test with maintenance_state. One host is in maintenance mode.
    hosts= {
      "items": [
        {"Hosts": {"host_name": "host1",
                   "maintenance_state":"OFF",
                   "cpu_count": 1}
         },
        {"Hosts": {"host_name": "host2",
                   "maintenance_state":"ON",
                   "cpu_count": 1}
         }
      ]
    }

    actualRecommendLayoutResponse = default_stack_advisor.recommendComponentLayout(services, hosts)
    expectedRecommendLayoutResponse = {
      "services": ["GANGLIA", "HBASE", "HDFS", "PIG", "TEZ", "ZOOKEEPER"],
      "recommendations": {
        "blueprint": {
          "host_groups": [
            {
              "name": "host-group-1",
              "components": [
                {
                  "name": "GANGLIA_SERVER"
                },
                {
                  "name": "HBASE_MASTER"
                },
                {
                  "name": "NAMENODE"
                },
                {
                  "name": "SECONDARY_NAMENODE"
                },
                {
                  "name": "ZOOKEEPER_SERVER"
                },
                {
                  "name": "ZOOKEEPER_CLIENT"
                }
              ]
            }
          ]
        },
        "blueprint_cluster_binding":
          {
            "host_groups": [
              {
                "hosts": [{"fqdn": "host1"}],
                "name": "host-group-1"
              }
            ]
          }
      },
      "hosts": ["host1"],
      "Versions": {"stack_name": "HDP1", "stack_version": "2.0.6"}
    }
    self.assertEquals(actualRecommendLayoutResponse, expectedRecommendLayoutResponse)

    # Test with maintenance_state. Both hosts are in maintenance mode.
    hosts= {
      "items": [
        {"Hosts": {"host_name": "host1",
                   "maintenance_state":"ON",
                   "cpu_count": 1,
                   "total_mem": 2097152,
                   "disk_info": [{
                     "size": '80000000',
                     "mountpoint": "/"
                   }]
                   }
         },
        {"Hosts": {"host_name": "host2",
                   "maintenance_state":"ON",
                   "cpu_count": 1,
                   "total_mem": 2097152,
                   "disk_info": [{
                     "size": '80000000',
                     "mountpoint": "/"
                   }]
                   }
         }
      ]
    }

    actualRecommendLayoutResponse = default_stack_advisor.recommendComponentLayout(services, hosts)

    expectedRecommendLayoutResponse = {
      "Versions": {"stack_name": "HDP1", "stack_version": "2.0.6"},
      "hosts": [],
      "services": ['GANGLIA', 'HBASE', 'HDFS', 'PIG', 'TEZ', 'ZOOKEEPER'],
      "recommendations": {
        "blueprint": {
          "host_groups": []
        },
        "blueprint_cluster_binding": {
          "host_groups": []
        }
      }
    }

    self.assertEquals(actualRecommendLayoutResponse, expectedRecommendLayoutResponse)

    # Config groups support by default
    services["config-groups"] = [{
      "configurations": {
      },
      "hosts": [
        'host2'
      ]
    }]

    actualConfigGroupRecommendConfigResponse = \
      default_stack_advisor.recommendConfigurations(services, hosts)
    expectedConfigGroupRecommendConfigResponse = {
      "Versions": {"stack_name": "HDP1", "stack_version": "2.0.6"},
      "hosts": ["host1", "host2"],
      "services": ['GANGLIA', 'HBASE', 'HDFS', 'PIG', 'TEZ', 'ZOOKEEPER'],
      "recommendations": {
        'config-groups': [
          {
            'configurations': {},
            'dependent_configurations': {},
            'hosts': [
              'host2'
            ]
          }
        ],
        "blueprint": {
          "configurations": {},
          "host_groups": []
        },
        "blueprint_cluster_binding": {
          "host_groups": []
        }
      }
    }
    self.assertEquals(actualConfigGroupRecommendConfigResponse, expectedConfigGroupRecommendConfigResponse)

    services = {
      "services": [
        {
          "StackServices" : {
            "service_name" : "YARN",
            "stack_name" : "HDP",
            "stack_version" : "2.3"
          },
          "configurations" : [
            {
              "StackConfigurations" : {
                "property_depended_by" : [
                  {
                    "type" : "yarn-site",
                    "name" : "yarn.scheduler.minimum-allocation-vcores"
                  },
                  {
                    "type" : "yarn-site",
                    "name" : "yarn.scheduler.maximum-allocation-vcores"
                  }
                ],
                "property_name" : "yarn.nodemanager.resource.cpu-vcores",
                "type" : "yarn-site.xml"
              },
             "dependencies": []
            },
            {
              "StackConfigurations" : {
                "property_name" : "yarn.nodemanager.resource.memory-mb",
                "type" : "yarn-site.xml"
              },
             "dependencies": [
                {
                  "StackConfigurationDependency" : {
                    "dependency_name": "yarn.scheduler.maximum-allocation-mb",
                    "dependency_type": "yarn-site"
                  }
                },
                {
                  "StackConfigurationDependency" : {
                    "dependency_name": "yarn.scheduler.minimum-allocation-mb",
                    "dependency_type": "yarn-site"
                  }
                },
              ]
            },
            {
              "StackConfigurations" : {
                "property_depended_by" : [
                  {
                    "type" : "mapred-site",
                    "name" : "yarn.app.mapreduce.am.resource.mb"
                  },
                  {
                    "type" : "mapred-site",
                    "name" : "mapreduce.map.memory.mb"
                  },
                  {
                    "type" : "mapred-site",
                    "name" : "mapreduce.reduce.memory.mb"
                  }
                ],
                "property_name" : "yarn.scheduler.maximum-allocation-mb",
                "type" : "yarn-site.xml"
              },
             "dependencies": []
            },
            {
              "StackConfigurations" : {
                "property_depended_by" : [ ],
                "property_name" : "yarn.scheduler.maximum-allocation-vcores",
                "type" : "yarn-site.xml"
              },
             "dependencies": []
            },
            {
              "StackConfigurations" : {
                "property_name" : "yarn.scheduler.minimum-allocation-mb",
                "type" : "yarn-site.xml"
              },
             "dependencies": [
                {
                  "StackConfigurationDependency" : {
                    "dependency_name": "hive.tez.container.size",
                    "dependency_type": "hive-site"
                  }
                },
                {
                  "StackConfigurationDependency" : {
                    "dependency_name": "yarn.app.mapreduce.am.resource.mb",
                    "dependency_type": "mapred-site"
                  }
                },
                {
                  "StackConfigurationDependency" : {
                    "dependency_name": "mapreduce.map.memory.mb",
                    "dependency_type": "mapred-site"
                  }
                },
                {
                  "StackConfigurationDependency" : {
                    "dependency_name": "mapreduce.reduce.memory.mb",
                    "dependency_type": "mapred-site"
                  }
                },
              ]
            },
            {
              "StackConfigurations" : {
                "property_name" : "yarn.scheduler.minimum-allocation-vcores",
                "type" : "yarn-site.xml"
              },
             "dependencies": []
            }
          ]
        }
      ],
      "changed-configurations": [
        {
          "type": "yarn-site",
          "name": "yarn.nodemanager.resource.memory-mb"
        }
      ]

    }

    properties_dict = default_stack_advisor.getAffectedConfigs(services)
    expected_properties_dict = [{'name': 'yarn.scheduler.maximum-allocation-mb', 'type': 'yarn-site'},
                                {'name': 'yarn.scheduler.minimum-allocation-mb', 'type': 'yarn-site'},
                                {'name': 'hive.tez.container.size', 'type': 'hive-site'},
                                {'name': 'yarn.app.mapreduce.am.resource.mb', 'type': 'mapred-site'},
                                {'name': 'mapreduce.map.memory.mb', 'type': 'mapred-site'},
                                {'name': 'mapreduce.reduce.memory.mb', 'type': 'mapred-site'}]

    self.assertEquals(properties_dict, expected_properties_dict)