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
  <h4>Drill Cluster Status</h4>

  <table class="table table-hover" style="width: auto;">
    <tr>
      <td>YARN Application ID:</td>
      <td><a href="${model.getRmAppLink( )}" data-toggle="tooltip" title="YARN Resource Manager page for this application">
      ${model.getAppId( )}</a></td>
    </tr>
    <tr>
      <td>YARN Resource Manager:</td>
      <td><#if model.getRmLink()?? > <#-- Occurs early in startup before app is fully registered. -->
      <a href="${model.getRmLink( )}" data-toggle="tooltip" title="YARN Resource Manager page for this container">
      ${model.getRmHost( )}</a>
      <#else>Unavailable
      </#if></td>
    </tr>
    <tr>
      <td>YARN Node Manager for AM:</td>
      <td><#if model.getNmLink()?? > <#-- Occurs early in startup before app is fully registered. -->
      <a href="${model.getNmLink( )}" data-toggle="tooltip" title="YARN Node Manager">
      ${model.getNmHost( )}</a> |
          <a href="${model.getNmAppLink( )}" data-toggle="tooltip" title="YARN Node Manager page for this application">App info</a>
      <#else>Unavailable
      </#if></td>
    </tr>
    <tr>
      <td>ZooKeeper Hosts:</td>
      <td><span data-toggle="tooltip" title="ZooKeeper connection string.">
      ${model.getZkConnectionStr( )}</span></td>
    </tr>
    <tr>
      <td>ZooKeeper Root:</td>
      <td><span data-toggle="tooltip" title="ZooKeeper Drill root.">
      ${model.getZkRoot( )}</span></td>
    </tr>
    <tr>
      <td>ZooKeeper Cluster ID:</td>
      <td><span data-toggle="tooltip" title="ZooKeeper Drill cluster-id.">
      ${model.getZkClusterId( )}</span></td>
    </tr>
    <tr>
      <td>State:</td>
      <td><span data-toggle="tooltip" title="${model.getStateHint( )}">
      ${model.getState( )}</span></td>
    </tr>
    <tr>
      <td>Target Drillbit Count:</td>
      <td>${model.getTargetCount( )}</td>
    </tr>
    <tr>
      <td>Live Drillbit Count:</td>
      <td>${model.getLiveCount( )}</td>
    </tr>
    <#if model.getUnmanagedCount( ) gt 0 >
      <tr>
        <td style="color: red">Unmanaged Drillbit Count:</td>
        <td>${model.getUnmanagedCount( )}</td>
      </tr>
    </#if>
    <#if model.getBlacklistCount( ) gt 0 >
      <tr>
        <td style="color: red">Blacklisted Node Count:</td>
        <td>${model.getBlacklistCount( )}</td>
      </tr>
    </#if>
    <tr>
      <td>Total Drill Virtual Cores:</td>
      <td>${model.getDrillTotalVcores( )}</td>
    </tr>
    <tr>
      <td>Total Drill Memory (MB):</td>
      <td>${model.getDrillTotalMemory( )}</td>
    </tr>
    <#if model.supportsDiskResource( ) >
      <tr>
        <td>Total Drill Disks:</td>
        <td>${model.getDrillTotalDisks( )}</td>
      </tr>
    </#if>
  </table>
  <table class="table table-hover" style="width: auto;">
    <tr>
      <th>Group</th>
      <th>Type</th>
      <th>Target Drillbit Count</th>
      <th>Total Drillbits</th>
      <th>Live Drillbits</th>
      <th>Memory per Drillbit (MB)</th>
      <th>VCores per Drillbit</th>
      <#if model.supportsDiskResource( ) >
        <th>Disks per Drillbit</th>
      </#if>
    </tr>
    <#list model.getGroups( ) as group>
      <tr>
        <td>${group.getName( )}</td>
        <td>${group.getType( )}</td>
        <td>${group.getTargetCount( )}</td>
        <td>${group.getTaskCount( )}</td>
        <td>${group.getLiveCount( )}</td>
        <td>${group.getMemory( )}</td>
        <td>${group.getVcores( )}</td>
        <#if model.supportsDiskResource( ) >
          <td>${group.getDisks( )}</td>
        </#if>
      </tr>
    </#list>
  </table>
</#macro>

<@page_html/>
