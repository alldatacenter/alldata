<!DOCTYPE html>
<!--
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 7]>
    <script src="js/external_lib/es5-shim.min.js?bust=<%- bust %>"></script>
    <script src="js/external_lib/respond.min.js?bust=<%- bust %>"></script>
<![endif]-->
<!--[if gt IE 8]><!-->
<html class="no-js">
<!--<![endif]-->

<head>
    <meta charset="utf-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8;" />
    <title>Atlas</title>
    <meta name="description" content="" />
    <meta name="viewport" content="width=device-width" />
    <link rel="shortcut icon" href="img/favicon.ico?bust=<%- bust %>" type="image/x-icon" />
    <link rel="icon" href="img/favicon.ico?bust=<%- bust %>" type="image/x-icon" />
    <!-- Place favicon.ico and apple-touch-icon.png in the root directory -->
    <link rel="stylesheet" type="text/css" href="css/animate.min.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="js/libs/backgrid/css/backgrid.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="js/libs/bootstrap/css/bootstrap.min.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="css/googlefonts.css?bust=<%- bust %>" type="text/css" />
    <link href="js/libs/font-awesome/css/font-awesome.min.css?bust=<%- bust %>" rel="stylesheet" />
    <link href="css/migration-style.css?bust=<%- bust %>" rel="stylesheet" />
</head>

<body>
    <div class="page-wrapper">
        <div class="initialLoading"></div>
    </div>
    <!-- build:js scripts/main.js -->
    <script data-main="js/migration.js?bust=<%- bust %>" src="js/libs/requirejs/require.js?bust=<%- bust %>"></script>
    <!-- endbuild -->
    <script type="text/javascript">
    var getBustValue = function() {
        return "<%- bust %>";
    };
    </script>
</body>

</html>