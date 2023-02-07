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
    <script>
    //Alter System Values
    function alterSysOption(optionName, optionValue, optionKind) {
        var currHref = location.href;
        var redirectHref = currHref.replace(/(.?filter=).*/,"");
        //Read filter value and apply to reload with new filter
        var reApplyFilter = $("#searchBox").val();
        if (reApplyFilter != null && reApplyFilter.trim().length > 0) {
            redirectHref = redirectHref + "?filter=" + reApplyFilter.trim();
        } else { //Apply filter for updated field
            redirectHref = redirectHref + "?filter=" + optionName;
        }
        $.post("/option/"+optionName, {
          kind: optionKind,
          name: optionName,
          value: optionValue,
          csrfToken: "${model.getCsrfToken()}"
        }, function () {
            //Remove existing filters
            location.href=redirectHref;
        });
    }

    //Read Values and apply
    function alterSysOptionUsingId(optionRawName) {
        //Escaping '.' for id search
        let optionName = optionRawName.replace(/\./gi, "\\.");
        //Extracting datatype from the form
        let optionKind = $("#"+optionName+" input[name='kind']").attr("value");
        //Extracting value from the form's INPUT element
        let optionValue = $("#"+optionName+" input[name='value']").val();
        if (optionKind === "BOOLEAN") {
            //Extracting boolean value from the form's SELECT element (since this is a dropdown input)
            optionValue = $("#"+optionName+" select[name='value']").val();
        } else if (optionKind !== "STRING") { //i.e. it is a number (FLOAT/DOUBLE/LONG)
            if (isNaN(optionValue)) {
                let actualOptionName=optionName.replace(/\\\./gi, ".");
                let alertValues = {'_numericOption_': optionValue, '_optionName_': actualOptionName };
                populateAndShowAlert('invalidOptionValue', alertValues);
                $("#"+optionName+" input[name='value']").focus();
                return;
            }
        }
        alterSysOption(optionRawName, optionValue, optionKind);
    }
    </script>
    <!-- List of Option Descriptions -->
    <script src="/dynamic/options.describe.js"></script>
    <link href="/static/css/datatables.min.css" rel="stylesheet">
    <link href="/static/css/drill-dataTables.sortable.css" rel="stylesheet">
</#macro>

<#macro page_body>
  <div class="container-fluid px-0">
    <div class="row">
      <div class="input-group input-sm col-4" >
        <input id="searchBox" name="searchBox" class="form-control" type="text" value="" placeholder="Search options...">
          <div class="input-group-btn">
            <button class="btn btn-light" type="button" onclick="$('#searchBox').val('').focus();" title="Clear search" style="font-weight:bold">&times;</button>
          </div>
      </div>
      <div class="btn-group btn-group-sm">
        <button type="button" class="btn btn-light" style="cursor:default;font-weight:bold;" > Quick Filters </button>
          <#list model.getFilters() as filter>
            <button type="button" class="btn btn-info" onclick="inject(this.innerHTML);">${filter}</button>
          </#list>
      </div>
    </div>
  </div>
  <#include "*/alertModals.ftl">
  <div class="table-responsive">
    <table id='optionsTbl' class="table table-striped table-condensed display sortable" style="table-layout: auto; width=100%;">
      <thead>
        <tr>
          <th style="width:30%">OPTION</th>
          <th style="width:25%">VALUE</th>
          <th style="width:45%">DESCRIPTION</th>
        </tr>
      </thead>
      <tbody><#assign i = 1><#list model.getOptions() as option>
          <tr id="row-${i}">
            <td style="font-family:Courier New; vertical-align:middle" id='optionName'>${option.getName()}</td>
            <td>
              <form class="form-inline" role="form" id="${option.getName()}">
                <div class="form-group form-row col-12 p-0">
                <input type="hidden" class="form-control" name="kind" value="${option.getKind()}">
                <input type="hidden" class="form-control" name="name" value="${option.getName()}">
                  <div class="input-group input-sm col-12">
                  <#if option.getKind() == "BOOLEAN" >
                  <select class="form-control" name="value">
                    <option value="false" ${(option.getValueAsString() == "false")?string("selected", "")}>false</option>
                    <option value="true" ${(option.getValueAsString() == "true")?string("selected", "")}>true</option>
                  </select>
                  <#else>
                    <input type="text" class="form-control" placeholder="${option.getValueAsString()}" name="value" value="${option.getValueAsString()}">
                  </#if>
                    <div class="input-group-append">
                      <button class="btn btn-primary" type="button" onclick="alterSysOptionUsingId('${option.getName()}')">Update</button>
                      <button class="btn btn-success" type="button" onclick="alterSysOption('${option.getName()}','${option.getDefaultValue()}', '${option.getKind()}')" <#if option.getDefaultValue() == option.getValueAsString()>disabled="true" style="pointer-events:none" <#else>
                      title="Reset to ${option.getDefaultValue()}"</#if>>Default</button>
                    </div>
                  </div>
                </div>
              </form>
            </td>
            <td id='description'></td>
          </tr>
          <#assign i = i + 1>
        </#list>
      </tbody>
    </table>
  </div>
  <script>
   //Defining the DataTable with a handle
   var optTable = $('#optionsTbl').DataTable( {
        "lengthChange": false,
         "pageLength": -1,
        "dom": 'lrit',
        "jQueryUI" : true,
        "searching": true,
        "language": {
            "lengthMenu": "Display _MENU_ records per page",
            "zeroRecords": "No matching options found. Check search entry",
            "info": "Found _END_ matches out of _MAX_ options",
            "infoEmpty": "No options available",
            "infoFiltered": ""
        }
      });

    //Draw when the table is ready
    $(document).ready(function() {
      //Inject Descriptions for table
      let size = $('#optionsTbl tbody tr').length;
      for (i = 1; i <= size; i++) {
        let currRow = $("#row-"+i);
        let optionName = currRow.find("#optionName").text();
        let setOptDescrip = currRow.find("#description").text(getDescription(optionName));
      }

      // Draw DataTable
      optTable.rows().invalidate().draw();

      //Re-Inject Filter keyword here
      let explicitFltr = "";
      if (window.location.search.indexOf("filter=") >= 1) {
        //Select 1st occurrence (Chrome accepts 1st of duplicates)
        let kvPair=window.location.search.substr(1).split('&')[0];
        explicitFltr=kvPair.split('=')[1]
        inject(explicitFltr);
      }
    });

    //EventListener to update table when changes are detected
    $('#searchBox').on('keyup focus change', function () {
      optTable.search(this.value).draw().toString();
    });

    //Inject word and force table redraw
    function inject(searchTerm) {
      $('#searchBox').val(searchTerm);
      optTable.search(searchTerm).draw().toString();
    }
  </script>
</#macro>
<@page_html/>
