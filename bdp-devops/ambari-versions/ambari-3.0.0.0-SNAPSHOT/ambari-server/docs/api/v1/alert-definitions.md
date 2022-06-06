<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
# Alert Definitions
Definitions are the templates that are used to distribute alerts to the appropriate Ambari agents. They govern the type of alert, the threshold values, and the information to be used when notifying a target. A single definition can be distributed to more than one host in order to produce multiple instances of an alert. 

### Concepts
Each definition contains common information, such as a unique identifier, service, and component. Beyond this, definitions declare a type with each type having its own distinct properties. 

##### Common Properties

- id
- name
- label
- cluster_name
- service_name
- component_name
- source

#### Types
###### SCRIPT
Script definitions defer all functionality to a Python script accessible to the Ambari agents from a specified relative or absolute path.

        "source" : {
          "path" : "HDFS/2.1.0.2.0/package/alerts/alert_ha_namenode_health.py",
          "type" : "SCRIPT"
        }

###### METRIC
METRIC source fields are used to define JMX endpoints that can be queried for values. The `source/reporting` and `jmx/value` fields are parameterized to match the `property_list` specified.

        "source" : {
          "jmx" : {
            "property_list" : [
              "java.lang:type=OperatingSystem/SystemCpuLoad",
              "java.lang:type=OperatingSystem/AvailableProcessors"
            ],
            "value" : "{0} * 100"
          },
          "reporting" : {
            "ok" : {
              "text" : "{1} CPU, load {0:.1%}"
            },
            "warning" : {
              "text" : "{1} CPU, load {0:.1%}",
              "value" : 200.0
            },
            "critical" : {
              "text" : "{1} CPU, load {0:.1%}",
              "value" : 250.0
            },
            "units" : "%"
          },
          "type" : "METRIC",
          "uri" : {
            "http" : "{{hdfs-site/dfs.namenode.http-address}}",
            "https" : "{{hdfs-site/dfs.namenode.https-address}}",
            "https_property" : "{{hdfs-site/dfs.http.policy}}",
            "https_property_value" : "HTTPS_ONLY",
            "default_port" : 0.0,
            "high_availability" : {
              "nameservice" : "{{hdfs-site/dfs.nameservices}}",
              "alias_key" : "{{hdfs-site/dfs.ha.namenodes.{{ha-nameservice}}}}",
              "http_pattern" : "{{hdfs-site/dfs.namenode.http-address.{{ha-nameservice}}.{{alias}}}}",
              "https_pattern" : "{{hdfs-site/dfs.namenode.https-address.{{ha-nameservice}}.{{alias}}}}"
            }
          }
        }
        
###### WEB
WEB definitions are similar in function to PORT definitions. However, instead of checking for TCP connectivity, they also verify that a proper HTTP response code was returned.

        "source" : {
          "reporting" : {
            "ok" : {
              "text" : "HTTP {0} response in {2:.3f} seconds"
            },
            "warning" : {
              "text" : "HTTP {0} response in {2:.3f} seconds"
            },
            "critical" : {
              "text" : "Connection failed to {1}: {3}"
            }
          },
          "type" : "WEB",
          "uri" : {
            "http" : "{{hdfs-site/dfs.namenode.http-address}}",
            "https" : "{{hdfs-site/dfs.namenode.https-address}}",
            "https_property" : "{{hdfs-site/dfs.http.policy}}",
            "https_property_value" : "HTTPS_ONLY",
            "kerberos_keytab" : "{{hdfs-site/dfs.web.authentication.kerberos.keytab}}",
            "kerberos_principal" : "{{hdfs-site/dfs.web.authentication.kerberos.principal}}",
            "default_port" : 0.0,
            "high_availability" : {
              "nameservice" : "{{hdfs-site/dfs.nameservices}}",
              "alias_key" : "{{hdfs-site/dfs.ha.namenodes.{{ha-nameservice}}}}",
              "http_pattern" : "{{hdfs-site/dfs.namenode.http-address.{{ha-nameservice}}.{{alias}}}}",
              "https_pattern" : "{{hdfs-site/dfs.namenode.https-address.{{ha-nameservice}}.{{alias}}}}"
            }
          }
        }

