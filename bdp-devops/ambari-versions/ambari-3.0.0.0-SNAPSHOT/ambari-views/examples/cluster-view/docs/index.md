<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Cluster View Example
======

Description
-----
The Cluster View is an example of a basic REST service that serves the configuration parameter values.
It demonstrates the basics of view / cluster association and cluster configuration for view instances.
It also shows how to auto-create an instance of a view and automatically associate it with a cluster.

Package
-----
All views are packaged as a view archive. The view archive contains the configuration
file and various optional components of the view.

###view.xml

The view.xml file is the only required file for a view archive.  The view.xml is the configuration that describes the view and view instances for Ambari.

Note the following in the view.xml for the CLUSTER view:

#####cluster-config

Some of the parameter elements in the view.xml are created with a cluster-config element.

    <parameter>
      <name>hdfs_user</name>
      <description>The hdfs_user value from the hadoop-env configuration.  Requires cluster association.</description>
      <label>HDFS User</label>
      <default-value>not available</default-value>
      <cluster-config>hadoop-env/hdfs_user</cluster-config>
    </parameter>

Including a cluster-config element means that the value for the parameter's property will be acquired from cluster configuration if the view instance is associated with a cluster.  
In this example, if an instance of this view is associated with a cluster then the value returned for the 'hdfs_user' property will come from the cluster's 'hadoop-env/hdfs_user' configuration. 

#####auto-instance

The view.xml contains an auto-instance element.

    <auto-instance>
      <name>AUTO_INSTANCE</name>
      <label>Auto Create instance for the CLUSTER view</label>
      <description>This view instance is auto created when the HDFS service is added to a cluster.</description>
      <property>
        <key>setting1</key>
        <value>value1</value>
      </property>
      <property>
        <key>setting2</key>
        <value>value2</value>
      </property>
      <stack-id>HDP-2.*</stack-id>
      <services>
        <service>HDFS</service>
      </services>
    </auto-instance>
    
The auto-instance element describes an instance of the view that will be automatically created when the matching services are added to a cluster with a matching stack id.  
In this example, an instance of the cluster view will be created when the HDFS service is added to a HDP-2.* cluster.    

Build
-----

The view can be built as a maven project.

    cd ambari-views/examples/cluster-view
    mvn clean package

The build will produce the view archive.

    ambari-views/examples/cluster-view/target/cluster-view-???.jar

Deploy
------

Place the view archive on the Ambari Server and start to deploy.    

    cp cluster-view-???.jar /var/lib/ambari-server/resources/views/
    ambari-server start
    

View Instances
-----

When you first start Ambari, you should see an instance of the CLUSTER view named INSTANCE_1.  The instance is defined in the view.xml and is created unconditionally when the view is first deployed.

Access the view instance end point:

    api/v1/views/CLUSTER/versions/0.1.0/instances/INSTANCE_1

Access the view UI:

    /views/CLUSTER/0.1.0/INSTANCE_1

At this point, the instance is not associated with any cluster so accessing the view properties from within the view code should show the default value of 'not available' for the cluster-config properties.

    api/v1/views/CLUSTER/versions/0.1.0/instances/INSTANCE_1/resources/configurations
    
    [{"hdfs_user" : "not available"}, {"proxyuser_group" : "not available"}]

Cluster Association
-----

To associate a view instance with a cluster, you need to have an Ambari managed cluster.  Use the Ambari install wizard to create a cluster that includes the HDFS service.

When the HDFS service is added to the cluster a new instance of the CLUSTER view named AUTO_INSTANCE should be automatically added.
 
Access the view instance end point:
     
     api/v1/views/CLUSTER/versions/0.1.0/instances/AUTO_INSTANCE
     
Note that the view instance is associated with the cluster "c1" through the property 'ViewInstanceInfo/cluster_handle'.
     
     {
       "href" : "http://...:8080/api/v1/views/CLUSTER/versions/0.1.0/instances/AUTO_INSTANCE",
       "ViewInstanceInfo" : {
         "cluster_handle" : "c1",
         "context_path" : "/views/CLUSTER/0.1.0/AUTO_INSTANCE",
         "description" : "This view instance is auto created when the HDFS services are added to a cluster.",
         "icon64_path" : null,
         "icon_path" : null,
         "instance_name" : "AUTO_INSTANCE",
         "label" : "Auto Create instance for the CLUSTER view",
         "static" : false,
         "version" : "0.1.0",
         "view_name" : "CLUSTER",
         "visible" : true,
         "instance_data" : { },
         "properties" : {
           "hdfs_user" : "not available",
           "proxyuser_group" : "not available",
           "setting1" : "value1",
           "setting2" : "value2"
         }
       }, ...
       
Access the view UI:

    /views/CLUSTER/0.1.0/AUTO_INSTANCE
           
You should be able to use the view UI or the API to see the values of the configuration properties.

    api/v1/views/CLUSTER/versions/0.1.0/instances/AUTO_INSTANCE/resources/configurations
           
    [{"hdfs_user" : "hdfs"}, {"proxyuser_group" : "users"}]
           
The values of the properties should match what is in the actual cluster configuration.

To associate the view instance INSTANCE_1 with the cluster, you should set the 'ViewInstanceInfo/cluster_handle' property.
 
    PUT /api/v1/views/CLUSTER/versions/0.1.0/instances/INSTANCE_1
        {
          "ViewInstanceInfo" : {
            "cluster_handle" : "c1"
          }
        }
Once the view is associated with the cluster, you should see the actual configuration values through the view UI.
        
        
           