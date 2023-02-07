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

<#macro page_body>
  <h4>Drillbit Status</h4>
  <p>&nbsp;

  <#if model.hasTasks( ) >
    <div class="table-responsive">
      <table class="table table-hover">
        <tr>
          <th><span data-toggle="tooltip" title="Internal AM ID for the Drillbit.">ID</span></th>
          <th><span data-toggle="tooltip" title="Cluster group from config file">Group</span></th>
          <th><span data-toggle="tooltip"
                    title="Host name or IP running the Drillbit and ink to Drillbit web UI.">
              Host</span></th>
          <th><span data-toggle="tooltip" title="State of the Drillbit process, hover for details.">State</span></th>
          <th><span data-toggle="tooltip" title="ZooKeeper tracking state for the Drillbit, hover for details.">ZK State</span></th>
          <th><span data-toggle="tooltip"
                    title="YARN Container allocated to the Drillbit and link to the YARN Node Manager container UI.">
              Container</span></th>
          <th><span data-toggle="tooltip" title="Memory granted by YARN to the Drillbit.">Memory (MB)</span></th>
          <th><span data-toggle="tooltip" title="Virtual cores granted by YARN to the Drillbit.">Virtual Cores</span></th>
          <#if showDisks >
            <th><span data-toggle="tooltip" title="Disk resources granted by YARN to the Drillbit.">Disks</span></th>
          </#if>
          <th><span data-toggle="tooltip" title="Start time in the AM server time zone.">Start Time</span></th>
        </th>
        <#list tasks as task>
          <tr>
            <td><b>${task.getTaskId( )}</b></td>
            <td>${task.getGroupName( )}</td>
            <td>
            <#if task.isLive( )>
              <a href="${task.getLink( )}" data-toggle="tooltip" title="Link to the Drillbit Web UI"></#if>
            ${task.getHost( )}
            <#if task.isLive( )></a></#if>
            </td>
            <td><span data-toggle="tooltip" title="${task.getStateHint( )}">${task.getState( )}</span>
            <#if task.isCancelled( )><br/>(Cancelled)</#if>
            <#if task.isCancellable( )>
              <a href="/cancel?id=${task.getTaskId( )}" data-toggle="tooltip" title="Kill this Drillbit">[x]</a>
            </#if>
            </td>
            <td><span data-toggle="tooltip" title="${task.getTrackingStateHint( )}">${task.getTrackingState( )}</span></td>
            <td><#if task.hasContainer( )>
              <a href="${task.getNmLink( )}" data-toggle="tooltip" title="Node Manager UI for Drillbit container">${task.getContainerId()}</a>
            <#else>&nbsp;</#if></td>
            <td>${task.getMemory( )}</td>
            <td>${task.getVcores( )}</td>
            <#if showDisks >
              <td>${task.getDisks( )}</td>
            </#if>
            <td>${task.getStartTime( )}</td>
          </tr>
        </#list>
      </table>
    <#else>
      <div class="alert alert-danger">
        No drillbits are running.
      </div>
    </#if>
    <#if model.hasUnmanagedDrillbits( ) >
      <hr>
      <div class="alert alert-danger">
        <strong>Warning:</strong> ZooKeeper reports that
        ${model.getUnmanagedDrillbitCount( )} Drillbit(s) are running that were not
        started by the YARN Application Master. Perhaps they were started manually.
      </div>
      <table class="table table-hover" style="width: auto;">
        <tr><th>Drillbit Host</th><th>Ports</th></tr>
        <#list strays as stray >
          <tr>
            <td>${stray.getHost( )}</td>
            <td>${stray.getPorts( )}</td>
          </tr>
        </#list>
      </table>
    </#if>
    <#if model.hasBlacklist( ) >
      <hr>
      <div class="alert alert-danger">
        <strong>Warning:</strong> 
        ${model.getBlacklistCount( )} nodes have been black-listed due to
        repeated Drillbit launch failures. Perhaps the nodes or Drill are
        improperly configured.
      </div>
      <table class="table table-hover" style="width: auto;">
        <tr><th>Blacklisted Host</th></tr>
        <#list blacklist as node >
          <tr>
            <td>${node}</td>
          </tr>
        </#list>
      </table>
    </#if>
  </div>
</#macro>

<@page_html/>
