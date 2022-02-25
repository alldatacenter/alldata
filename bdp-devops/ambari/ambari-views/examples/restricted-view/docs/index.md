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

Restricted View Example
========
Description
-----
The Restricted view is another simple view example.  Like the HelloWorld view example, it demonstrates the basics of how to write and deploy a view in Ambari but also includes a couple of simple resources and a custom view permission.  The Restricted resources use JAX-RS annotations to define the actions that can be performed on the resources.

Package
-----

All views are packaged as a view archive.  The view archive contains the configuration file and various optional components of the view.

#####view.xml

The view.xml file is the only required file for a view archive.  The view.xml is the configuration that describes the view and view instances for Ambari.

      <view>
        <name>RESTRICTED</name>
        <label>The Restricted View</label>
        <version>1.0.0</version>
        <resource>
          <name>restricted</name>
          <service-class>org.apache.ambari.view.restricted.RestrictedResource</service-class>
        </resource>
        <resource>
          <name>unrestricted</name>
          <service-class>org.apache.ambari.view.restricted.UnrestrictedResource</service-class>
        </resource>
        <permission>
          <name>RESTRICTED</name>
          <description>
            Access permission for a restricted view resource.
          </description>
        </permission>
        <instance>
          <name>INSTANCE_1</name>
        </instance>
      </view>

The configuration in this case defines a view named RESTRICTED that has a single instance.  The view also defines a resource named 'unrestricted' and a resource name 'restricted'.



#####UnrestrictedResource.java

The UnrestrictedResource class defines a simple resource for the view.  It uses JAX-RS annotations to define the actions that can be performed on the resource.

    @GET
    @Produces({"text/html"})
    public Response getUnrestricted() throws IOException{
      return Response.ok("<b>You have accessed an unrestricted resource.</b>").type("text/html").build();
    }

The getUnrestricted method will service requests to **'INSTANCE_1/resources/unrestricted'** for INSTANCE_1 of the RESTRICTED view.  

For example ...

     http://<server>:8080/api/v1/views/RESTRICTED/versions/1.0.0/instances/INSTANCE_1/resources/unrestricted

#####RestrictedResource.java

The RestrictedResource class defines a simple resource for the view.  It uses JAX-RS annotations to define the actions that can be performed on the resource.

    @GET
    @Produces({"text/html"})
    public Response getRestricted() throws IOException{
  
      String userName = context.getUsername();
  
      try {
        context.hasPermission(userName, "RESTRICTED");
      } catch (org.apache.ambari.view.SecurityException e) {
        return Response.status(401).build();
      }
  
      return Response.ok("<b>You have accessed a restricted resource.</b>").type("text/html").build();
    }

The getRestricted method will service requests to **'INSTANCE_1/resources/restricted'** for INSTANCE_1 of the RESTRICTED view.  

For example ...

     http://<server>:8080/api/v1/views/RESTRICTED/versions/1.0.0/instances/INSTANCE_1/resources/restricted

Build
-----

The view can be built as a maven project.

    cd ambari-views/examples/restricted-view
    mvn clean package

The build will produce the view archive.

    ambari-views/examples/restricted-view/target/restricted-view-1.0.0.jar


Deploy
-----
To deploy a view we simply place the view archive in the views folder of the ambari-server machine.  By default the views folder is located at ...

    /var/lib/ambari-server/resources/views

To deploy the Restricted view simply copy the restricted-view jar to the ambari-server views folder and restart the ambari server.

Use
-----

After deploying a view you should see it as a view resource in the Ambari REST API.  If we request all views, we should see the RESTRICTED view.

      {
        "href" : "http://<server>:8080/api/v1/views",
        "items" : [
          {
            "href" : "http://<server>:8080/api/v1/views/RESTRICTED",
            "ViewInfo" : {
              "view_name" : "RESTRICTED"
            }
          },
          {
            "href" : "http://<server>:8080/api/v1/views/HELLO_SERVLET",
            "ViewInfo" : {
              "view_name" : "HELLO_SERVLET"
            }
          },
          {
            "href" : "http://<server>:8080/api/v1/views/HELLO_WORLD",
            "ViewInfo" : {
              "view_name" : "HELLO_WORLD"
            }
          }
        ]
      }


