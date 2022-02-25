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

# View Resources

##### Get Views
The user may query for all of the deployed views.  Note that views are a top level resources (they are not sub-resources of a cluster).

    GET http://<server>:8080/api/v1/views/

    {
      "href" : "http://<server>:8080/api/v1/views/",
      "items" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES",
          "ViewInfo" : {
            "view_name" : "FILES"
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

##### Get View
To get a specific view, request it by name.  Note that the response shows all of the versions of the view as sub-resources.

    GET http://<server>:8080/api/v1/views/FILES/
    
    {
      "href" : "http://<server>:8080/api/v1/views/FILES/",
      "ViewInfo" : {
        "view_name" : "FILES"
      },
      "versions" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0",
          "ViewVersionInfo" : {
            "version" : "0.1.0",
            "view_name" : "FILES"
          }
        }
      ]
    }


##### Get Versions
The user may request all of the versions of a named view.

    GET http://<server>:8080/api/v1/views/FILES/versions/
    
    {
      "href" : "http://<server>:8080/api/v1/views/FILES/versions/",
      "items" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0",
          "ViewVersionInfo" : {
            "version" : "0.1.0",
            "view_name" : "FILES"
          }
        }
      ]
    }

##### Get Version
A specific version can be requested.  Note that all of the instances of the view version are included in the response.

    GET http://<server>:8080/api/v1/views/FILES/versions/0.1.0/
    
    {
      "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/",
      "ViewVersionInfo" : {
        "archive" : "/var/lib/ambari-server/resources/views/work/FILES{0.1.0}",
        "label" : "Files",
        "parameters" : [
          {
            "name" : "dataworker.defaultFs",
            "description" : "FileSystem URI",
            "required" : true
          },
          {
            "name" : "dataworker.username",
            "description" : "The username (defaults to ViewContext username)",
            "required" : false
          }
        ],
        "version" : "0.1.0",
        "view_name" : "FILES"
      },
      "instances" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1",
          "ViewInstanceInfo" : {
            "instance_name" : "FILES_1",
            "version" : "0.1.0",
            "view_name" : "FILES"
          }
        }
      ]
    }

##### Get Instances
The user may request all of the instances of a view.

    GET http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/
    
    {
      "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/",
      "items" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1",
          "ViewInstanceInfo" : {
            "instance_name" : "FILES_1",
            "version" : "0.1.0",
            "view_name" : "FILES"
          }
        }
      ]
    }


##### Get Instance
A specific view instance may be requested by specifying its name.  Note that the instance resources are listed as sub-resources.

    GET http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1
    
    {
      "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1",
      "ViewInstanceInfo" : {
        "context_path" : "/views/FILES/0.1.0/FILES_1",
        "instance_name" : "FILES_1",
        "version" : "0.1.0",
        "view_name" : "FILES",
        "instance_data" : { },
        "properties" : {
          "dataworker.defaultFs" : "hdfs://<server>:8020"
        }
      },
      "resources" : [
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1/resources/files",
          "instance_name" : "FILES_1",
          "name" : "files",
          "version" : "0.1.0",
          "view_name" : "FILES"
        }
      ]
    }
    
##### Create Instance
New view instances may be created through the API.

    POST http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_2
    
##### Update Instance
The properties of a view instance may be updated through the API.

    PUT http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_2
    [{
      "ViewInstanceInfo" : {
          "properties" : {
            "dataworker.defaultFs" : "hdfs://MyServer:8020"
          }
        }
    }]

##### Delete Instance
A view instances may be deleted through the API.

    DELETE http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_2
    

##### Resources
A view resource may be accessed through the REST API.  The href for each view resource can be found in a request for the parent view instance.  The resource endpoints and behavior depends on the implementation of the JAX-RS annotated service class specified for the resource element in the view.xml. 

    GET http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/FILES_1/resources/files/fileops/listdir?path=%2F
    
    [{"path":"/app-logs","replication":0,"isDirectory":true,"len":0,"owner":"yarn","group":"hadoop","permission":"-rwxrwxrwx","accessTime":0,"modificationTime":1400006792122,"blockSize":0},{"path":"/mapred","replication":0,"isDirectory":true,"len":0,"owner":"mapred","group":"hdfs","permission":"-rwxr-xr-x","accessTime":0,"modificationTime":1400006653817,"blockSize":0},{"path":"/mr-history","replication":0,"isDirectory":true,"len":0,"owner":"hdfs","group":"hdfs","permission":"-rwxr-xr-x","accessTime":0,"modificationTime":1400006653822,"blockSize":0},{"path":"/tmp","replication":0,"isDirectory":true,"len":0,"owner":"hdfs","group":"hdfs","permission":"-rwxrwxrwx","accessTime":0,"modificationTime":1400006720415,"blockSize":0},{"path":"/user","replication":0,"isDirectory":true,"len":0,"owner":"hdfs","group":"hdfs","permission":"-rwxr-xr-x","accessTime":0,"modificationTime":1400006610050,"blockSize":0}]

##### Managed Resources
Any managed resources for a view may also be accessed through the REST API.  Note that instances of a managed resource appear as sub-resources of an instance under the plural name specified for the resource in the view.xml.  In this example, a list of managed resources named **'scripts'** is included in the response for the view instance … 
 
    GET http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1
    
    {
      "href" : "http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1",
      "ViewInstanceInfo" : {
        "context_path" : "/views/PIG/0.1.0/INSTANCE_1",
        "instance_name" : "INSTANCE_1",
        "version" : "0.1.0",
        "view_name" : "PIG",
        "instance_data" : { },
        "properties" : {
          … 
        }
      },
      "resources" : [ ],
      "scripts" : [
        {
          "href" : "http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1/scripts/script1",
          "id" : "script1",
          "instance_name" : "INSTANCE_1",
          "version" : "0.1.0",
          "view_name" : "PIG"
        },
        {
          "href" : "http://<server>:8080/api/v1/views/FILES/versions/0.1.0/instances/INSTANCE_1/scripts/script2",
          "id" : "script2",
          "instance_name" : "INSTANCE_1",
          "version" : "0.1.0",
          "view_name" : "PIG"
        },
        …
      ]
    }
    
 
To get a single managed resource …
    
    
    GET http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1/scripts/script1
    
    
    {
      "href" : "http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1/scripts/script1",
      "id" : "script1",      
      "pigScript" : "… ",
      "pythonScript" : "… ",
      "templetonArguments" : "… ",
      "dateCreated" : … ,
      "owner" : "… ",
      "instance_name" : "INSTANCE_1",
      "version" : "0.1.0",
      "view_name" : "PIG"
    }
    
Because the resource is managed through the Ambari API framework, it may be queried using partial response and query predicates.  For example …
 
    GET http://<server>:8080/api/v1/views/PIG/versions/0.1.0/instances/INSTANCE_1/scripts?fields=pythonScript&owner=jsmith
    

