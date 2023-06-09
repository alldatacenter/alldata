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

<script src="/static/js/datatables.min.js"></script>
<link href="/static/css/datatables.min.css" rel="stylesheet">
<script>
    $(document).ready(function() {
      $.each(["running","completed"], function(i, key) {
        $("#profileList_"+key).DataTable( {
          //Permit sorting-by-column
          "ordering": true,
          "order": [[0, "desc"]],
          "searching": true,
          "paging": true,
          "pagingType": "full_numbers",
          "lengthMenu": [[${model.getQueriesPerPage()}, -1], [${model.getQueriesPerPage()}, "All"]],
          "lengthChange": true,
          "info": true,
          //Ref: https://legacy.datatables.net/ref#sDom
          "sDom": '<"top"lftip><"bottom"><"clear">',
          //Customized info labels
          "language": {
              "lengthMenu": "<b>Display </b>_MENU_<b> profiles per page</b>",
              "zeroRecords": "No matching profiles found!",
              "info": "Showing page _PAGE_ of _PAGES_ ",
              "infoEmpty": "No profiles available",
              "infoFiltered": "(filtered _TOTAL_ from _MAX_)",
              "search": "<b>Search Profiles  </b>"
          }
        } );
      } );
    } );

    //Close the cancellation status popup
    function refreshStatus() {
        $("#queryCancelModal").modal("hide");
        console.log("location.reload(true);")
        location.reload(true);
    }

    //Toggle Selection for canceling running queries
    function toggleRunningSelection(source) {
        let checkboxes = document.getElementsByName('cancelQ');
        let toggleCount = checkboxes.length;
        for(let i=0; i < toggleCount; i++) {
            checkboxes[i].checked = source.checked;
        }
    }

    //Submit Cancellations & show status
    function cancelSelection() {
        let checkedBoxes = document.querySelectorAll('input[name=cancelQ]:checked');
        let checkedCount = checkedBoxes.length;
        if (checkedCount == 0)  return;

        for(var i=0; i < checkedCount; i++) {
            let queryToCancel = checkedBoxes[i].value;
            //Asynchronously cancel the query
            $.get("/profiles/cancel/" + queryToCancel, function(data, status){
                /*Not Tracking Response*/
            });
        }
        document.getElementById("cancelTitle").innerHTML = "Drillbit on " + location.hostname + " says";
        document.getElementById("cancelText").innerHTML = "Issued cancellation for "+checkedCount+" quer"+(checkedCount == 1 ? "y":"ies");
        $("#queryCancelModal").modal("show");
    }

    //Trigger the click event of file input and submit the file selected to the server
    function viewProfile() {
        $("#view-profile-file").change(function() {
            $("#view-profile").submit();
            this.value = null;
        });
        $("#view-profile-file").trigger("click");
    }
</script>

<!-- CSS to control DataTable Elements -->
<style type="text/css" class="init">
  /* Control Padding for length and filter as a pair */
  div.dataTables_length {
    float: right;
    font-weight: normal;
  }
  div.dataTables_filter {
    float: left;
    font-weight: normal;
    padding-left: 0.45em;
  }
  div.dataTables_info {
    padding-right: 2em;
    float: right;
  }

  /* Add spaces between pagination links */
  #profileList_completed_paginate *, #profileList_running_paginate * {
    float:left 
  }
</style>
</#macro>

