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
<script src="/static/js/d3.v3.js"></script>
<script src="/static/js/dagre-d3.min.js"></script>
<script src="/static/js/graph.js"></script>
<script src="/static/js/datatables.min.js"></script>
<script src="/static/js/jquery.form.js"></script>
<script src="/static/js/querySubmission.js"></script>
<!-- Ace Libraries for Syntax Formatting -->
<script src="/static/js/ace-code-editor/ace.js" type="text/javascript" charset="utf-8"></script>
<!-- Disabled in favour of dynamic: script src="/static/js/ace-code-editor/mode-sql.js" type="text/javascript" charset="utf-8" -->
<script src="/dynamic/mode-sql.js" type="text/javascript" charset="utf-8"></script>
<script src="/static/js/ace-code-editor/ext-language_tools.js" type="text/javascript" charset="utf-8"></script>
<script src="/static/js/ace-code-editor/theme-sqlserver.js" type="text/javascript" charset="utf-8"></script>
<script src="/static/js/ace-code-editor/snippets/sql.js" type="text/javascript" charset="utf-8"></script>
<script src="/static/js/ace-code-editor/mode-snippets.js" type="text/javascript" charset="utf-8"></script>
<link href="/static/css/drill-dataTables.sortable.css" rel="stylesheet">

<script>
    var globalconfig = {
        "queryid" : "${model.getQueryId()}"
    };

    $(document).ready(function() {
      $(".sortable").DataTable({
        "searching": false,
        "lengthChange": false,
        "paging": false,
        "info": false
      });
      //Enable Warnings by making it visible
      checkForWarnings();
    });

    //Check for Warnings
    function checkForWarnings() {
      //No Progress Warning
      let noProgressFragmentCount = document.querySelectorAll('td[class=no-progress-tag]').length;
      let majorFragmentCount = document.querySelectorAll('#fragment-overview table tbody tr').length;
      if (majorFragmentCount > 0) { // For fast-failed queries majorFragmentCount=0
        toggleWarning("noProgressWarning", majorFragmentCount, noProgressFragmentCount);
      }

      //Spill To Disk Warnings
      let spillCount = document.querySelectorAll('td[class=spill-tag]').length;
      toggleWarning("spillToDiskWarning", true, (spillCount > 0));

      //Slow Scan Warnings
      let longScanWaitCount = document.querySelectorAll('td[class=scan-wait-tag]').length;
      toggleWarning("longScanWaitWarning", true, (longScanWaitCount > 0));
    }

    //Show Warnings
    function toggleWarning(warningElemId, expectedVal, actualVal) {
        if (expectedVal == actualVal) {
            document.getElementById(warningElemId).style.display="block";
        } else {
            closeWarning(warningElemId);
        }
    }

    //Close Warning
    function closeWarning(warningElemId) {
        document.getElementById(warningElemId).style.display="none";
    }

    //Injects Estimated Rows
    function injectEstimatedRows() {
      Object.keys(opRowCountMap).forEach(key => {
        var tgtElem = $("td.estRowsAnchor[key='" + key + "']");
        var status = tgtElem.append("<div class='estRows' title='Estimated'>(" + opRowCountMap[key] + ")</div>");
      });
    }

    //Toggle Estimates' visibility
    function toggleEstimates(tgtColumn) {
      var colClass = '.est' + tgtColumn;
      var estimates = $(colClass);
      if (estimates.filter(":visible").length > 0) {
        $(colClass).each(function () {
          $(this).attr("style", "display:none");
        });
      } else {
        $(colClass).each(function () {
          $(this).attr("style", "display:block");
        });
      }
    }

    //Close the cancellation status popup
    function refreshStatus() {
      //Close PopUp Modal
      $("#queryCancelModal").modal("hide");
      location.reload(true);
    }

    //Cancel query & show cancellation status
    function cancelQuery() {
      document.getElementById("cancelTitle").innerHTML = "Drillbit on " + location.hostname + " says";
      $.get("/profiles/cancel/" + globalconfig.queryid, function(data, status){/*Not Tracking Response*/});
      //Show PopUp Modal
      $("#queryCancelModal").modal("show");
    };

</script>
</#macro>

