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

Favorite View Example
======

Description
-----
The Favoriate View is a very simple view example. It demonstrates the very basics of how
to write and expose a REST service which works with instance parameters and instance data.

Package
-----
All views are packaged as a view archive. The view archive contains the configuration
file and various optional components of the view.

#####view.xml

The view.xml file is the only required file for a view archive.  The view.xml is the configuration that describes the view and view instances for Ambari.
The view.xml for this example defines two required parameters for configuring a view instance: what.is.the.question and i.do.not.know. The view also
defines a single resource that exposes a REST service at the /favorite end point.

    <view>
      <name>FAVORITE</name>
      <label>Favorite</label>
      <version>1.0.0</version>
      <parameter>
        <name>what.is.the.question</name>
        <description>Ask a question</description>
        <required>true</required>
      </parameter>
      <parameter>
        <name>i.do.not.know</name>
        <description>If you do not know</description>
        <required>true</required>
      </parameter>
      <resource>
        <name>favorite</name>
        <service-class>org.apache.ambari.view.favorite.FavoriteService</service-class>
      </resource>
    </view>

Build
-----

The view can be built as a maven project.

    cd ambari-views/examples/favorite-view
    mvn clean package

The build will produce the view archive.

    ambari-views/examples/favorite-view/target/favorite-view-1.0.0.jar

Place the view archive on the Ambari Server and restart to deploy.    

    cp favorite-view-1.0.0.jar /var/lib/ambari-server/resources/views/
    ambari-server restart
    
Create View Instance
-----

With the view deployed, create an instance of the view to be used by Ambari users.

    POST
    /api/v1/views/FAVORITE/versions/1.0.0/instances/FAVCOLOR
    
    [ {
      "ViewInstanceInfo" : {
        "label" : "Colors",
        "description" : "Choose your favorite color",
        "properties" : {
          "what.is.the.question" : "What is your favorite color?",
          "i.do.not.know" : "green"
        }
      }
    } ]

Access the view service end point:

    /api/v1/views/FAVORITE/versions/1.0.0/instances/FAVCOLOR/resources/favorite

To get the favorite color:

    GET
    /api/v1/views/FAVORITE/versions/1.0.0/instances/FAVCOLOR/resources/favorite/item

To set the favorite color:

    POST
    /api/v1/views/FAVORITE/versions/1.0.0/instances/FAVCOLOR/resources/favorite/item/blue