<#macro page_body>
  <#if (model.getErrors()?size > 0) >
    <div id="message" class="alert alert-danger alert-dismissable">
        <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>
        <strong>Failed to get profiles:</strong><br>
        <#list model.getErrors() as error>
          ${error}<br>
        </#list>
    </div>
  </#if>
  <#if (model.getRunningQueries()?size > 0) >
    <h3>Running Queries 
    <div  style="display: inline-block; line-height:2" >
      <button type="button" class="btn btn-warning btn-sm" onClick="cancelSelection()">
      Cancel Selected</button>
    </div></h3>
    <!-- Cancellation Modal -->
    <div class="modal fade" id="queryCancelModal" role="dialog">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" onclick="refreshStatus()">&times;</button>
            <h4 class="modal-title" id="cancelTitle"></h4>
          </div>
          <div class="modal-body" style="line-height:2" ><h3 id="cancelText">Cancellation Status</h3></div>
          <div class="modal-footer"><button type="button" class="btn btn-secondary" onclick="refreshStatus()">Close</button></div>
        </div>
      </div>
    </div>

    <@list_queries queries=model.getRunningQueries() stateList="running" />
    <div class="pb-2 mt-4 mb-2 border-bottom">
    </div>
  <#else>
    <div id="message" class="alert alert-info alert-dismissable">
      <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>
      <strong>No running queries.</strong>
    </div>
  </#if>

  <#include "*/alertModals.ftl">

  <table width="100%">
    <script type="text/javascript" language="javascript">
    //Validate that the fetch number is valid
    function checkMaxFetch() {
      var maxFetch = document.forms["profileFetch"]["max"].value;
      if (isNaN(maxFetch) || (maxFetch < 1) || (maxFetch > 100000) ) {
        let alertValues = {'_fetchSize_': maxFetch };
        populateAndShowAlert('invalidProfileFetchSize', alertValues);
        $("#fetchMax").focus();
        return false;
      }
      return true;
    }
    </script>
    <tr>
      <td><h3>Completed Queries</h3></td>
      <td align="right">
        <form name="profileFetch" action="/profiles" onsubmit="return checkMaxFetch();" method="get"><span title="Max number of profiles to load">Loaded <b>${model.getFinishedQueries()?size}</b> profiles </span>
        <input id="fetchMax" type="text" size="5" name="max" value="" style="text-align: right" />
        <input type="submit" value="Reload"/>
      </form></td>
      <td align="right" width="1px">
        <form id="view-profile" action="/profiles/view" enctype='multipart/form-data' method="post">
        <input type="file" id="view-profile-file" name="profileData" style="display: none"/>
        <input type="button" value="View" onclick = "viewProfile()"/>
      </form></td>
    </tr></table>
    <!-- Placed after textbox to allow for DOM to contain "fetchMax" element -->
    <script type="text/javascript" language="javascript">
    //Get max fetched from URL for populating textbox
    var maxFetched="${model.getMaxFetchedQueries()}";
    if (window.location.search.indexOf("max=") >= 1) {
      //Select 1st occurrence (Chrome accepts 1st of duplicates)
      var kvPair=window.location.search.substr(1).split('&')[0];
      maxFetched=kvPair.split('=')[1]
    }
    //Update textbox
    $(document).ready(function() {
            $("#fetchMax").val(maxFetched);
    });
    </script>
  <@list_queries queries=model.getFinishedQueries() stateList="completed" />
</#macro>

<#macro list_queries queries stateList>
    <div class="table-responsive">
        <table id="profileList_${stateList}" class="table table-hover table-striped table-bordered sortable dataTable" role="grid">
            <thead class="">
            <tr role="row">
                <#if stateList == "running" >
                <th><input type="checkbox" name="selectToggle" onClick="toggleRunningSelection(this)" /></th>
                </#if>
                <th>Time</th>
                <th>User</th>
                <th>Query</th>
                <th>State</th>
                <th>Duration</th>
                <th>Foreman</th>
            </tr>
            </thead>
            <tbody>
            <#list queries as query>
            <tr>
                <#if stateList == "running" >
                <td><input type="checkbox" name="cancelQ" value="${query.getQueryId()}"/></td>
                </#if>
                <td data-order='${query.getStartTime()}'>${query.getTime()}</td>
                <td>${query.getUser()}</td>
                <td>
                    <a href="/profiles/${query.getQueryId()}">
                        <div style="height:100%;width:100%;white-space:pre-line">${query.getQuery()}</div>
                    </a>
                </td>
                <td>${query.getState()}</td>
                <td data-order='${query.getEndTime() - query.getStartTime()}'>${query.getDuration()}</td>
                <td>${query.getForeman()}</td>
            </tr>
            </#list>
            </tbody>
        </table>
    </div>
    <div style="padding-bottom: 2em;"/>
</#macro>

<@page_html/>