<#macro page_body>

   <#include "*/alertModals.ftl">
   <#include "*/runningQuery.ftl">

  <h3>Query and Planning</h3>
  <ul id="query-tabs" class="nav nav-tabs" role="tablist">
    <li class="nav-item"><a href="#query-query" role="tab" data-toggle="tab" class="nav-link">Query</a></li>
    <li class="nav-item"><a href="#query-physical" role="tab" data-toggle="tab" class="nav-link">Physical Plan</a></li>
    <li class="nav-item"><a href="#query-visual" role="tab" data-toggle="tab" class="nav-link">Visualized Plan</a></li>
    <li class="nav-item"><a href="#query-edit" role="tab" data-toggle="tab" class="nav-link">Edit Query</a></li>
    <#if model.hasError()>
        <li class="nav-item"><a href="#query-error" role="tab" data-toggle="tab" class="nav-link">Error</a></li>
    </#if>
  </ul>
  <div id="query-content" class="tab-content">
    <div id="query-query" class="tab-pane" style="background-color: #ffffff">
      <p><pre id="query-text" class="border" name="query-text"  style="background-color: #f5f5f5; font-family:courier,monospace">${model.getProfile().query}</pre></p>
      <#if model.hasAutoLimit()>
          <div name="autoLimitWarning" style="cursor:help" class="card bg-warning" title="WebUI Queries allow restricting the number of rows returned to avoid the server to hold excessive number of results rows in memory.&#10;This helps maintain server stability with faster response if the entire resultset will not be visualized in the browser">
            <div class="card-header">
            <b>WARNING:</b> Query result was <b>automatically</b> limited to <span style="font-style:italic;font-weight:bold">${model.getAutoLimit()} rows</span>
            </div>
          </div>
      </#if>
    </div>
    <div id="query-physical" class="tab-pane">
      <p><pre class="bg-light border p-2">${model.profile.plan}</pre></p>
    </div>
    <div id="query-visual" class="tab-pane">
      <div style='padding: 15px 15px;'>
        <button type='button' class='btn btn-light' onclick='popUpAndPrintPlan();'><span class="material-icons">print</span> Print Plan</button>
      </div>
      <div>
        <svg id="query-visual-canvas" class="d-block mx-auto"></svg>
      </div>
    </div>
    <div id="query-edit" class="tab-pane">
      <p>

       <#-- DRILL-7697: merge with copy in query.ftl -->
       <form role="form" id="queryForm" action="/query" method="POST">
          <#if model.isOnlyImpersonationEnabled()>
            <div class="form-group">
              <label for="userName">User Name</label>
              <input type="text" size="30" name="userName" id="userName" placeholder="User Name" value="${model.getProfile().user}">
            </div>
          </#if>
          <div class="form-group">
            <div class="container-fluid">
              <div class="row">
                <label class="font-weight-bold" for="queryType">Query type:&nbsp;&nbsp;</label>
                <div class="form-check">
                  <label class="font-weight-bold">
                    <input type="radio" name="queryType" id="sql" value="SQL" checked>
                    SQL
                  </label>
                </div>
                <div class="form-check">
                  <label class="font-weight-bold">
                    <input type="radio" name="queryType" id="physical" value="PHYSICAL">
                    Physical
                  </label>
                </div>
                <div class="form-check">
                  <label class="font-weight-bold">
                    <input type="radio" name="queryType" id="logical" value="LOGICAL">
                    Logical
                  </label>
                </div>
              </div>
            </div>

            <div class="form-group">
              <div style="display: inline-block"><label class="font-weight-bold" for="query">Query</label></div>
              <div style="display: inline-block; float:right; padding-right:5%"><b>Hint: </b>Use
                <div id="keyboardHint" style="display:inline-block; font-style:italic"></div> to submit</div>
              <div id="query-editor" class="border">${model.getProfile().query}</div>
              <input class="form-control" id="query" name="query" type="hidden" value="${model.getProfile().query}"/>
            </div>

            <div>
              <button class="btn btn-primary" type="button" id="rerunButton" onclick="<#if model.isOnlyImpersonationEnabled()>doSubmitQueryWithUserName()<#else>doSubmitQueryWithAutoLimit()</#if>">
                Re-run query
              </button>
              &nbsp;&nbsp;&nbsp;
              <input type="checkbox" name="forceLimit" value="limit" <#if model.hasAutoLimit()>checked</#if>>
                Limit results to <input type="text" id="autoLimit" name="autoLimit" min="0"
                  value="<#if model.hasAutoLimit()>${model.getAutoLimit()?c}<#else>${model.getDefaultAutoLimit()?c}</#if>" size="6" pattern="[0-9]*">
                  rows <span data-toggle="tooltip" class="material-icons" title="Limits the number of records retrieved in the query.
                  Ignored if query has a limit already" style="cursor: help;">info</span>
              &nbsp;&nbsp;&nbsp;
              Default schema: <input type="text" size="10" name="defaultSchema" id="defaultSchema">
                <span data-toggle="tooltip" class="material-icons" title="Set the default schema used to find table names
                and for SHOW FILES and SHOW TABLES." style="cursor: help;">info</span>
           </div>
            <input type="hidden" name="csrfToken" value="${model.getCsrfToken()}">
          </div>
          </form>
      </p>

      <p>
      <form action="/profiles/cancel/${model.queryId}" method="GET">
        <div class="form-group">
          <button type="submit" class="btn btn-warning">Cancel query</button>
        </div>
      </form>
        </p>
       <#include "*/runningQuery.ftl">
    </div>
    <#if model.hasError()>
      <div id="query-error" class="tab-pane fade">
        <p><pre class="bg-light border p-2">${model.getProfile().error?trim}</pre></p>
        <p>Failure node: ${model.getProfile().errorNode}</p>
        <p>Error ID: ${model.getProfile().errorId}</p>

        <div class="pb-2 mt-4 mb-2 border-bottom"></div>
        <h3>Verbose Error Message</h3>
        <div class="card">
          <div class="card-header">
            <h4>
              <a class="text-secondary"data-toggle="collapse" href="#error-message-overview">
                 Overview
              </a>
            </h4>
          </div>
          <div id="error-message-overview" class="collapse">
            <div class="card-body">
              <pre class="bg-light border p-2">${model.getProfile().verboseError?trim}</pre>
            </div>
          </div>
        </div>
      </div>
    </#if>

  </div>

  <#assign queueName = model.getProfile().getQueueName() />
  <#assign queued = queueName != "" && queueName != "-" />

  <div class="pb-2 mt-4 mb-2 border-bottom"></div>
  <h3>Query Profile: <span style='font-size:85%'>${model.getQueryId()}</span>
  <#if model.getQueryStateDisplayName() == "Prepared" || model.getQueryStateDisplayName() == "Planning" || model.getQueryStateDisplayName() == "Enqueued" || model.getQueryStateDisplayName() == "Starting" || model.getQueryStateDisplayName() == "Running">
    <div  style="display: inline-block;">
      <button type="button" id="cancelBtn" class="btn btn-warning btn-sm" onclick="cancelQuery()" > Cancel </button>
    </div>

  <!-- Cancellation Modal -->
  <div class="modal fade" id="queryCancelModal" role="dialog">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <button type="button" class="close" data-dismiss="modal" onclick="refreshStatus()">&times;</button>
          <h4 class="modal-title" id="cancelTitle"></h4>
        </div>
        <div class="modal-body" style="line-height:2">
          Cancellation issued for Query ID:<br>${model.getQueryId()}
        </div>
        <div class="modal-footer"><button type="button" class="btn btn-secondary" onclick="refreshStatus()">Close</button></div>
      </div>
    </div>
  </div>
  </#if>
  </h3>

  <div id="query-profile-accordion">
    <div class="card">
      <div class="card-header">
        <h4>
          <a class="text-secondary" data-toggle="collapse" href="#query-profile-overview">
              Overview
          </a>
        </h4>
      </div>
      <div id="query-profile-overview" class="collapse show">
        <div class="card-body">
          <table class="table table-bordered">
            <thead>
            <tr>
                <th>State</th>
                <th>Foreman</th>
                <th>Total Fragments</th>
     <#if queued>
                <th>Total Cost</th>
                <th>Queue</th>
     </#if>
            </tr>
            </thead>
            <tbody>
              <tr>
                  <td>${model.getQueryStateDisplayName()}</td>
                  <td>${model.getProfile().getForeman().getAddress()}</td>
                  <td>${model.getProfile().getTotalFragments()}</td>
     <#if queued>
                  <td>${model.getProfile().getTotalCost()}</td>
                  <td>${queueName}</td>
     </#if>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h4>
          <a class="text-secondary" data-toggle="collapse" href="#query-profile-duration">
             Duration
          </a>
        </h4>
      </div>
      <div id="query-profile-duration" class="collapse show">
        <div class="card-body">
          <table class="table table-bordered">
            <thead>
              <tr>
                <th>Planning</th>
     <#if queued>
                <th>Queued</th>
     </#if>
                <th>Execution</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>${model.getPlanningDuration()}</td>
     <#if queued>
                <td>${model.getQueuedDuration()}</td>
     </#if>
                <td>${model.getExecutionDuration()}</td>
                <td>${model.getProfileDuration()}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>

  <#assign sessionOptions = model.getSessionOptions()>
  <#assign queryOptions = model.getQueryOptions()>
  <#if (sessionOptions?keys?size > 0 || queryOptions?keys?size > 0) >
    <div class="pb-2 mt-4 mb-2 border-bottom"></div>
    <h3>Options</h3>
    <div id="options-accordion">
      <div class="card">
        <div class="card-header">
          <h4>
            <a class="text-secondary" data-toggle="collapse" href="#options-overview">
              Overview
            </a>
          </h4>
        </div>
        <div id="options-overview" class="collapse show">
          <@list_options options=sessionOptions scope="Session" />
          <@list_options options=queryOptions scope="Query" />
        </div>
      </div>
    </div>
  </#if>

  <div class="pb-2 mt-4 mb-2 border-bottom"></div>
  <h3>Fragment Profiles</h3>

  <div id="fragment-accordion">
    <div class="card">
      <div class="card-header">
        <h4>
          <a class="text-secondary"data-toggle="collapse" href="#fragment-overview">
            Overview
          </a>
        </h4>
      </div>
      <div id="fragment-overview" class="collapse">
        <div class="card-body">
          <svg id="fragment-overview-canvas" class="d-block mx-auto"></svg>
          <div id="noProgressWarning" style="display:none;cursor:help" class="card bg-warning">
            <div class="card-header" title="Check if any of the Drillbits are waiting for data from a SCAN operator, or might actually be hung with its VM thread being busy." style="cursor:pointer">
            <span class="material-icons" style="font-size:125%">warning</span> <b>WARNING:</b> No fragments have made any progress in the last <b>${model.getNoProgressWarningThreshold()}</b> seconds. (See <span style="font-style:italic;font-weight:bold">Last Progress</span> below)
            </div>
          </div>
          ${model.getFragmentsOverview()?no_esc}
        </div>
      </div>
      <#list model.getFragmentProfiles() as frag>
      <div class="card">
        <div class="card-header">
          <h4>
            <a class="text-secondary"data-toggle="collapse" href="#${frag.getId()}">
              ${frag.getDisplayName()}
            </a>
          </h4>
        </div>
        <div id="${frag.getId()}" class="collapse">
          <div class="card-body">
            ${frag.getContent()?no_esc}
          </div>
        </div>
      </div>
      </#list>
    </div>
  </div>

  <div class="pb-2 mt-4 mb-2 border-bottom"></div>
  <h3>Operator Profiles
 <button onclick="toggleEstimates('Rows')" class="btn btn-light" style="font-size:60%; float:right">Show/Hide Estimated Rows</button></h3>

 <style>
  .estRows {
    color:navy;
    font-style:italic;
    font-size: 80%;
    display:<#if model.showEstimatedRows()>block<#else>none</#if>;
  }
