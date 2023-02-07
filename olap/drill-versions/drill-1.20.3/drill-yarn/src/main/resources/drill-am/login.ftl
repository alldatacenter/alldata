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
  <div align="center" class="table-responsive">
    <form role="form" name="input" action="/j_security_check" method="POST">
      <fieldset>
        <div class="form-group">
          <img src="/drill-am/static/img/apache-drill-logo.png" alt="Apache Drill Logo">
          <#if model??>
          <div class="alert alert-danger">
            <strong>Error</strong> ${model}
          </div>
          </#if>
          <p><input type="text" size="30" name="j_username" placeholder="Username"></p>
          <p><input type="password" size="30" name="j_password" placeholder="Password"></p>
          <p><button type="submit" class="btn btn-primary">Log In</button></p>
        </div>
      </fieldset>
    </form>
  </div>
</#macro>
<@page_html/>
