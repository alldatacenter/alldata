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
    <script type="text/javascript" language="javascript"  src="/static/js/datatables.min.js"> </script>
    <link href="/static/css/datatables.min.css" rel="stylesheet">
    <style>
      /* See comments above DataTable initialisation. */
      .dataTables_scroll
      {
          overflow:auto;
      }
    </style>
</#macro>

<#macro page_body>
  <div>
  <div class="container-fluid px-0 py-1">
    <div class="row">
      <div class="table-responsive col-10">
        <button type="button"  title="Open in new window" onclick="popOutProfile('${model.getQueryId()}');" class="btn btn-light btn-sm">
          <b>Query Profile:</b> ${model.getQueryId()} <#switch model.getQueryState()>
            <#case "COMPLETED">
          <span class="badge badge-success">
        <#break>
              <#case "CANCELED">
        <span class="badge badge-warning">
        <#break>
            <#case "FAILED">
        <span class="badge badge-danger">
        <#break>
            <#default>
        <span class="badge badge-light">
    </#switch>${model.getQueryState()}</span>&nbsp;&nbsp;&nbsp;<span class="material-icons">open_in_new</span></button>
      </div>
      <div class="table-responsive col-2">
        <div class="input-group">
          <div class="input-group-prepend">
            <span class="input-group-text" style="font-size:95%">Delimiter </span>
          </div>
          <input id="delimitBy" type="text" class="form-control input-sm" name="delimitBy" title="Specify delimiter" placeholder="Required" maxlength="2" size="2" value=",">
          <button type="button"  title="Export visible table as CSV. Show ALL rows to export entire resultSet" onclick="exportTableAsCsv('${model.getQueryId()}');" class="btn btn-light btn-sm">
            <b>Export </b> <span class="material-icons">import_export</span>
          </button>
        </div>
      </div>
    </div>
  </div>
  </div>
  <#if model.isEmpty()>
    <div class="jumbotron">
      <p class="lead">No result found.</p>
    </div>
  <#else>
    <table id="result" class="table table-striped table-bordered table-condensed" style="table-layout: auto; width=100%; white-space: pre;">
      <thead>
        <tr>
          <#list model.getColumns() as value>
          <th>${value}</th>
          </#list>
        </tr>
      </thead>
      <tbody>
      <#list model.getRows() as record>
        <tr>
          <#list record as value>
          <td>${value!"null"}</td>
          </#list>
        </tr>
      </#list>
      </tbody>
    </table>
  </#if>
  <script charset="utf-8">
    // DataTable's scrollX causes misalignment when colvis is used to remove
    // columns until the table's width becomes smaller than the page's so we
    // use our own { overflow: auto } div instead.
    $(document).ready(function() {
      $('#result').dataTable( {
        "aaSorting": [],
        "scrollX" : false,
        "lengthMenu": [[${model.getRowsPerPageValues()},-1], [${model.getRowsPerPageValues()},"ALL"]],
        "lengthChange": true,
        "dom": "Blfrtip",
        "buttons": [ "colvis", "spacer" ],
        "language": {
              "infoEmpty": "No records to show <#if model.isResultSetAutoLimited()> [NOTE: Results are auto-limited to max ${model.getAutoLimitedRowCount()} rows]</#if>",
              "info": "Showing _START_ to _END_ of _TOTAL_ entries <#if model.isResultSetAutoLimited()>[<b>NOTE:</b> Results are auto-limited to max ${model.getAutoLimitedRowCount()} rows]</#if>"
        }
      } );
      jQuery('.dataTable').wrap('<div class="dataTables_scroll" />');
    } );

    //Pop out profile (needed to avoid losing query results)
    function popOutProfile(queryId) {
      var profileUrl = location.protocol+'//'+ location.host+'/profiles/'+queryId;
      var tgtWindow = '_blank';
      window.open(profileUrl, tgtWindow);
    }

    //Ref: https://jsfiddle.net/gengns/j1jm2tjx/
    function downloadCsv(csvRecords, filename) {
      var csvFile;
      var downloadElem;

      //CSV File
      csvFile = new Blob([csvRecords], {type: "text/csv"});
      // Download link
      downloadElem = document.createElement("a");
      // File name
      downloadElem.download = filename;

      // We have to create a link to the file
      downloadElem.href = window.URL.createObjectURL(csvFile);

      // Make sure that the link is not displayed
      downloadElem.style.display = "none";

      // Add the link to your DOM
      document.body.appendChild(downloadElem);

      // Launch the download prompt
      downloadElem.click();
    }

    function exportTableAsCsv(queryId) {
      var filename = queryId + '.csv';
      var csv = []; //Array of records
      var rows = document.getElementById('result').querySelectorAll("tr");
      var delimiter = document.getElementById('delimitBy').value;
      if (delimiter == 'undefined' || delimiter.length==0) {
        delimiter = ",";
      }
      for (var i = 0; i < rows.length; i++) {
        var row = [], cols = rows[i].querySelectorAll("th, td");
        for (var j = 0; j < cols.length; j++)
          row.push(cols[j].textContent);
          csv.push(row.join(delimiter));
        }
        // Download CSV
        downloadCsv(csv.join("\n"), filename);
    }

    </script>
</#macro>

<@page_html/>
