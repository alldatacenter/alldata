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
<#include "*/generic.ftl">
<#macro page_head>
</#macro>

<#macro page_body>
  <p align="right">Auto Refresh <span class="badge badge-pill badge-info" id="refreshTimeLabel"></span>
    <button id="clippy" type="button" class="btn btn-light btn-sm" onClick="copyThreads();" title="Copy To Clipboard">
      <span class="material-icons" style="font-size:180%">file_copy</span>
    </button>
  </p>
  <div id="mainDiv" role="main">
  </div>
  </div>
  <script>
    var statusUrl = location.protocol + "//" + location.host + "/status/threads";
    var refreshInterval = 3;
    document.getElementById('refreshTimeLabel').innerHTML = refreshInterval+" sec"; 
    var update = function() {
      $.get(statusUrl, function(data) {
        $("#mainDiv").html("<pre class=\"bg-light border p-2\">" + data + "</pre>");
      });
    };

    //Ref: https://stackoverflow.com/a/36640126/8323038
    function copyThreads() {
      if (document.selection) { 
        var range = document.body.createTextRange();
        range.moveToElementText(document.getElementById("mainDiv"));
        range.select().createTextRange();
        document.execCommand("copy");
      } else if (window.getSelection) {
        var range = document.createRange();
        range.selectNode(document.getElementById("mainDiv"));
        window.getSelection().addRange(range);
        document.execCommand("copy");
      }
    }
    update();
    setInterval(update, refreshInterval*1000);
  </script>
</#macro>

<@page_html/>