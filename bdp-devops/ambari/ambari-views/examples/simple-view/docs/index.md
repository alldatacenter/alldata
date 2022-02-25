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

Simple View Example
======

Description
-----
The Simple View is an example of a basic REST service that serves the configuration parameter value,
and a single HTML page that shows the value. It demonstrates the very basics of how
to write and expose a REST service which works with instance parameters, and the basics of
how to include UI assets with your view that access that service.

Package
-----
All views are packaged as a view archive. The view archive contains the configuration
file and various optional components of the view.

#####view.xml

The view.xml file is the only required file for a view archive.  The view.xml is the configuration that describes the view and view instances for Ambari.
The view.xml for this example defines one required parameter for configuring a view instance: what.is.the.value. The view also
defines a single resource that exposes a REST service at the /simple end point.

    <view>
      <name>SIMPLE</name>
      <label>Simple</label>
      <version>0.1.0</version>
      <parameter>
        <name>what.is.the.value</name>
        <description>Provide a configuration value</description>
        <required>true</required>
      </parameter>
      <resource>
        <name>simple</name>
        <service-class>org.apache.ambari.view.simple.SimpleService</service-class>
      </resource>
    </view>

Build
-----

The view can be built as a maven project.

    cd ambari-views/examples/simple-view
    mvn clean package

The build will produce the view archive.

    ambari-views/examples/simple-view/target/simple-view-0.1.0.jar

Place the view archive on the Ambari Server and restart to deploy.    

    cp simple-view-0.1.0.jar /var/lib/ambari-server/resources/views/
    ambari-server restart
    
Create View Instance
-----

With the view deployed, create an instance of the view to be used by Ambari users.

    POST
    /api/v1/views/SIMPLE/versions/0.1.0/instances/SIMPLE_1
    
    [ {
      "ViewInstanceInfo" : {
        "label" : "Simple 1",
        "description" : "Enter a configuration value",
        "properties" : {
          "what.is.the.value" : "blue"
        }
      }
    } ]

Access the view service end point:

    /api/v1/views/SIMPLE/versions/0.1.0/instances/SIMPLE_1/resources/simple

Access the view UI:

    /views/SIMPLE/0.1.0/SIMPLE_1