</style>
  <div id="operator-accordion">
    <div class="card">
      <div class="card-header">
        <h4>
          <a class="text-secondary"data-toggle="collapse" href="#operator-overview">
            Overview
          </a>
        </h4>
      </div>
      <div id="operator-overview" class="collapse">
        <div class="card-body">
      <#if model.hasAutoLimit()>
          <div name="autoLimitWarning" style="cursor:help" class="card bg-warning" title="WebUI Queries allow restricting the number of rows returned to avoid the server to hold excessive number of results rows in memory.&#10;This helps maintain server stability with faster response if the entire resultset will not be visualized in the browser">
            <div class="card-header">
            <b>WARNING:</b> Query result was <b>automatically</b> limited to <span style="font-style:italic;font-weight:bold">${model.getAutoLimit()} rows</span>
            </div>
          </div>
      </#if>
          <div id="spillToDiskWarning" style="display:none;cursor:help" class="card bg-warning" title="Spills occur because a buffered operator didn't get enough memory to hold data in memory. Increase the memory or ensure that number of spills &lt; 2">
            <div class="card-header"><span class="material-icons" style="font-size:125%">warning</span> <b>WARNING:</b> Some operators have data spilled to disk. This will result in performance loss. (See <span style="font-style:italic;font-weight:bold">Avg Peak Memory</span> and <span style="font-style:italic;font-weight:bold">Max Peak Memory</span> below)
            <button type="button" class="close" onclick="closeWarning('spillToDiskWarning')" style="font-size:180%">&times;</button>
            </div>
          </div>
          <div id="longScanWaitWarning" style="display:none;cursor:help" class="card bg-warning">
            <div class="card-header" title="Check if any of the Drillbits are waiting for data from a SCAN operator, or might actually be hung with its VM thread being busy." style="cursor:pointer">
            <span class="material-icons" style="font-size:125%">warning</span> <b>WARNING:</b> Some of the SCAN operators spent more time waiting for the data than processing it. (See <span style="font-style:italic;font-weight:bold">Avg Wait Time</span> as compared to <span style="font-style:italic;font-weight:bold">Average Process Time</span> for the <b>SCAN</b> operators below)
            <button type="button" class="close" onclick="closeWarning('longScanWaitWarning')" style="font-size:180%">&times;</button>
            </div>
          </div>
          ${model.getOperatorsOverview()?no_esc}
        </div>
      </div>
    </div>

    <#list model.getOperatorProfiles() as op>
    <div class="card">
      <div class="card-header">
        <h4>
          <a class="text-secondary"data-toggle="collapse" href="#${op.getId()}">
            ${op.getDisplayName()}
          </a>
        </h4>
      </div>
      <div id="${op.getId()}" class="collapse">
        <div class="card-body">
          ${op.getContent()?no_esc}
        </div>
        <div class="card">
          <div class="card-header bg-info">
            <h4>
              <a class="text-white"data-toggle="collapse" href="#${op.getId()}-metrics">
                Operator Metrics
              </a>
            </h4>
          </div>
          <div id="${op.getId()}-metrics" class="collapse">
            <div class="card-body" style="display:block;overflow-x:auto">
              ${op.getMetricsTable()?no_esc}
            </div>
          </div>
        </div>
      </div>
    </div>
    </#list>
  </div>

  <div class="pb-2 mt-4 mb-2 border-bottom"></div>
  <h3>Full JSON Profile</h3>

  <div class="span4 collapse-group" id="full-json-profile">
    <a class="btn btn-light" data-toggle="collapse" data-target="#full-json-profile-json">JSON profile</a>
    <br> <br>
    <pre class="collapse bg-light border p-2" id="full-json-profile-json">
    </pre>
  </div>
  <div class="pb-2 mt-4 mb-2 border-bottom">
  </div> <br>

    <script>
    // Enable the tooltip
    $(function () {
      var options = { delay: { "show" : 100, "hide" : 100 } };
      $('[data-toggle="tooltip"]').tooltip(options);
    });
    //Inject Spilled Tags
    $(window).on('load', function () {
      injectIconByClass("spill-tag","get_app");
      injectIconByClass("time-skew-tag","schedule");
      injectSlowScanIcon();
      //Building RowCount
      buildRowCountMap();
      injectEstimatedRows();
    });

    //Inject Glyphicon by Class tag
    function injectIconByClass(tagLabel, tagIcon) {
        //Inject Spill icons
        var tagElemList = document.getElementsByClassName(tagLabel);
        var i;
        for (i = 0; i < tagElemList.length; i++) {
            var content = tagElemList[i].innerHTML;
            tagElemList[i].innerHTML = "<span class=\"material-icons\">" + tagIcon + "</span>" + content;
        }
    }

    //Inject PNG icon for slow
    function injectSlowScanIcon() {
        //Inject Spill icons
        var tagElemList = document.getElementsByClassName("scan-wait-tag");
        var i;
        for (i = 0; i < tagElemList.length; i++) {
            var content = tagElemList[i].innerHTML;
            tagElemList[i].innerHTML = "<img src='/static/img/turtle.png' alt='slow'> " + content;
        }
    }

    //Configuration for Query Viewer in Profile
    ace.require("ace/ext/language_tools");
    var viewer = ace.edit("query-text");
    viewer.setAutoScrollEditorIntoView(true);
    viewer.setOption("minLines", 3);
    viewer.setOption("maxLines", 20);
    viewer.renderer.setShowGutter(false);
    viewer.renderer.setOption('showLineNumbers', false);
    viewer.renderer.setOption('showPrintMargin', false);
    viewer.renderer.setOption("vScrollBarAlwaysVisible", true);
    viewer.renderer.setOption("hScrollBarAlwaysVisible", true);
    viewer.renderer.setScrollMargin(10, 10, 10, 10);
    viewer.getSession().setMode("ace/mode/sql");
    viewer.setTheme("ace/theme/sqlserver");
    //CSS Formatting
    document.getElementById('query-query').style.fontSize='13px';
    document.getElementById('query-query').style.fontFamily='courier,monospace';
    document.getElementById('query-query').style.lineHeight='1.5';
    document.getElementById('query-query').style.margin='auto';
    viewer.resize();
    viewer.setReadOnly(true);
    viewer.setOptions({
      enableBasicAutocompletion: false,
      enableSnippets: false,
      enableLiveAutocompletion: false
    });

    //Configuration for Query Editor in Profile
    ace.require("ace/ext/language_tools");
    var editor = ace.edit("query-editor");
    //Hidden text input for form-submission
    var queryText = $('input[name="query"]');
    editor.getSession().on("change", function () {
      queryText.val(editor.getSession().getValue());
    });
    editor.setAutoScrollEditorIntoView(true);
    editor.setOption("maxLines", 16);
    editor.setOption("minLines", 10);
    editor.renderer.setShowGutter(true);
    editor.renderer.setOption('showLineNumbers', true);
    editor.renderer.setOption('showPrintMargin', false);
    editor.renderer.setOption("vScrollBarAlwaysVisible", true);
    editor.renderer.setOption("hScrollBarAlwaysVisible", true);;
    editor.renderer.setScrollMargin(10, 10, 10, 10);
    editor.getSession().setMode("ace/mode/sql");
    editor.getSession().setTabSize(4);
    editor.getSession().setUseSoftTabs(true);
    editor.setTheme("ace/theme/sqlserver");
    editor.$blockScrolling = "Infinity";
    //CSS Formatting
    document.getElementById('query-editor').style.fontSize='13px';
    document.getElementById('query-editor').style.fontFamily='courier,monospace';
    document.getElementById('query-editor').style.lineHeight='1.5';
    document.getElementById('query-editor').style.margin='auto';
    document.getElementById('query-editor').style.backgroundColor='#ffffff';
    editor.setOptions({
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: false
    });

    //Pops out a new window and provide prompt to print
    var popUpAndPrintPlan = function() {
      var srcSvg = $('#query-visual-canvas');
      var screenRatio=0.9;
      let printWindow = window.open('', 'PlanPrint', 'width=' + (screenRatio*screen.width) + ',height=' + (screenRatio*screen.height));
      printWindow.document.writeln($(srcSvg).parent().html());
      printWindow.print();
    };

    //Provides hint based on OS
    var browserOS = navigator.platform.toLowerCase();
    if ((browserOS.indexOf("mac") > -1)) {
      document.getElementById('keyboardHint').innerHTML="Meta+Enter";
    } else {
      document.getElementById('keyboardHint').innerHTML="Ctrl+Enter";
    }

    // meta+enter / ctrl+enter to submit query
    document.getElementById('queryForm')
            .addEventListener('keydown', function(e) {
      if (!(e.keyCode == 13 && (e.metaKey || e.ctrlKey))) return;
      if (e.target.form)
        <#if model.isOnlyImpersonationEnabled()>doSubmitQueryWithUserName()<#else>doSubmitQueryWithAutoLimit()</#if>;
    });

    // Extract estimated rowcount map
    var opRowCountMap = {};
    // Get OpId-Rowocunt Map
    function buildRowCountMap() {
      var phyText = $('#query-physical').find('pre').text();
      var opLines = phyText.split("\n");
      opLines.forEach(line => {
        if (line.trim().length > 0) {
          var opId = line.match(/\d+-\d+/g)[0];
          var opRowCount = line.match(/rowcount = ([^,]+)/)[1];
          opRowCountMap[opId] = Number(opRowCount).toLocaleString('en');
        }
      });
    }
    </script>

</#macro>

<#macro list_options options scope>
 <#if (options?keys?size > 0) >
   <div class="card-body">
     <h4>${scope} Options</h4>
     <table id="${scope}_options_table" class="table table-bordered">
       <thead>
         <tr>
           <th>Name</th>
           <th>Value</th>
         </tr>
       </thead>
         <tbody>
           <#list options?keys as name>
             <tr>
               <td>${name}</td>
               <td>${options[name]}</td>
             </tr>
           </#list>
         </tbody>
     </table>
   </div>
 </#if>
</#macro>

<@page_html/>