If we want to see the details about a specific view, we can ask for it by name.  This shows us that the RESTRICTED view has a single instance named INSTANCE_1.  Note the RESTRICTED permission sub-resource.

    {
      "href" : "http://<server>/api/v1/views/RESTRICTED/versions/1.0.0/",
      "ViewVersionInfo" : {
        "archive" : "/var/lib/ambari-server/resources/views/work/RESTRICTED{1.0.0}",
        "label" : "The Restricted View",
        "masker_class" : null,
        "parameters" : [ ],
        "version" : "1.0.0",
        "view_name" : "RESTRICTED"
      },
      "permissions" : [
        {
          "href" : "http://<server>/api/v1/views/RESTRICTED/versions/1.0.0/permissions/7",
          "PermissionInfo" : {
            "permission_id" : 7,
            "version" : "1.0.0",
            "view_name" : "RESTRICTED"
          }
        }
      ],
      "instances" : [
        {
          "href" : "http://<server>/api/v1/views/RESTRICTED/versions/1.0.0/instances/INSTANCE_1",
          "ViewInstanceInfo" : {
            "instance_name" : "INSTANCE_1",
            "version" : "1.0.0",
            "view_name" : "RESTRICTED"
          }
        }
      ]    
    }

To see a specific instance of a view, we can ask for it by name.  Here we can see the attributes of the view instance.  We can also see that this view has two resources named 'restricted' and 'unrestricted'.  Note that there are no privileges granted on the view instance.

    {
      "href" : "http://<server>/api/v1/views/RESTRICTED/versions/1.0.0/instances/INSTANCE_1/",
      "ViewInstanceInfo" : {
        "context_path" : "/views/RESTRICTED/1.0.0/INSTANCE_1",
        "description" : null,
        "icon64_path" : null,
        "icon_path" : null,
        "instance_name" : "INSTANCE_1",
        "label" : "The Restricted View",
        "version" : "1.0.0",
        "view_name" : "RESTRICTED",
        "visible" : true,
        "instance_data" : { },
        "properties" : { }
      },
      "resources" : [
        {
          "href" : "http://<server>/api/v1/views/RESTRICTED/versions/1.0.0/instances/INSTANCE_1/resources/restricted",
          "instance_name" : "INSTANCE_1",
          "name" : "restricted",
          "version" : "1.0.0",
          "view_name" : "RESTRICTED"
        },
        {
          "href" : "http://<server>/api/v1/views/RESTRICTED/versions/1.0.0/instances/INSTANCE_1/resources/unrestricted",
          "instance_name" : "INSTANCE_1",
          "name" : "unrestricted",
          "version" : "1.0.0",
          "view_name" : "RESTRICTED"
        }
      ],
      "privileges" : [
      ]
    }
    
We can access the view's unrestricted resource through the resource's href.

    http://<server>:8080/api/v1/views/RESTRICTED/versions/1.0.0/instances/INSTANCE_1/resources/unrestricted/
    
    You have accessed an unrestricted resource.


If we try to access the view's restricted resource through the resource's href it should return a 401 Unauthorized response.

    http://<server>:8080/api/v1/views/RESTRICTED/versions/1.0.0/instances/INSTANCE_1/resources/unrestricted/
    
    401 Unauthorized

To grant privileges to access the restricted resource we can create a privilege sub-resource for the view instance.  The following API will grant RESTICTED permission to the user 'bob' for the view instance 'INSTANCE_1' of the 'RESTRICTED' view. 

    POST http://<server>/api/v1/views/RESTRICTED/versions/1.0.0/instances/INSTANCE_1
    
    [
      {
        "PrivilegeInfo" : {
          "permission_name" : "RESTRICTED",
          "principal_name" : "bob",
          "principal_type" : "USER"
        }
      }
    ]

We should now see a privilege sub resource for the view instance and the user 'bob' should be able to access the restricted resource.

    {
      "href" : "http://<server>/api/v1/views/RESTRICTED/versions/1.0.0/instances/INSTANCE_1/privileges/5",
      "PrivilegeInfo" : {
        "instance_name" : "INSTANCE_1",
        "permission_name" : "RESTRICTED",
        "principal_name" : "bob",
        "principal_type" : "USER",
        "privilege_id" : 5,
        "version" : "1.0.0",
        "view_name" : "RESTRICTED"
      }
    }