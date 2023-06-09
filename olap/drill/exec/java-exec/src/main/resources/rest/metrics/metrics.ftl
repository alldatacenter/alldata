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
  <div class="row">

    <div class="col-md-3 sidebar">
      <div id="sideDiv" class="list-group">
        <a class="list-group-item" href="#gauges">Gauges</a>
        <a class="list-group-item" href="#counters">Counters</a>
        <a class="list-group-item" href="#histograms">Histograms</a>
        <a class="list-group-item" href="#meters">Meters</a>
        <a class="list-group-item" href="#timers">Timers</a>
      </div>
      <br>
      Heap
      <div class="progress">
        <div id="heapUsage" class="progress-bar" role="progressbar" aria-valuenow="50" aria-valuemin="0" aria-valuemax="100" style="width: 50%;">
        </div>
      </div>
      Non heap
      <div class="progress">
        <div id="non-heapUsage" class="progress-bar" role="progressbar" aria-valuenow="50" aria-valuemin="0" aria-valuemax="100" style="width: 50%;">
        </div>
      </div>
      Total
      <div class="progress">
        <div id="totalUsage" class="progress-bar" role="progressbar" aria-valuenow="50" aria-valuemin="0" aria-valuemax="100" style="width: 50%;">
        </div>
      </div>
      Actively Used Direct (Estimate)
      <div class="progress">
        <div id="estDirectUsage" class="progress-bar" role="progressbar" aria-valuenow="50" aria-valuemin="0" aria-valuemax="100" style="width: 50%;">
        </div>
      </div>
    </div>

    <div id="mainDiv" class="col-md-9" role="main">
      <h3 id="gauges">Gauges</h3>
      <div id="gaugesVal">
        <table class="table table-striped" id="gaugesTable">
        </table>
      </div>
      <h3 id="counters">Counters</h3>
      <div id="countersVal">
        <div class="alert alert-info">
          <strong>No counters.</strong>
        </div>
      </div>
      <h3 id="histograms">Histograms</h3>
      <div id="histogramsVal">
        <div class="alert alert-info">
          <strong>No histograms.</strong>
        </div>
      </div>
      <h3 id="timers">Timers</h3>
      <div id="timersVal">
        <div class="alert alert-info">
          <strong>No timers.</strong>
        </div>
      </div>
    </div>

  </div>

  <script>
    var round = function(val, n) {
      return Math.round(val * Math.pow(10, n)) / Math.pow(10, n);
    };
    var isAllocator = function(metricName) {
      return (metricName.startsWith("drill.allocator"));
    };
    function getGBUsageText(val, perc) {
      if (isNaN(perc)) {
        perc = 0;
      }
      return round((val / 1073741824), 2) + "GB (" + Math.max(0, perc) + "%)";
    }

    function updateGauges(gauges) {
      $("#gaugesTable").html(function() {
        var table = "<tbody>";
        $.each(gauges, function(key, value) {
          table += "<tr>";
          table += "<td>" + key + "</td>";
          table += "<td>" + round(value.value, 6) + "</td>";
          table += "</tr>";
        });
        table += "</tbody>";
        return table;
      });
    };

    function createCountersTable(counters) {
      $("#countersVal").html(function() {
        var table = "<table class=\"table table-striped\" id=\"countersTable\">";
        table += "<tbody>";
        $.each(counters, function(key, value) {
          table += "<tr>";
          table += "<td>" + key + "</td>";
          table += "<td>" + value.count + "</td>";
          table += "</tr>";
        });
        table += "</tbody>";
        table += "</table>";
        return table;
      });
    };

    function updateBars(gauges) {
      $.each(["heap","non-heap","total","drill.allocator.root"], function(i, key) {
        var used = gauges[key + ".used"].value;
        var max;
        if (isAllocator(key)) {
          max = gauges[key + ".peak"].value;
        } else {
          max = gauges[key + ".max"].value;
        }
        var percent = round((100 * used / max), 2);
        var usage = getGBUsageText(used, percent);

        var styleVal = "width: " + percent + "%;color: #202020;white-space: nowrap"
        $("#" + (isAllocator(key) ? "estDirect" : key) + "Usage").attr({
          "aria-valuenow" : percent,
          "style" : styleVal
        });
        $("#" + (isAllocator(key) ? "estDirect" : key) + "Usage").html(usage);
      });
    };

    function createTable(metric, name) {
      $("#" + name + "Val").html(function() {
        var tables = "";
        $.each(metric, function(clazz, stats) {

          var table = "<strong>Reporting class:</strong> " + clazz + "<br>";
          table += "<table class=\"table table-striped\"><tbody>";
          $.each(stats, function(key, value) {
            table += "<tr>";
            table += "<td>" + key + "</td>";
            if(typeof value === "number") {
              value = round(value, 5);
            }
            table += "<td>" + value + "</td>";
            table += "</tr>";
          });
          table += "</tbody></table>";

          tables += table;
        });
        return tables;
      });
    };

    var update = function() {
      $.get("/status/metrics", function(metrics) {
        updateGauges(metrics.gauges);
        updateBars(metrics.gauges);
        if(! $.isEmptyObject(metrics.timers)) createTable(metrics.timers, "timers");
        if(! $.isEmptyObject(metrics.histograms)) createTable(metrics.histograms, "histograms");
        if(! $.isEmptyObject(metrics.counters)) createCountersTable(metrics.counters);
      });
    };

    update();
    setInterval(update, 3000);
  </script>
</#macro>

<@page_html/>