###### PORT
PORT definitions are used to check TCP connectivity to a remote endpoint.

       "source" : {
          "default_port" : 2181,
          "reporting" : {
            "ok" : {
              "text" : "TCP OK - {0:.3f}s response on port {1}"
            },
            "warning" : {
              "text" : "TCP OK - {0:.3f}s response on port {1}",
              "value" : 1.5
            },
            "critical" : {
              "text" : "Connection failed: {0} to {1}:{2}",
              "value" : 5.0
            }
          },
          "type" : "PORT",
          "uri" : "{{core-site/ha.zookeeper.quorum}}"
        }
      }

###### AGGREGATE
AGGREGATE definitions are used to combine the results of another alert definition from different nodes.  The `source/alert_name` field must match the `name` field of another alert definition.

        "source": {
          "type": "AGGREGATE",
          "alert_name": "datanode_process",
          "reporting": {
            "ok": {
              "text": "affected: [{1}], total: [{0}]"
            },
            "warning": {
              "text": "affected: [{1}], total: [{0}]",
              "value": 10
            },
            "critical": {
              "text": "affected: [{1}], total: [{0}]",
              "value": 30
            },
            "units" : "%",
            "type": "PERCENT"
          }
        }

#### Structures & Concepts
- `uri` Definition types that contain a URI can depend on any number of valid subproperties. In some cases, the URI may be very simple and only include a single port. In other scenarios, the URI may be more complex and include properties for plaintext, SSL, and secure endpoints protected by Kerberos.

    - http - a property that contains the plaintext endpoint to test
    - https - a property that contains the SSL endpoint to test
    - https_property - a property that contains the value which can be used to determine if the component is SSL protected
    - https_property_value - a constant value to compare with `https_property` in order to determine if the component is protected by SSL
    - kerberos_keytab - a property that contains the Kerberos keytab if security is enabled
    - kerberos_principal - a property that contains the Kerberos principal if security is enabled
    - default_port - a default port which can be used if none of the above properties can be realized
    - high_availability - a structure that contains a way to dynamically build properties which contain the endpoints to use when the components are running in HA mode
    
<p/>

- `reporting` A structure that defines the thresholds and text to use when determining the state for an alert. `ok` is always a required element, however only a single `warning` or `critical` element is needed. Some alerts may only have two states (such as `OK` and `CRITICAL`) and will bypass the need for a `warning` element.

<p/>

- `default_port` A URI, host, or integer that represents a fallback value to use if none of the other specified properties can be realized.

### API Summary

##### Create
    POST api/v1/clusters/<cluster>/alert_definitions
    
    {
      "AlertDefinition" : {
        "service_name" : "YARN",      
        "component_name" : "RESOURCEMANAGER",
        "enabled" : true,
        "interval" : 1,
        "label" : "Example YARN ResourceManager Alert",
        "name" : "example_yarn_rm_alert",
        "scope" : "ANY",
        "source" : {
          "default_port" : 8050,
          "reporting" : {
            "ok" : {
              "text" : "TCP OK - {0:.4f} response on port {1}"
            },
           "warning" : {
              "text" : "TCP OK - {0:.3f}s response on port {1}",
              "value" : 1.5
            },
            "critical" : {
              "text" : "Connection failed: {0} on host {1}:{2}",
              "value" : 5.0
            }
          },
          "type" : "PORT",
          "uri" : "{{yarn-site/yarn.resourcemanager.address}}"
        }
      }
    }    

##### Update
When updating an existing definition, then entire structure is not required. A partial request is allowed and will only change the properties that were supplied. Properties that are omitted are considered unchanged.

    PUT api/v1/clusters/<cluster>/alert_definitions/<definition-id>
    
    {
      "AlertDefinition" : {
        "interval" : 10,
          "uri" : "{{yarn-site/yarn.resourcemanager.address.foo}}"
        }
      }
    }
        

