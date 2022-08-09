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

Property Validator View Example
======

Description
-----
The Property Validator View is an example of a basic view that validates configuration parameter values.
It demonstrates the very basics of how to use the Validator.

The view.xml specifies a Validator class to use for property validation. This class is packaged with the view
and implements the [org.apache.ambari.view.validation.Validator](https://github.com/apache/ambari/blob/trunk/ambari-views/src/main/java/org/apache/ambari/view/validation/Validator.java)
interface. When a view instance is created, or updated, the validator class is called where
the view performs any special configuration property validation. The result of that validation is returned as a
[org.apache.ambari.view.validation.ValidationResult](https://github.com/apache/ambari/blob/trunk/ambari-views/src/main/java/org/apache/ambari/view/validation/ValidationResult.java)

    <validator-class>org.apache.ambari.view.property.MyValidator</validator-class>

Package
-----
All views are packaged as a view archive. The view archive contains the configuration
file and various optional components of the view.

#####view.xml

The view.xml file is the only required file for a view archive.  The view.xml is the configuration that describes the view and view instances for Ambari.

Build
-----

The view can be built as a maven project.

    cd ambari-views/examples/property-validator-view
    mvn clean package

The build will produce the view archive.

    target/property-validator-view-0.1.0.jar

Place the view archive on the Ambari Server and restart to deploy.    

    cp property-validator-view-0.1.0.jar /var/lib/ambari-server/resources/views/
    ambari-server restart
    
Create View Instance
-----

With the view deployed, from the Ambari Administration interface,
create an instance of the view (called P_1) to be used by Ambari users.

Access the view service end point:

    /api/v1/views/PROPERTY-VALIDATOR/versions/0.1.0/instances/P_1/resources/properties

Access the view UI:

    /views/PROPERTY-VALIDATOR/0.1.0/P_1
