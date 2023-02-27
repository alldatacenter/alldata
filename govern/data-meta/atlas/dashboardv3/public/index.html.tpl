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
    <script type="text/javascript">
    var isHtmlInPath = window.location.pathname.indexOf("index.html");
    if (isHtmlInPath === -1) {
        var pathname = window.location.pathname.replace(/\/[\w-]+.(jsp|html)|\/+$/ig, ''),
            indexpath = pathname + "/index.html";
        if (location.hash.length > 2) {
            indexpath += location.hash;
        }
        window.location.replace(indexpath);
    }
    </script>
    <link rel="shortcut icon" href="img/favicon.ico?bust=<%- bust %>" type="image/x-icon" />
    <link rel="icon" href="img/favicon.ico?bust=<%- bust %>" type="image/x-icon" />
    <!-- Place favicon.ico and apple-touch-icon.png in the root directory -->
    <link rel="stylesheet" type="text/css" href="css/animate.min.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="js/libs/backgrid/css/backgrid.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="js/libs/backgrid-filter/css/backgrid-filter.min.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="js/libs/backgrid-paginator/css/backgrid-paginator.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="js/libs/backgrid-orderable-columns/css/backgrid-orderable-columns.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="js/libs/backgrid-sizeable-columns/css/backgrid-sizeable-columns.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="js/external_lib/backgrid-columnmanager/css/Backgrid.ColumnManager.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="js/libs/select2/css/select2.min.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="js/libs/bootstrap/css/bootstrap.min.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="js/libs/jquery-asBreadcrumbs/css/asBreadcrumbs.min.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="css/googlefonts.css?bust=<%- bust %>" type="text/css" />
    <link rel="stylesheet" type="text/css" href="js/external_lib/jquery-ui/jquery-ui.min.css?bust=<%- bust %>" />
    <link rel="stylesheet" href="css/nv.d3.min.css?bust=<%- bust %>" >
    <link href="css/bootstrap-sidebar.css?bust=<%- bust %>" rel="stylesheet" />
    <link href="js/libs/font-awesome/css/font-awesome.min.css?bust=<%- bust %>" rel="stylesheet" />
    <link href="js/external_lib/pnotify/pnotify.custom.min.css?bust=<%- bust %>" rel="stylesheet" />
    <link href="js/libs/jQueryQueryBuilder/css/query-builder.default.min.css?bust=<%- bust %>" rel="stylesheet" />
    <link href="js/libs/bootstrap-daterangepicker/css/daterangepicker.css?bust=<%- bust %>" rel="stylesheet" />
    <link href="js/libs/dropzone/css/dropzone.css?bust=<%- bust %>" rel="stylesheet">
    <link href="js/libs/jstree/css/default/default-theme.min.css?bust=<%- bust %>" rel="stylesheet" />
    <link href="js/libs/pretty-checkbox/css/pretty-checkbox.min.css?bust=<%- bust %>" rel="stylesheet" />
     <link href="js/external_lib/atlas-lineage/dist/styles.css?bust=<%- bust %>" rel="stylesheet">
    <link href="css/style.css?bust=<%- bust %>" rel="stylesheet" />
</head>

<body>
    <div>
        <div id="header" class="clearfix"></div>
        <div class="container-fluid view-container">
            <div id="sidebar-wrapper" class="col-sm-3 no-padding"></div>
            <div id="page-wrapper" class="col-sm-9 col-sm-offset-3 no-padding">
                <div>
                    <div class="initialLoading"></div>
                </div>
            </div>
        </div>
    </div>
    <div class="module-loader"></div>
    <!-- build:js scripts/main.js -->
    <script data-main="js/main.js?bust=<%- bust %>" src="js/libs/requirejs/require.js?bust=<%- bust %>"></script>
    <!-- endbuild -->
    <script type="text/javascript">
    var getBustValue = function() {
        return "<%- bust %>";
    };
    </script>
</body>

</html>