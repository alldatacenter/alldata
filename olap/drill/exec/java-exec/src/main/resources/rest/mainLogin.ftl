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
  <div class="container container-table">
    <div align="center" class="table-responsive">
      <#if model?? && model.isFormEnabled()>
        <a href ="/login" class="btn btn-primary"> Login using FORM AUTHENTICATION </a>
      </#if>
      <#if model?? && model.isSpnegoEnabled()>
        <a href = "/spnegoLogin" class="btn btn-primary"> Login using SPNEGO </a>
      </#if>
      <#if model?? && model.getError()??>
        <p style="color:red">${model.getError()}</p></br>
      </#if>
    </div>
  </div>
</#macro>
<@page_html/>
