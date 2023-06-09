<#--

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

-->
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>Apache Drill - Application Master</title>
    <META http-equiv="refresh" content="0;URL=${amLink}">
    <style>
      body { font-family: sans-serif;
             text-align: center; }
    </style> 
  </head>
  <body>
    <h3>YARN Application Master &ndash; ${clusterName}</h3>   
    <h4>Redirect</h4>
    The Drill Application Master UI does not work correctly inside the proxy page
    provided by YARN.
    <p>Click
    <a href="${amLink}">here</a> to go to the Application Master directly
    if this page does not automatically redirect you.
  </body>
</html>
  