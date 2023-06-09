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

<#-- Format comma-delimited string-->
<#macro format_string str>
  <#if str?has_content>
    ${(str?split(","))?join(", ")}
  <#else>
    ${"<empty>"}
  </#if>
</#macro>

<#macro page_head>
<style>
.list-value {
    text-align: right !important;
}
</style>
</#macro>

<#macro page_body>
  <#if (model.getMismatchedVersions()?size > 0)>
    <div id="message" class="alert alert-danger alert-dismissable">
      <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>
      <strong>Drill does not support clusters containing a mix of Drillbit versions.
          Current drillbit version is ${model.getCurrentVersion()}.
          One or more drillbits in cluster have different version:
          ${model.getMismatchedVersions()?join(", ")}.
      </strong>
    </div>
  </#if>

  <#include "*/confirmationModals.ftl">

  <div class="row">
    <div class="col-md-12">
      <h3><span id="sizeLabel">Drillbits <span class="badge badge-primary" id="size">${model.getDrillbits()?size}</span>
          <button type="button" class="btn btn-warning" id='reloadBtn' style="display:none;font-size:50%" title='New Drillbits detected! Click to Refresh' onclick='location.reload();'>
          <span class="material-icons">refresh</span></button></h3>
      <div class="table-responsive">
        <table class="table table-hover" id="bitTable">
          <thead>
            <tr>
              <th>#</th>
              <th data-toggle="tooltip" title="Drillbits in Cluster" style="cursor: help;">Address <span class="material-icons" style="font-size:100%">info</span></th>
              <th data-toggle="tooltip" title="Heap Memory in use (as percent of Total Heap)" style="cursor: help;">Heap Memory Usage <span class="material-icons" style="font-size:100%">info</span></th>
              <th data-toggle="tooltip" title="Estimated Direct Memory ACTIVELY in use (as percent of Peak Usage)" style="cursor: help;">Direct Memory Usage <span class="material-icons" style="font-size:100%">info</span></th>
              <th data-toggle="tooltip" title="Current CPU usage by Drill" style="cursor: help;">CPU Usage <span class="material-icons" style="font-size:100%">info</span></th>
              <th data-toggle="tooltip" title="Average load on the system in the last 1 minute" style="cursor: help;">Avg Sys Load <span class="material-icons" style="font-size:100%">info</span></th>
              <th>User Port</th>
              <th>Control Port</th>
              <th>Data Port</th>
              <th>Version</th>
              <th>Status</th>
              <th>Uptime</th>
              <th style="display:${(model.isAuthEnabled() && !model.shouldShowAdminInfo())?then("none","table-cell")}">Shutdown</th>
            </tr>
          </thead>
          <tbody>
            <#assign i = 1>
            <#list model.getDrillbits() as drillbit>
              <tr id="row-${i}">
                <td>${i}</td>
                <td id="address" >${drillbit.getAddress()}<#if drillbit.isCurrent()>
                    <span class="badge badge-info" id="current">Current</span>
                    <#else>
                      <a onclick="popOutRemoteDbitUI('${drillbit.getAddress()}','${drillbit.getHttpPort()}');" style="cursor:pointer;color:blue" title="Open in new window"><span class="material-icons">open_in_new</span></a>
                  </#if>
                </td>
                <td id="httpPort" style="display:none">${drillbit.getHttpPort()}</td>
                <td class="heap">Not Available</td>
                <td class="direct">Not Available</td>
                <td class="bitload"><span class="badge badge-info" id="NA">Not Available</span></td>
                <td class="avgload"><span class="badge badge-info" id="NA">Not Available</span></td>
                <td id="port">${drillbit.getUserPort()}</td>
                <td>${drillbit.getControlPort()}</td>
                <td>${drillbit.getDataPort()}</td>
                <td>
                  <span class="badge
                    <#if drillbit.isVersionMatch()>badge-success<#else>badge-danger</#if>">
                    ${drillbit.getVersion()}
                  </span>
                </td>
                <td id="status" >${drillbit.getState()}</td>
                <td class="uptime" >Not Available</td>
                <td>
                <#if (model.isAuthEnabled() && !model.shouldShowAdminInfo())>
                      <button class='btn btn-danger' type="button" id="shutdown" disabled="true" style="opacity:0.5;cursor:not-allowed;display:none" onClick="" comment="Placeholder. Hidden by default">
                <#else>
                      <button class='btn btn-danger' type="button" id="shutdown" disabled="true" style="opacity:0.5;cursor:not-allowed;display:inline" onClick="shutdown($(this));" title="Not available for remote Drillbits">
                </#if>
                      <span class="material-icons">power_settings_new</span></button>
                </td>
                <td id="queriesCount">  </td>
              </tr>
              <#assign i = i + 1>
            </#list>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <div class="row">
      <div class="col-md-12">
        <h3>Encryption</h3>
        <div class="table-responsive">
          <table class="table table-hover" style="width: auto;">
            <tbody>
                <tr>
                  <td>Client to Bit Encryption</td>
                  <td class="list-value">${model.isUserEncryptionEnabled()?string("Enabled", "Disabled")}
                    <#if model.isBitEncryptionEnabled()><span class="material-icons">lock</span></#if>
                  </td>
                </tr>
                <tr>
                  <td>Bit to Bit Encryption</td>
                  <td class="list-value">${model.isBitEncryptionEnabled()?string("Enabled", "Disabled")}
                    <#if model.isBitEncryptionEnabled()><span class="material-icons">lock</span></#if>
                  </td>
                </tr>
            </tbody>
          </table>
        </div>
      </div>
  </div>

   <#if model.shouldShowAdminInfo()>
       <div class="row">
            <div class="col-md-12">
              <h3>User Info </h3>
              <div class="table-responsive">
                <table class="table table-hover" style="width: auto;">
                  <tbody>
                      <tr>
                        <td>Admin Users</td>
                        <td class="list-value"><@format_string str=model.getAdminUsers()/></td>
                      </tr>
                      <tr>
                        <td>Admin User Groups</td>
                        <td class="list-value"><@format_string str=model.getAdminUserGroups()/></td>
                      </tr>
                      <tr>
                        <td>Process User</td>
                        <td class="list-value">${model.getProcessUser()}</td>
                      </tr>
                      <tr>
                        <td>Process User Groups</td>
                        <td class="list-value">${model.getProcessUserGroups()}</td>
                      </tr>
                  </tbody>
                </table>
              </div>
            </div>
        </div>
   </#if>
  <#assign queueInfo = model.queueInfo() />
  <div class="row">
      <div class="col-md-12">
        <h3>Query Throttling</h3>
        <div class="table-responsive">
          <table class="table table-hover" style="width: auto;">
            <tbody>
               <tr>
                  <td>Queue Status</td>
                  <td class="list-value">${queueInfo.isEnabled()?string("Enabled", "Disabled")}</td>
                </tr>
  <#if queueInfo.isEnabled() >
                <tr>
                  <td>Maximum Concurrent "Small" Queries</td>
                  <td class="list-value">${queueInfo.smallQueueSize()}</td>
                 </tr>
                 <tr>
                  <td>Maximum Concurrent "Large" Queries</td>
                  <td class="list-value">${queueInfo.largeQueueSize()}</td>
                </tr>
                  <tr>
                  <td>Cost Threshhold for Large vs. Small Queries</td>
                  <td class="list-value">${queueInfo.threshold()}</td>
                </tr>
                <tr>
                  <td>Total Memory</td>
                  <td class="list-value">${queueInfo.totalMemory()}</td>
                </tr>
                <tr>
                  <td>Memory per Small Query</td>
                  <td class="list-value">${queueInfo.smallQueueMemory()}</td>
                </tr>
                <tr>
                  <td>Memory per Large Query</td>
                  <td class="list-value">${queueInfo.largeQueueMemory()}</td>
                </tr>
  </#if>
            </tbody>
          </table>
        </div>
      </div>
  </div>
   <script charset="utf-8">
      var refreshTime = 10000;
      var nAText = "Not Available";
      var refresh = getRefreshTime();
      var timeout;
      var size = $("#size").html();
      reloadMetrics();
      setInterval(reloadMetrics, refreshTime);
      
      //Enable the tooltip
      $(function () {
        var options = { delay: { "show" : 100, "hide" : 100 } };
        $('[data-toggle="tooltip"]').tooltip(options);
      });

      //Gets a refresh time for graceful shutdown
      function getRefreshTime() {
          $.ajax({
              type: 'GET',
              url: '/gracePeriod',
              dataType: "json",
              complete: function (data) {
                  let gracePeriod = data.responseJSON["gracePeriod"];
                  if (gracePeriod > 0) {
                      refreshTime = gracePeriod / 3;
                  }
                  timeout = setTimeout(reloadStatus, refreshTime);
              }
          });
      }

      //Periodic reload of status of Drillbits
      function reloadStatus () {
          let result = $.ajax({
                      type: 'GET',
                      url: '/state',
                      dataType: "json",
                      complete: function(data) {
                            fillStatus(data,size);
                            }
                      });
          timeout = setTimeout(reloadStatus, refreshTime);
      }

      //Fill the Status table for all the listed drillbits
      function fillStatus(dataResponse,size) {
          let status_map = (dataResponse.responseJSON);
          //In case localhost has gone down (i.e. we don't know status from ZK)
          if (typeof status_map == 'undefined') {
            //Query a couple of other nodes for state details
            //Pick 3 Random bits
            let randomAltBitList = getRandomIndexList(size, 3);
            let numAltBits = randomAltBitList.length;
            for (j = 0; j < numAltBits; j++) {
              let currentRow = $("#row-"+ randomAltBitList[j]);
              if (currentRow.find("#current").html() == "Current") {
                continue; //Skip LocalHost
              }
              let address = currentRow.find("#address").contents().get(0).nodeValue.trim();
              let restPort = currentRow.find("#httpPort").contents().get(0).nodeValue.trim();
              let altStateUrl = location.protocol + "//" + address+":"+restPort + "/state";
              let altResponse = $.ajax({
               url:altStateUrl, 
               dataType:'json', 
               timeout: 3000,
                    success : function(stateDataJson) {
                        //Update Status & Buttons for alternate stateData
                        if (typeof status_map == 'undefined') {
                          //Note: Race-condition would not exist due to JScript's single threaded model, only one thread can update the variable
                          //Ref: https://stackoverflow.com/questions/7238586/do-i-need-to-be-concerned-with-race-conditions-with-asynchronous-javascript
                          status_map = stateDataJson; //Marking map as 'defined'
                          updateStatusAndShutdown(stateDataJson);
                        }
                    }
                });
              //Don't loop any more (for small #bits, status_map will never update fast enough)
              if (typeof status_map != 'undefined') {
                break;
              }
            }
          } else {
            updateStatusAndShutdown(status_map);
          }
      }

      //Generates a list of indices
      function getRandomIndexList(inputSize, outputSize) {
        //Generate inputList
        let inputList = [];
        let outputList = [];
        //Skip 'row-1'
        for (p = 2; p <= inputSize; p++) {
          let currentRow = $("#row-"+ p);
          if (currentRow.find("#status").text() == 'ONLINE' ) {
              inputList.push(p);
          }
        }
        //Define how many to pick (pick min if numDBits is too small)
        let actualOPSize = (inputList.length > outputSize ? outputSize : inputList.length);
        for (q = 0; q < actualOPSize; q++) {
          let randomIndex = Math.floor(Math.random() * inputList.length);
          outputList.push(inputList[randomIndex]); //Capture random pick
          inputList.splice(randomIndex, 1); //Remove from list
        }
        return outputList;
      }

      //Updates the status map
      function updateStatusAndShutdown(status_map) {
        let bitMap = {};
        if (typeof status_map != 'undefined') {
            for (let k in status_map) {
              bitMap[k] = status_map[k];
            }
        }
        for (i = 1; i <= size; i++) {
            let currentRow = $("#row-"+i);
            let address = currentRow.find("#address").contents().get(0).nodeValue.trim();
            let port = currentRow.find("#httpPort").html();
            let key = address+"-"+port;
            if (typeof status_map == 'undefined') {
                currentRow.find("#status").text(nAText).css('font-style','').prop('title','');
                currentRow.find("#shutdown").prop('disabled',true).css('opacity',0.5).css('cursor','not-allowed');
                currentRow.find("#queriesCount").text("");
            } else if (status_map[key] == null) {
                currentRow.find("#status").text("OFFLINE*").css('font-style','italic').prop('title','Drillbit is De-Registered from ZooKeeper');
                currentRow.find("#shutdown").prop('disabled',true).css('opacity',0.5).css('cursor','not-allowed');
                currentRow.find("#queriesCount").text("");
            } else {
                if (status_map[key] === "ONLINE") {
                    currentRow.find("#status").text(status_map[key]).css('font-style','').prop('title','');
                    //EnableShutdown IFF => !isAuthEnabled OR isAuthEnabled-&&-current
                    if ( ( !${model.isAuthEnabled()?c} ) || ( ${model.shouldShowAdminInfo()?c} && currentRow.find("#current").html() === "Current" ) ) {
                      currentRow.find("#shutdown").prop('disabled',false).css('opacity',1.0).css('cursor','pointer').attr('title','');
                    }
                } else {
                    if (currentRow.find("#current").html() === "Current") {
                        fillQueryCount(i);
                    }
                    currentRow.find("#status").text(status_map[key]).css('font-style', '').prop('title', '');
                }
                //Removing accounted key
                delete bitMap[key];
            }
        }
        //If bitMap is not empty, then new bits have been discovered!
        listNewDrillbits(bitMap);
      }

      //Add new Bits for listing
      function listNewDrillbits(newBits) {
        let newBitList = Object.keys(newBits);
        let tableRef = document.getElementById('bitTable').getElementsByTagName('tbody')[0];
        let bitId = size;
        for (i = 0; i < newBitList.length; i++) {
           let splitPt = newBitList[i].lastIndexOf("-");
           let displayNodeName = newBitList[i].substring(0, splitPt);
           let newBitHttpPort = newBitList[i].substring(splitPt+1);
           let newBitElemId = "neo-"+newBitList[i];
           let newBitElem = document.getElementsByName(newBitElemId);
           bitId++;
           let bitState = newBits[newBitList[i]];
           //Injecting new row for previously unseen Drillbit
           $('#bitTable').find('tbody')
               .append("<tr id='row-" + bitId + "' class='newbit' title='Recommend page refresh for more info'>"
                 + "<td>"+bitId+"</td>"
                 + "<td id='address' name='"+newBitElemId+"'>"+displayNodeName+" <span class='badge badge-primary' id='size' >new</span></td>"
                 + "<td id='httpPort' style='display:none'>"+newBitHttpPort+"</td>"
                 + "<td class='heap'>"+nAText+"</td>"
                 + "<td class='direct'>"+nAText+"</td>"
                 + "<td class='bitload'>"+nAText+"</td>"
                 + "<td  class='avgload'>"+nAText+"</td>"
                 + "<td uiElem='userPort'>--</td>"
                 + "<td uiElem='ctrlPort'>--</td>"
                 + "<td uiElem='dataPort'>--</td>"
                 + "<td uiElem='version'>"+nAText+"</td>"
                 + "<td id='status'>"+bitState+"</td>"
                 + "<td class='uptime'>"+nAText+"</td>"
                 + "<td>"
                   + "<button type='button' id='shutdown' onclick='shutdown($(this));' disabled='true' style='opacity:0.5;cursor:not-allowed;${(model.isAuthEnabled() && !model.shouldShowAdminInfo())?then('display:none','')}'>"
                   + "<span class='material-icons'>power_settings_new</span></button></td>"
                 + "<td uiElem='queriesCount' />"
                 + "</tr>"
            );
        }
        //Update Drillbits count on top
        if (newBitList.length > 0) {
          //Setting Drillbit Count & Display Refresh Icon
          $('#size').text(bitId);
          $('#reloadBtn').css('display', 'inline');
          size = bitId;
        }
      }

      //Updates the outstanding queries in flight before shutdown
      function fillQueryCount(row_id) {
          let requestPath = "/queriesCount";
          let url = getRequestUrl(requestPath);
          let result = $.ajax({
                        type: 'GET',
                        url: url,
                        complete: function(data) {
                              queries = data.responseJSON["queriesCount"];
                              fragments = data.responseJSON["fragmentsCount"];
                              $("#row-"+row_id).find("#queriesCount").text(queries+" queries and "+fragments+" fragments remaining before shutting down");
                              }
                        });
      }
       <#if (model.shouldShowAdminInfo() || !model.isAuthEnabled()) >
        function shutdown(shutdownBtn) {
            let rowElem = $(shutdownBtn).parent().parent();
            let hostAddr = $(rowElem).find('#address').contents().get(0).nodeValue.trim();
            let url = rowElem.find("#current").html() === "Current" ? "/gracefulShutdown" : "/gracefulShutdown/" + hostAddr;

            showConfirmationDialog("Are you sure you want to shutdown Drillbit running on " + hostAddr + " node?", function() {
              let result = $.ajax({
                    type: 'POST',
                    url: url,
                    contentType : 'text/plain',
                    error: function (request, textStatus, errorThrown) {
                        alert(errorThrown);
                    },
                    success: function(data) {
                        alert(data.responseJSON["response"]);
                        shutdownBtn.prop('disabled',true).css('opacity',0.5);
                    }
              });
            });
        }
        </#if>

      //Pops out the WebUI for a remote Drillbit
      function popOutRemoteDbitUI(dbitHost, dbitPort) {
            let dbitWebUIUrl = location.protocol+'//'+ dbitHost+':'+dbitPort;
            let tgtWindow = 'dbit_'+dbitHost;
            window.open(dbitWebUIUrl, tgtWindow);
      }

      //Construct the URL with the appropriate protocol and host
      function getRequestUrl(requestPath) {
            let protocol = location.protocol;
            let host = location.host;
            var url = protocol + "//" + host + requestPath;
            return url;
      }

      //Iterates through all the nodes for update
      function reloadMetrics() {
        for (i = 1; i <= size; i++) {
          let currentRow = $("#row-" + i);
          let address = "";
          let isCurrent = false;
          //For 'current' bit, address is referred to by location.hostname instead of FQDN ()
          if (i === 1) {
            isCurrent = true;
          } else {
            address = currentRow.find("#address").contents().get(0).nodeValue.trim();
          }
          updateMetricsHtml(address, isCurrent, i);
        }
      }

      //Update memory
      function updateMetricsHtml(drillbit, isCurrent, idx) {
        let drillbitURL = isCurrent ? "/status/metrics" : "/status/metrics/" + drillbit;
        let result = $.ajax({
          type: 'GET',
          url: drillbitURL,
          dataType: "json",
          error: function (data) {
            resetMetricsHtml(idx);
          },
          complete: function (data) {
            if (typeof data.responseJSON == 'undefined') {
              resetMetricsHtml(idx);
              return;
            }
            let metrics = data.responseJSON['gauges'];
            //Memory
            let usedHeap = metrics['heap.used'].value;
            let maxHeap = metrics['heap.max'].value;
            let usedDirect = metrics['drill.allocator.root.used'].value;
            let peakDirect = metrics['drill.allocator.root.peak'].value;
            let heapUsage = computeMemUsage(usedHeap, maxHeap);
            let directUsage = computeMemUsage(usedDirect, peakDirect);
            let rowElem = document.getElementById("row-" + idx);
            let heapElem = rowElem.getElementsByClassName("heap")[0];
            heapElem.innerHTML = heapUsage;
            let directElem = rowElem.getElementsByClassName("direct")[0];
            directElem.innerHTML = directUsage;
            //DrillbitLoad
            let dbitLoad = metrics['drillbit.load.avg'].value;
            let dbitLoadElem = rowElem.getElementsByClassName("bitload")[0];
            if (dbitLoad >= 0) {
              dbitLoadElem.innerHTML = parseFloat(Math.round(dbitLoad * 10000) / 100).toFixed(2) + "%";
            } else {
              dbitLoadElem.innerHTML = nAText;
            }
            //AvgSysLoad
            let avgSysLoad = metrics['os.load.avg'].value;
            let sysLoadElem = rowElem.getElementsByClassName("avgload")[0];
            sysLoadElem.innerHTML = avgSysLoad;
            //Uptime
            let uptimeValue = metrics['drillbit.uptime'].value;
            let uptimeElem = rowElem.getElementsByClassName("uptime")[0];
            uptimeElem.innerHTML = elapsedTime(uptimeValue);
          }
        });
      }

      //Reset the metrics of a Drillbit in the table
      function resetMetricsHtml(idx) {
        $.each(["heap","direct","bitload","avgload","uptime"], function(i, key) {
          let resetElem = document.getElementById("row-"+idx).getElementsByClassName(key)[0];
          resetElem.innerHTML = nAText;
        });
    };

      //Compute Usage
      function computeMemUsage(used, max) {
        let percent = 0;
        if ( max > 0) {
          percent = Math.round((100 * used / max), 2);
        }
        let usage   =  bytesInGB(used, 2) + "GB (" + Math.max(0, percent) + "% of "+ bytesInGB(max, 2) +"GB)";
        return usage;
      }

      //Translate bytes into GB with decimal places
      function bytesInGB(byteVal, decimal) {
        let scale = Math.pow(10,decimal);
        return Math.round(scale * byteVal / 1073741824)/scale;
      }

      //Calculate Uptime
      function elapsedTime(valueInMsec) {
        let elapsedTime = "";
        let d, h, m, s;
        s = Math.floor(valueInMsec / 1000);
        m = Math.floor(s / 60);
        s = s % 60;
        h = Math.floor(m / 60);
        m = m % 60;
        d = Math.floor(h / 24);
        h = h % 24;
        let detailCount = 0;
        if (!isNaN(d) && d > 0) {
          elapsedTime = d+"d ";
          detailCount += 1;
        }
        if (!isNaN(h) && h > 0) {
          elapsedTime = elapsedTime + h+"h ";
          detailCount += 1;
        }
        if (!isNaN(m) && m > 0 && detailCount < 2) {
          elapsedTime = elapsedTime + m+"m ";
          detailCount += 1;
        }
        if (s > 0 && detailCount < 2) {
          elapsedTime = elapsedTime + s+"s";
        }
        return elapsedTime;
     };
  </script>
</#macro>

<@page_html/>