##### Delete
    DELETE api/v1/clusters/<cluster>/alert_definitions/<definition-id>

##### Querying
Definitions are always retrieved from the cluster endpoint since each cluster can have its own assortment of alerts defined. 

###### Request
    GET api/v1/clusters/<cluster>/alert_definitions    
    
###### Response
    {
      "href" : "http://<server>/api/v1/clusters/<cluster>/alert_definitions",
      "items" : [
        {
          "href" : "http://<server>/api/v1/clusters/<cluster>/alert_definitions/1",
          "AlertDefinition" : {
            "cluster_name" : "<cluster>",
            "id" : 1,
            "label" : "History Server Web UI",
            "name" : "mapreduce_history_server_webui"
          }
        },
        {
          "href" : "http://<server>/api/v1/clusters/<cluster>/alert_definitions/2",
          "AlertDefinition" : {
            "cluster_name" : "<cluster>",
            "id" : 2,
            "label" : "History Server Process",
            "name" : "mapreduce_history_server_process"
          }
        },
        ...
      ]
    }

Querying for definitions accepts the same predicates and syntax as other areas of the REST API. For example, to query for all definitions for `NAMENODE` that are of type `WEB` and `METRIC` the following query can be used.

###### Request
    GET api/v1/clusters/<cluster>/alert_definitions?AlertDefinition/component_name=NAMENODE&AlertDefinition/source/type.in(WEB,METRIC)

###### Response
    {
      "href" : "http://<server>/api/v1/clusters/<cluster>/alert_definitions?AlertDefinition/component_name=NAMENODE&AlertDefinition/source/type.in(WEB,METRIC)",
      "items" : [
        {
          "href" : "http://<server>/api/v1/clusters/<cluster>/alert_definitions/14",
          "AlertDefinition" : {
            "cluster_name" : "<cluster>",
            "component_name" : "NAMENODE",
            "id" : 14,
            "label" : "NameNode Host CPU Utilization",
            "name" : "namenode_cpu",
            "source" : {
              "jmx" : {
                "property_list" : [
                  "java.lang:type=OperatingSystem/SystemCpuLoad",
                  "java.lang:type=OperatingSystem/AvailableProcessors"
                ],
                "value" : "{0} * 100"
              },
              "reporting" : {
                "ok" : {
                  "text" : "{1} CPU, load {0:.1%}"
                },
                "warning" : {
                  "text" : "{1} CPU, load {0:.1%}",
                  "value" : 200.0
                },
                "critical" : {
                  "text" : "{1} CPU, load {0:.1%}",
                  "value" : 250.0
                },
                "units" : "%"
              },
              "type" : "METRIC",
              "uri" : {
                "http" : "{{hdfs-site/dfs.namenode.http-address}}",
                "https" : "{{hdfs-site/dfs.namenode.https-address}}",
                "https_property" : "{{hdfs-site/dfs.http.policy}}",
                "https_property_value" : "HTTPS_ONLY",
                "default_port" : 0.0,
                "high_availability" : {
                  "nameservice" : "{{hdfs-site/dfs.nameservices}}",
                  "alias_key" : "{{hdfs-site/dfs.ha.namenodes.{{ha-nameservice}}}}",
                  "http_pattern" : "{{hdfs-site/dfs.namenode.http-address.{{ha-nameservice}}.{{alias}}}}",
                  "https_pattern" : "{{hdfs-site/dfs.namenode.https-address.{{ha-nameservice}}.{{alias}}}}"
                }
              }
            }
          }
        },
        ...
      ]
    }
    
##### Immediate Execution
Definitions can be forcible triggered to run outside of their normally scheduled timeframe. This can be useful when verifying that an alert has changed state without waiting for it to run on its own.

###### Request
Using the `run_now` directive, a definition can be scheduled to run immediately on all hosts where it is distributed.

    PUT http://<server>/api/v1/clusters/<cluster>/alert_definitions/<definition-id>?run_now=true
    
###### Response
    HTTP 200 OK
