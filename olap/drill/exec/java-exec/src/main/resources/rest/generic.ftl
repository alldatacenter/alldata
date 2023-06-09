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
<#macro page_head>
</#macro>

<#macro page_body>
</#macro>

<#macro page_html>
  <!DOCTYPE html>
  <html lang="en">
    <head>
      <meta charset="utf-8">
      <meta http-equiv="X-UA-Compatible" content="IE=edge">

      <title>Apache Drill</title>
      <link rel="shortcut icon" href="/static/img/drill.ico">

      <link href="/static/css/bootstrap.min.css" rel="stylesheet">
      <link href="/static/css/drillStyle.css" rel="stylesheet">

      <script type="text/javascript" language="javascript" src="/static/js/jquery-3.6.1.min.js"></script>
      <script src="/static/js/popper.min.js"></script>
      <script src="/static/js/bootstrap.min.js"></script>

      <!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
      <!--[if lt IE 9]>
        <script src="/static/js/html5shiv.js"></script>
        <script src="/static/js/1.4.2/respond.min.js"></script>
      <![endif]-->

      <@page_head/>
    </head>

    <body role="document">

      <div class="navbar navbar-dark bg-dark fixed-top navbar-expand" role="navigation">
        <div class="container-fluid">
          <div class="navbar-header">
            <button type="button" class="navbar-toggler" data-toggle="collapse" data-target=".navbar-collapse">
              <span class="sr-only">Toggle navigation</span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="/">Apache Drill</a>
          </div>
          <div class="navbar-collapse collapse">
            <#if showControls == true>
            <ul class="nav navbar-nav mr-auto">
              <li class="nav-item"><a class="nav-link" href="/query">Query</a></li>
              <li class="nav-item"><a class="nav-link" href="/profiles">Profiles</a></li>
              <#if showStorage == true>
              <li class="nav-item"><a class="nav-link" href="/storage">Storage</a></li>
              </#if>
              <li class="nav-item"><a class="nav-link" href="/metrics">Metrics</a></li>
              <#if showThreads == true>
              <li class="nav-item"><a class="nav-link" href="/threads">Threads</a></li>
              </#if>
              <#if showLogs == true>
                  <li class="nav-item"><a class="nav-link" href="/logs">Logs</a></li>
              </#if>
            </ul>
            </#if>
            <ul class="nav navbar-nav navbar-right">
              <#if showOptions == true>
              <li class="nav-item"><a class="nav-link" href="/options">Options</a></li>
              </#if>
              <li class="nav-item"><a class="nav-link" href="http://drill.apache.org/docs/">Documentation</a>
              <#if showLogin == true >
              <li class="nav-item"><a class="nav-link" href="/mainLogin">Log In</a>
              </#if>
              <#if showLogout == true >
              <li class="nav-item"><a class="nav-link" href="/logout">Log Out (${loggedInUserName})</a>
              </#if>
            </ul>
          </div>
        </div>
      </div>

      <div class="container-fluid" role="main">
        <@page_body/>
      </div>
    </body>
  </html>
</#macro>
