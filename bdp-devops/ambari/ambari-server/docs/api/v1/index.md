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

Ambari API Reference v1
=========

The Ambari API facilitates the management and monitoring of the resources of an Apache Hadoop cluster. This document describes the resources and syntax used in the Ambari API and is intended for developers who want to integrate with Ambari.

- [Release Version](#release-version)
- [Authentication](#authentication)
- [Monitoring](#monitoring)
- [Management](#management)
- [Resources](#resources)
- [Partial Response](#partial-response)
- [Query Predicates](#query-predicates)
- [Batch Requests](#batch-requests)
- [RequestInfo](#request-info)
- [Temporal Metrics](#temporal-metrics)
- [Pagination](#pagination)
- [Errors](#errors)


Release Version
----
_Last Updated June 3, 2013;  Note that this is the official release of Ambari V1 API. No breaking changes will be introduced to this version of the API._
 

Authentication
----

The operations you perform against the Ambari API require authentication. Access to the API requires the use of **Basic Authentication**. To use Basic Authentication, you need to send the **Authorization: Basic** header with your requests. For example, this can be handled when using curl and the --user option.

    curl --user name:password http://{your.ambari.server}/api/v1/clusters

_Note: The authentication method and source is configured at the Ambari Server. Changing and configuring the authentication method and source is not covered in this document._

Monitoring
----
The Ambari API provides access to monitoring and metrics information of an Apache Hadoop cluster.

### GET
Use the GET method to read the properties, metrics and sub-resources of an Ambari resource.  Calling the GET method returns the requested resources and produces no side-effects.  A response code of 200 indicates that the request was successfully processed with the requested resource included in the response body.
 
**Example**

Get the DATANODE component resource for the HDFS service of the cluster named 'c1'.

    GET /clusters/c1/services/HDFS/components/DATANODE

**Response**

    200 OK
    {
    	"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE",
    	"metrics" : {
    		"process" : {
              "proc_total" : 697.75,
              "proc_run" : 0.875
    		},
      		"rpc" : {
        		...
      		},
      		"ugi" : {
      			...
      		},
      		"dfs" : {
        		"datanode" : {
          		...
        		}
      		},
      		"disk" : {
        		...
      		},
      		"cpu" : {
        		...
      		}
      		...
        },
    	"ServiceComponentInfo" : {
      		"cluster_name" : "c1",
      		"component_name" : "DATANODE",
      		"service_name" : "HDFS"
      		"state" : "STARTED"
    	},
    	"host_components" : [
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/host1/host_components/DATANODE",
      			"HostRoles" : {
        			"cluster_name" : "c1",
        			"component_name" : "DATANODE",
        			"host_name" : "host1"
        		}
      		}
       	]
    }


Management
----
The Ambari API provides for the management of the resources of an Apache Hadoop cluster.  This includes the creation, deletion and updating of resources.

### POST
The POST method creates a new resource. If a new resource is created then a 201 response code is returned.  The code 202 can also be returned to indicate that the instruction was accepted by the server (see [asynchronous response](#asynchronous-response)). 

**Example**

Create the HDFS service.


    POST /clusters/c1/services/HDFS


**Response**

    201 Created

### PUT
Use the PUT method to update resources.  If an existing resource is modified then a 200 response code is retrurned to indicate successful completion of the request.  The response code 202 can also be returned to indicate that the instruction was accepted by the server (see [asynchronous response](#asynchronous-response)).

**Example**

Start the HDFS service (update the state of the HDFS service to be ‘STARTED’).


    PUT /clusters/c1/services/HDFS/

**Body**

    {
      "ServiceInfo": {
        "state" : "STARTED"
      }
    }


**Response**

The response code 202 indicates that the server has accepted the instruction to update the resource.  The body of the response contains the ID and href of the request resource that was created to carry out the instruction (see [asynchronous response](#asynchronous-response)).

    202 Accepted
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/3",
      "Requests" : {
        "id" : 3,
        "status" : "InProgress"
      } 
    }


### DELETE
Use the DELETE method to delete a resource. If an existing resource is deleted then a 200 response code is retrurned to indicate successful completion of the request.  The response code 202 can also be returned which indicates that the instruction was accepted by the server and the resource was marked for deletion (see [asynchronous response](#asynchronous-response)).

**Example**

Delete the cluster named 'c1'.

    DELETE /clusters/c1

**Response**

    200 OK

### Asynchronous Response

The managment APIs can return a response code of 202 which indicates that the request has been accepted.  The body of the response contains the ID and href of the request resource that was created to carry out the instruction. 
    
    202 Accepted
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6",
      "Requests" : {
        "id" : 6,
        "status" : "InProgress"
      } 
    }

The href in the response body can then be used to query the associated request resource and monitor the progress of the request.  A request resource has one or more task sub resources.  The following example shows how to use [partial response](#partial-response) to query for task resources of a request resource. 

    /clusters/c1/requests/6?fields=tasks/Tasks/*   
    
The returned task resources can be used to determine the status of the request.

    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6",
      "Requests" : {
        "id" : 6,
        "cluster_name" : "c1"
      },
      "tasks" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6/tasks/32",
          "Tasks" : {
            "exit_code" : 777,
            "stdout" : "default org.apache.hadoop.mapred.CapacityTaskScheduler\nwarning: Dynamic lookup of ...",
            "status" : "IN_PROGRESS",
            "stderr" : "",
            "host_name" : "dev.hortonworks.com",
            "id" : 32,
            "cluster_name" : "c1",
            "attempt_cnt" : 1,
            "request_id" : 6,
            "command" : "START",
            "role" : "NAMENODE",
            "start_time" : 1367240498196,
            "stage_id" : 1
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6/tasks/33",
          "Tasks" : {
            "exit_code" : 999,
            "stdout" : "",
            "status" : "PENDING",
            ...
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6/tasks/31",
          "Tasks" : {
            "exit_code" : 0,
            "stdout" : "warning: Dynamic lookup of $ambari_db_rca_username ...",
            "status" : "COMPLETED",
            ...
          }
        }
      ]
    }

Resources
----
### Collection Resources


A collection resource is a set of resources of the same type, rather than any specific resource. For example:

    /clusters  

  _Refers to a collection of clusters_

### Instance Resources

An instance resource is a single specific resource. For example:

    /clusters/c1

  _Refers to the cluster resource identified by the id "c1"_

### Types
Resources are grouped into types.  This allows the user to query for collections of resources of the same type.  Some resource types are composed of subtypes (e.g. services are sub-resources of clusters).

The following is a list of some of the Ambari resource types with descriptions and usage examples.
 
#### clusters
Cluster resources represent named Hadoop clusters.  Clusters are top level resources. 

[Cluster Resources](cluster-resources.md)

#### services
Service resources are services of a Hadoop cluster (e.g. HDFS, MapReduce and Ganglia).  Service resources are sub-resources of clusters. 

[Service Resources](service-resources.md)

#### components
Component resources are the individual components of a service (e.g. HDFS/NameNode and MapReduce/JobTracker).  Components are sub-resources of services.

[Component Resources](component-resources.md)

#### hosts
Host resources are the host machines that make up a Hadoop cluster.  Hosts are top level resources but can also be sub-resources of clusters. 

[Host Resources](host-resources.md)


#### host_components
Host component resources are usages of a component on a particular host.  Host components are sub-resources of hosts.

[Host Component Resources](host-component-resources.md)


#### configurations
Configuration resources are sets of key/value pairs that configure the services of a Hadoop cluster.

[Configuration Resource Overview](configuration.md)

#### config_groups
Config group is type of resource that supports grouping of configuration resources and host resources for a service.

[Config Group Overview](config-groups.md)

#### requests
Request resources are groups of tasks that were created to carry out an instruction.

[Request Resources](request-resources.md)

#### tasks
Task resources are the individual tasks that make up a request resource.

[Task Resources](task-resources.md)

#### request_schedules
A request schedule defines a batch of requests to be executed based on a schedule.

[Request Schedule Resources](request-schedules.md)

#### workflows
Workflow resources are DAGs of MapReduce jobs in a Hadoop cluster.

[Workflow Resources](workflow-resources.md)

#### jobs
Job resources represent individual nodes (MapReduce jobs) in a workflow.

[Job Resources](job-resources.md)

#### taskattempts
Task attempt resources are individual attempts at map or reduce tasks for a job.

[Task Attempt Resources](taskattempt-resources.md)

#### views
Views offer a systematic way to plug-in UI capabilities to surface custom visualization, management and monitoring features in Ambari Web.

[View Resources](view-resources.md)

#### repository_versions
Repository version resources contain information about available repositories with Hadoop stacks for cluster.

[Repository Version Resources](repository-version-resources.md)

#### stack_versions
Stack version resources contain relationship between some resource and repository versions.

[Stack Version Resources](stack-version-resources.md)

#### rolling_upgrades_check
Rolling upgrades check resources store information about successful/unsuccessful checks performed before rolling upgrade.

[Rolling Upgrade Check Resources](rolling-upgrade-check-resources.md)

#### upgrades
[Upgrade Resources](upgrades.md)

#### alerts
Alert resources contain the relationships between definitions, history, and the dispatching of outbound notifications. Ambari leverages its own alert framework in order to monitor a service, component, or host and produce notifications to interested parties.

- [Alert Definitions](alert-definitions.md) Definitions are the templates that are used to distribute alerts to the appropriate Ambari agents. They govern the type of alert, the threshold values, and the information to be used when notifying a target.

- [Alert Dispatching](alert-dispatching.md) Dispatching involves creating groups of alert definitions and adding notification targets to those groups.

- [Alert History](alerts.md) The current state of an alert and all of its historical events are available for querying.

#### credentials
Credential resources are principal (or username) and password pairs that are tagged with an alias and stored either in a _temporary_ or _persisted_ storage facility.  These resources may be created, updated, and deleted; however (for security reasons) when getting credential resources, only the alias and an indicator of whether the credential is stored in the temporary or persisted store is returned.  Credentials are sub-resources of Clusters.

[Credential Resources](credential-resources.md)

#### permissions
Permission resources are used to help determine authorization rights for a user.  A permission is assigned to a user by setting up a privilege relationship between a user and the permission to be projected onto some resource.  

[Permission Resources](permission-resources.md)

#### users
User resources represent users that may use Ambari. A user is given permissions to perform tasks within Ambari.  

[User Resources](user-resources.md)

#### authentication sources
Authentication source resources are child resources of [user resources](#users). Each source represent an authentication 
source that a user may use to login into Ambari.  There are different types of authentication sources
such as (but not limited to) local, LDAP, JWT, and Kerberos.

[Authentication Source Resources](authentication-source-resources.md)

Partial Response
----

Used to control which fields are returned by a query.  Partial response can be used to restrict which fields are returned and additionally, it allows a query to reach down and return data from sub-resources.  The keyword “fields” is used to specify a partial response.  Only the fields specified will be returned to the client.  To specify sub-elements, use the notation “a/b/c”.  Properties, categories and sub-resources can be specified.  The wildcard ‘*’ can be used to show all categories, fields and sub-resources for a resource.  This can be combined to provide ‘expand’ functionality for sub-components.  Some fields are always returned for a resource regardless of the specified partial response fields.  These fields are the fields, which uniquely identify the resource.  This would be the primary id field of the resource and the foreign keys to the primary id fields of all ancestors of the resource.

**Example: Using Partial Response to restrict response to a specific field**

    GET    /api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total

    200 OK
	{
    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total”,
    	“ServiceComponentInfo” : {
        	“cluster_name” : “c1”,
        	“component_name” : NAMENODE”,
        	“service_name” : “HDFS”
    	},
    	“metrics” : {
        	"disk" : {       
            	"disk_total" : 100000
        	}
    	}
    }

**Example: Using Partial Response to restrict response to specified category**

    GET    /api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk

    200 OK
	{
    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk”,
    	“ServiceComponentInfo” : {
        	“cluster_name” : “c1”,
        	“component_name” : NAMENODE”,
        	“service_name” : “HDFS”
    	},
    	“metrics” : {
        	"disk" : {       
            	"disk_total" : 100000,
            	“disk_free” : 50000,
            	“part_max_used” : 1010
        	}
    	}
	}

**Example – Using Partial Response to restrict response to multiple fields/categories**

	GET	/api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total,metrics/cpu
	
	200 OK
	{
    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total,metrics/cpu”,
    	“ServiceComponentInfo” : {
        	“cluster_name” : “c1”,
        	“component_name” : NAMENODE”,
        	“service_name” : “HDFS”
    	},
    	“metrics” : {
        	"disk" : {       
            	"disk_total" : 100000
        	},
        	“cpu” : {
            	“cpu_speed” : 10000000,
            	“cpu_num” : 4,
            	“cpu_idle” : 999999,
            	...
        	}
    	}
	}

**Example – Using Partial Response to restrict response to a sub-resource**

	GET	/api/v1/clusters/c1/hosts/host1?fields=host_components

	200 OK
	{
    	“href”: “.../api/v1/clusters/c1/hosts/host1?fields=host_components”,
    	“Hosts” : {
        	“cluster_name” : “c1”,
        	“host_name” : “host1”
    	},
    	“host_components”: [
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : “NAMENODE”,
                	“host_name” : “host1”
            	}
        	},
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : DATANODE”,
                	“host_name” : “host1”
            	}
        	},
            ... 
    	]
	}

**Example – Using Partial Response to expand a sub-resource one level deep**

	GET	/api/v1/clusters/c1/hosts/host1?fields=host_components/*

	200 OK
	{
    	“href”: “.../api/v1/clusters/c1/hosts/host1?fields=host_components/*”,
    	“Hosts” : {
        	“cluster_name” : “c1”,
        	“host_name” : “host1”
        },
        “host_components”: [
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
            	“HostRoles” : {
                	“cluster_name” : “c1”,
               		“component_name” : DATANODE”,
                	“host_name” : “host1”,
                	“state” : “RUNNING”,
                	...
            	},        
            	"host" : {     
                	"href" : ".../api/v1/clusters/c1/hosts/host1"  
            	},
            	“metrics” : {
                	"disk" : {       
                    	"disk_total" : 100000000,       
                    	"disk_free" : 5000000,       
                    	"part_max_used" : 10101     
                	},
                	...
            	},
            	"component" : {
                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE", 
                	“ServiceComponentInfo” : {
                    	"cluster_name" : "c1",         
                    	"component_name" : "NAMENODE",         
                    	"service_name" : "HDFS"       
                	}
            	}  
        	},
        	...
    	]
	}

**Example – Using Partial Response for multi-level expansion of sub-resources**
	
	GET /api/v1/clusters/c1/hosts/host1?fields=host_components/component/*
	
	200 OK
	{
    	“href”: “http://ambari.server/api/v1/clusters/c1/hosts/host1?fields=host_components/*”,
    	“Hosts” : {
        	“cluster_name” : “c1”,
        	“host_name” : “host1”
        	...
    	},
    	“host_components”: [
    		{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”,
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : DATANODE”,
                	“host_name” : “host1”
            	}, 
            	"component" : {
                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE", 
                	“ServiceComponentInfo” : {
                   		"cluster_name" : "c1",         
                    	"component_name" : "DATANODE",         
                    	"service_name" : "HDFS"  
                    	...     
                	},
             		“metrics”: {
                   		“dfs”: {
                       		“datanode” : {
          	                	“blocks_written " :  10000,
          	                	“blocks_read" : 5000,
                             	...
                        	}
                    	},
                    	“disk”: {
                       		"disk_total " :  1000000,
                        	“disk_free" : 50000,
                        	...
                    	},
                   		... 	
					}
            	}
        	},
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”,
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : NAMENODE”,
                	“host_name” : “host1”
            	}, 
            	"component" : {
                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE", 
                	“ServiceComponentInfo” : {
                   		"cluster_name" : "c1",         
                    	"component_name" : "NAMENODE",         
                    	"service_name" : "HDFS"       
                	},
             		“metrics”: {
                    	“dfs”: {
                       		“namenode” : {
          	            		“FilesRenamed " :  10,
          	            		“FilesDeleted" : 5
                         		…
                    		}
						},	
                    	“disk”: {
                       		"disk_total " :  1000000,
                       		“disk_free" : 50000,
                        	...
                    	}
                	},
                	...
            	}
        	},
        	...
    	]
	}

**Example: Using Partial Response to expand collection resource instances one level deep**

	GET /api/v1/clusters/c1/hosts?fields=*

	200 OK
	{
    	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/?fields=*”,    
    	“items”: [ 
        	{
            	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/host1”,
            	“Hosts” : {
                	“cluster_name” :  “c1”,
                	“host_name” : “host1”
            	},
            	“metrics”: {
                	“process”: {          	    
                   		"proc_total" : 1000,
          	       		"proc_run" : 1000
                	},
                	...
            	},
            	“host_components”: [
                	{
                   		“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”
                    	“HostRoles” : {
                       		“cluster_name” : “c1”,
                         	“component_name” : “NAMENODE”,
                        	“host_name” : “host1”
                    	}
                	},
                	{
                    	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
                    	“HostRoles” : {
                       		“cluster_name” : “c1”,
                        	“component_name” : DATANODE”,
                        	“host_name” : “host1”
                    	}
                	},
                	...
            	},
            	...
        	},
        	{
            	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/host2”,
            	“Hosts” : {
                	“cluster_name” :  “c1”,
                	“host_name” : “host2”
            	},
            	“metrics”: {
               		“process”: {          	    
                   		"proc_total" : 555,
          	     		"proc_run" : 55
                	},
                	...
            	},
            	“host_components”: [
                	{
                   		“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
                    	“HostRoles” : {
                       		“cluster_name” : “c1”,
                        	“component_name” : “DATANODE”,
                        	“host_name” : “host2”
                    	}
                	},
                	...
            	],
            	...
        	},
        	...
    	]
	}

### Additional Partial Response Examples

**Example – For each cluster, get cluster name, all hostname’s and all service names**

	GET   /api/v1/clusters?fields=Clusters/cluster_name,hosts/Hosts/host_name,services/ServiceInfo/service_name

**Example - Get all hostname’s for a given component**

	GET	/api/v1/clusters/c1/services/HDFS/components/DATANODE?fields=host_components/HostRoles/host_name

**Example - Get all hostname’s and component names for a given service**

	GET	/api/v1/clusters/c1/services/HDFS?fields=components/host_components/HostRoles/host_name,
                                      	          components/host_components/HostRoles/component_name



Query Predicates
----

Used to limit which data is returned by a query.  This is synonymous to the “where” clause in a SQL query.  Providing query parameters does not result in any link expansion in the data that is returned, with the exception of the fields used in the predicates.  Query predicates can only be applied to collection resources.  A predicate consists of at least one relational expression.  Predicates with multiple relational expressions also contain logical operators, which connect the relational expressions.  Predicates may also use brackets for explicit grouping of expressions. 

### Relational Query Operators

<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>=</td>
    <td>name=host1</td>
    <td>String or numerical EQUALS</td>
  </tr>
  <tr>
    <td>!=</td>
    <td>name!=host1</td>
    <td>String or numerical NOT EQUALS</td>
  </tr>
  <tr>
    <td>&lt;</td>
    <td>disk_total&lt;50</td>
    <td>Numerical LESS THAN</td>
  </tr>
  <tr>
    <td>&gt;</td>
    <td>disk_total&gt;50</td>
    <td>Numerical GREATER THAN</td>
  </tr>
  <tr>
    <td>&lt;=</td>
    <td>disk_total&lt;=50</td>
    <td>Numerical LESS THAN OR EQUALS</td>
  </tr>
  <tr>
    <td>&gt;=</td>
    <td>disk_total&gt;=50</td>
    <td>Numerical GREATER THAN OR EQUALS</td>
  </tr>  
</table>

### Logical Query Operators

<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>|</td>
    <td>name=host1|name=host2</td>
    <td>Logical OR operator</td>
  </tr>
  <tr>
    <td>&</td>
    <td>prop1=foo&prop2=bar</td>
    <td>Logical AND operator</td>
  </tr>
  <tr>
    <td>!</td>
    <td>!prop<50</td>
    <td>Logical NOT operator</td>
  </tr>
</table>

**Logical Operator Precedence**

Standard logical operator precedence rules apply.  The above logical operators are listed in order of precedence starting with the lowest priority.  

### Brackets

<table>
  <tr>
    <th>Bracket</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>(</td>
    <td>Opening Bracket</td>
  </tr>
  <tr>
    <td>)</td>
    <td>Closing Bracket</td>
  </tr>

</table>
  
Brackets can be used to provide explicit grouping of expressions. Expressions within brackets have the highest precedence.

### Operator Functions
 
<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>in()</td>
    <td>name.in(foo,bar)</td>
    <td>IN function.  More compact form of name=foo|name=bar. </td>
  </tr>
  <tr>
    <td>isEmpty()</td>
    <td>category.isEmpty()</td>
    <td>Used to determine if a category contains any properties. </td>
  </tr>
</table>
Operator functions behave like relational operators and provide additional functionality.  Some operator functions, such as in(), act as binary operators like the above relational operators, where there is a left and right operand.  Some operator functions are unary operators, such as isEmpty(), where there is only a single operand.

### Query Examples

**Example – Get all hosts with “HEALTHY” status that have 2 or more cpu**
	
	GET	/api/v1/clusters/c1/hosts?Hosts/host_status=HEALTHY&Hosts/cpu_count>=2
	
**Example – Get all hosts with less than 2 cpu or host status != HEALTHY**
	

	GET	/api/v1/clusters/c1/hosts?Hosts/cpu_count<2|Hosts/host_status!=HEALTHY

**Example – Get all “rhel6” hosts with less than 2 cpu or “centos6” hosts with 3 or more cpu**  

	GET	/api/v1/clusters/c1/hosts?Hosts/os_type=rhel6&Hosts/cpu_count<2|Hosts/os_type=centos6&Hosts/cpu_count>=3

**Example – Get all hosts where either state != “HEALTHY” or last_heartbeat_time < 1360600135905 and rack_info=”default_rack”**

	GET	/api/v1/clusters/c1/hosts?(Hosts/host_status!=HEALTHY|Hosts/last_heartbeat_time<1360600135905)
                                  &Hosts/rack_info=default_rack

**Example – Get hosts with host name of host1 or host2 or host3 using IN operator**
	
	GET	/api/v1/clusters/c1/hosts?Hosts/host_name.in(host1,host2,host3)

**Example – Get and expand all HDFS components, which have at least 1 property in the “metrics/jvm” category (combines query and partial response syntax)**

	GET	/api/v1/clusters/c1/services/HDFS/components?!metrics/jvm.isEmpty()&fields=*

**Example – Update the state of all ‘INSTALLED’ services to be ‘STARTED’**

	PUT /api/v1/clusters/c1/services?ServiceInfo/state=INSTALLED 
    {
      "ServiceInfo": {
        "state" : "STARTED"
      }
    }

Batch Requests
----
Requests can be batched.  This allows for multiple bodies to be specified as an array in a single request. 

**Example – Creating multiple hosts in a single request**
     
    POST /api/v1/clusters/c1/hosts/         

    [
      {
        "Hosts" : {
          "cluster_name" : "c1",
          "host_name" : "host1"
        }
      },
      {
        "Hosts" : {
          "cluster_name" : "c1",
          "host_name" : "host2"
        }
      },
      {
        "Hosts" : {
          "cluster_name" : "c1",
          "host_name" : "host3"
        }
      }
    ]


RequestInfo
----
RequestInfo allows the user to specify additional properties in the body of a request.

<table>
  <tr>
    <th>Key</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>query</td>
    <td>The query string for the request.  Useful for overcoming length limits of the URL and for specifying a query string for each element of a batch request.</td>  
  </tr>
  <tr>
    <td>context</td>
    <td>The context string.  The API client can pass a string context to able to specify what the requests were for (e.g., "HDFS Service Start", "Add Hosts", etc.)</td>  
  </tr>
</table>

### query


The query property allows the user to specify the query string as part of the request body.  This is sometimes required in the case of a very long query string that causes the request to exceed the limits of the URL.

**Example – Specifying the query string in the request body**

    PUT  /clusters/c1/services
    
    {
      "RequestInfo":{
        "query":"ServiceInfo/state=STARTED&ServiceInfo/service_name=HDFS&…"
      },

      "Body":
      {
        "ServiceInfo": {
          "state" : "INSTALLED"
        }
      }
    }
    
The query property can also be applied to the elements of a [batch request](#batch-request).

**Example – Specifying the query string in the request body for a batch request**


    PUT /api/v1/clusters/c1/hosts
    
    [
      {
        "RequestInfo":{
          "query":"Hosts/host_name=host1"
        },
        "Body":
        {
          "Hosts": {
            "desired_config": {
              "type": "global",
              "tag": "version50",
              "properties": { "a": "b", "x": "y" }
            }
          }
        }
      },
      {
        "RequestInfo":{
          "query":"Hosts/host_name=host2"
        },
        "Body":
        {
          "Hosts": {
            "desired_config": {
              "type": "global",
              "tag": "version51",
              "properties": { "a": "c", "x": "z" }
            }
          }
        }
      }
    ]


### context
In some cases a request will return a 202 to indicate that the instruction was accepted by the server (see [asynchronous response](#asynchronous-response)).  In these cases the body of the response contains the ID and href of the request resource that was created to carry out the instruction.  It may be desirable to attach a context string to the request which will then be assigned to the resulting request response.

In the following example a request is made to stop the HDFS service.  Notice that a context is passed as a RequestInfo property.

**Example – Specifying the query string in the request body**

    PUT  /clusters/c1/services
    
    {
      "RequestInfo":{
        "query":"ServiceInfo/state=STARTED&ServiceInfo/service_name=HDFS",
        "context":"Stop HDFS service."
      },

      "Body":
      {
        "ServiceInfo": {
          "state" : "INSTALLED"
        }
      }
    }

**Response**

    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/13",
      "Requests" : {
        "id" : 13,
        "status" : "InProgress"
      }
    }
    
When the request resource returned in the above example is queried, the supplied context string is returned as part of the response.

    GET api/v1/clusters/c1/requests/13 

**Response**


    {
      "href" : "http://ec2-50-19-183-89.compute-1.amazonaws.com:8080/api/v1/clusters/c1/requests/13",
      "Requests" : {
        "id" : 13,
        "cluster_name" : "c1",
        "request_context" : "Stop HDFS service."
      },
      "tasks" : [
        …
      ]
    }
 

Temporal Metrics
----

Some metrics have values that are available across a range in time.  To query a metric for a range of values, the following partial response syntax is used.  

To get temporal data for a single property:
?fields=category/property[start-time,end-time,step]	

To get temporal data for all properties in a category:
?fields=category[start-time,end-time,step]

start-time: Required field.  The start time for the query in Unix epoch time format.
end-time: Optional field, defaults to now.  The end time for the query in Unix epoch time format.
step: Optional field, defaults to the corresponding metrics system’s default value.  If provided, end-time must also be provided. The interval of time between returned data points specified in seconds. The larger the value provided, the fewer data points returned so this can be used to limit how much data is returned for the given time range.  This is only used as a suggestion so the result interval may differ from the one specified.

The returned result is a list of data points over the specified time range.  Each data point is a value / timestamp pair.

**Note**: It is important to understand that requesting large amounts of temporal data may result in severe performance degradation.  **Always** request the minimal amount of information necessary.  If large amounts of data are required, consider splitting the request up into multiple smaller requests.

**Example – Temporal Query for a single property using only start-time**

	GET	/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm/gcCount[1360610225]

	
	200 OK
	{
    	“href” : …/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm/gcCount[1360610225]”,
    	...
    	“metrics”: [
        	{
            	“jvm”: {
          	    	"gcCount" : [
                   		[10, 1360610165],
                     	[12, 1360610180],
                     	[13, 1360610195],
                     	[14, 1360610210],
                     	[15, 1360610225]
                  	]
             	}
         	}
    	]
	}

**Example – Temporal Query for a category using start-time, end-time and step**

	GET	/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm[1360610200,1360610500,100]

	200 OK
	{
    	“href” : …/clusters/c1/hosts/host1?fields=metrics/jvm[1360610200,1360610500,100]”,
    	...
    	“metrics”: [
        	{
            	“jvm”: {
          	    	"gcCount" : [
                   		[10, 1360610200],
                     	[12, 1360610300],
                     	[13, 1360610400],
                     	[14, 1360610500]
                  	],
                	"gcTimeMillis" : [
                   		[1000, 1360610200],
                     	[2000, 1360610300],
                     	[5000, 1360610400],
                     	[9500, 1360610500]
                  	],
                  	...
             	}
         	}
    	]
	}

Pagination
----

It is possible to divide the resources returned for a request up into pages by specifying a page size and offset.

<table>
  <tr>
    <th>Parameter</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>page_size</td>
    <td>The number of resources to be returned for the paged response.</td>  
  </tr>
  <tr>
    <td>from</td>
    <td>The starting page resource (inclusive).  Valid values are :offset | "start"</td>  
  </tr>
  <tr>
    <td>to</td>
    <td>The ending page resource (inclusive).  Valid values are :offset | "end"</td>  
  </tr>
</table>


**Note**: either from or to can be specified, not both.  If neither is specified then 'from=0' is assumed.

The :offset is an integer value that represents an offset (zero based) into the set of resources.  For example, 'from=21' means that the first resource of the response page should be the 21st resource of the resource set.

**Example - Get a page of 10 request resources starting with the 21st**

    /api/v1/clusters/cl1/requests?from=21&page_size=10

The "start" keyword indicates the start of the resource set and is equivalent to an offset of 0.

**Example - Get a page of 10 request resources from the start**

    /api/v1/clusters/cl1/requests?from=start&page_size=10
    
    /api/v1/clusters/cl1/requests?from=0&page_size=10
    


The "end" keyword indicates the end of the set of resources and is equivalent to an offset of -1.

**Example - Get the last 10 request resources**

    /api/v1/clusters/cl1/requests?to=end&page_size=10
    
    /api/v1/clusters/cl1/requests?to=-1&page_size=10


The default ordering of the resources (by the natural ordering of the resource key properties) is implied.	

	
HTTP Return Codes
----

The following HTTP codes may be returned by the API.
<table>
  <tr>
    <th>HTTP CODE</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>200</td>
    <td>OK</td>  
  </tr>
  <tr>
    <td>400</td>
    <td>Bad Request</td>  
  </tr>
  <tr>
    <td>401</td>
    <td>Unauthorized</td>  
  </tr>
  <tr>
    <td>403</td>
    <td>Forbidden</td>  
  </tr> 
  <tr>
    <td>404</td>
    <td>Not Found</td>  
  </tr>
  <tr>
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
</table>


Errors
----

**Example errors responses**

    404 Not Found
	{   
    	"status" : 404,   
    	"message" : "The requested resource doesn't exist: Cluster not found, clusterName=someInvalidCluster" 
	} 

&nbsp;

	400 Bad Request
	{   
    	"status" : 400,   
    	"message" : "The properties [foo] specified in the request or predicate are not supported for the 
                	 resource type Cluster."
	}


