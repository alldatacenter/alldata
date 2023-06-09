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
  <h4>Manage Drill Cluster</h4>

  Current Status: ${model.getLiveCount( )}
  <#if model.getLiveCount( ) == 1 >Drillbit is
  <#else>Drillbits are
  </#if>running.
  <p>
  Free YARN nodes: Approximately ${model.getFreeNodeCount( )} 
  <p><p>
  
  <table class="table table-hover" style="width: auto;">
    <#-- Removed per user feedback. (Kept in REST API as client needs them.
    <tr><td style="vertical-align: middle;">
      <form action="/resize" method="POST" class="form-inline" role="form">
        <div class="form-group">
          <input hidden name="type" value="grow">
          <label for="add">Add</label>
          <input type="text" name="n" size="6" id="add" class="form-control"
                  placeholder="+n" style="padding: 0 1em; margin: 0 1em;"/>
          drillbits.
          <button type="submit" class="btn btn-primary" style="margin: 0 1em;">Go</button>
        </div>
      </form>
    </td></tr>
    <tr><td>
      <form action="/resize" method="POST" class="form-inline" role="form">
        <div class="form-group">
          <input hidden name="type" value="shrink">
          <label for="shrink">Remove</label>
          <input type="text" name="n" size="6" id="shrink" class="form-control"
                  placeholder="-n" style="padding: 0 1em; margin: 0 1em;"/>
          drillbits.
          <button type="submit" class="btn btn-primary" style="margin: 0 1em;">Go</button>
        </div>
      </form>
    </td></tr>
    -->
    <tr><td>
      <form action="/resize" method="POST" class="form-inline" role="form">
        <div class="form-group">
          <input hidden name="type" value="resize">
          <label for="resize">Resize to</label>
          <input type="text" name="n" id="resize" size="6"
                  placeholder="Size" class="form-control" style="padding: 0 1em; margin: 0 1em;"/>
          drillbits.
          <button type="submit" class="btn btn-primary" style="margin: 0 1em;">Go</button>
        </div>
        <input type="hidden" name="csrfToken" value="${csrfToken}">
      </form>
    </td></tr>
    <tr><td>
      <form action="/stop" method="GET" class="form-inline" role="form">
        <div class="form-group">
          <label for="stop">Stop</label> the Drill cluster.
          <button type="submit" id="stop" class="btn btn-primary" style="margin: 0 1em;">Go</button>
        </div>
      </form>
    </td></tr>
  </table>

</#macro>

<@page_html/>
