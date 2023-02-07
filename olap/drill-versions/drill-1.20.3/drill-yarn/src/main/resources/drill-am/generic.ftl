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
<#-- Adapted from the Drill generic.ftl, adjusted for use in the AM. -->

<#macro page_head>
</#macro>

<#macro page_body>
</#macro>

<#macro page_html>
      <meta http-equiv="X-UA-Compatible" content="IE=edge">

      <title>Apache Drill - Application Master</title>
      <link rel="shortcut icon" href="/static/img/drill.ico">

      <link href="/static/css/bootstrap.min.css" rel="stylesheet">
      <link href="/drill-am/static/css/drill-am.css" rel="stylesheet">

      <script src="/static/js/jquery-3.6.1.min.js"></script>
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
            <ul class="nav navbar-nav mr-auto">
              <li class="nav-item"><a class="nav-link" href="/config">Configuration</a></li>
              <li class="nav-item"><a class="nav-link" href="/drillbits">Drillbits</a></li>
              <li class="nav-item"><a class="nav-link" href="/manage">Manage</a></li>
              <li class="nav-item"><a class="nav-link" href="/history">History</a></li>
            </ul>
            <ul class="nav navbar-nav navbar-right">
              <li class="nav-item"><a class="nav-link" href="${docsLink}">Documentation</a>
              <#if showLogin == true >
              <li class="nav-item"><a class="nav-link" href="/login">Log In</a>
              </#if>
              <#if showLogout == true >
              <li class="nav-item"><a class="nav-link" href="/logout">Log Out (${loggedInUserName})</a>
             </#if>
            </ul>
          </div>
        </div>
      </div>

      <div class="container-fluid drill-am" role="main">
        <h3>YARN Application Master &ndash; ${clusterName}</h3>
        <@page_body/>
      </div>
    </body>
  </html>
</#macro>
