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
  <script src="/static/js/jquery.form.js"></script>
  <script src="/static/js/querySubmission.js"></script>
  <!-- Ace Libraries for Syntax Formatting -->
  <script src="/static/js/ace-code-editor/ace.js" type="text/javascript" charset="utf-8"></script>
  <!-- Disabled in favour of dynamic: script src="/static/js/ace-code-editor/mode-sql.js" type="text/javascript" charset="utf-8" -->
  <script src="/dynamic/mode-sql.js" type="text/javascript" charset="utf-8"></script>
  <script src="/static/js/ace-code-editor/ext-language_tools.js" type="text/javascript" charset="utf-8"></script>
  <script src="/static/js/ace-code-editor/theme-sqlserver.js" type="text/javascript" charset="utf-8"></script>
  <script src="/static/js/ace-code-editor/snippets/sql.js" type="text/javascript" charset="utf-8"></script>
  <script src="/static/js/ace-code-editor/mode-snippets.js" type="text/javascript" charset="utf-8"></script>
</#macro>

<#macro page_body>
  <div id="message" class="alert alert-info alert-dismissable" style="font-family: courier,monospace;">
    <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>
    Sample SQL query: <strong>SELECT * FROM cp.`employee.json` LIMIT 20</strong>
  </div>

<#include "*/alertModals.ftl">

<#include "*/runningQuery.ftl">

  <#-- DRILL-7697: merge with copy in profile.ftl -->
  <form role="form" id="queryForm" action="/query" method="POST">
    <#if model.isOnlyImpersonationEnabled()>
      <div class="form-group">
        <label for="userName">User Name</label>
        <input type="text" size="30" name="userName" id="userName" placeholder="User Name">
      </div>
    </#if>
    <div class="container-fluid">
      <div class="form-group row">
        <label class="font-weight-bold" for="queryType">Query type:&nbsp;&nbsp;</label>
        <div class="form-check">
          <label class="font-weight-bold">
            <input type="radio" name="queryType" id="sql" value="SQL" checked>
            SQL
          </label>
        </div>
        <div class="form-check">
          <label class="font-weight-bold">
            <input type="radio" name="queryType" id="physical" value="PHYSICAL">
            Physical
          </label>
        </div>
        <div class="form-check">
          <label class="font-weight-bold">
            <input type="radio" name="queryType" id="logical" value="LOGICAL">
            Logical
          </label>
        </div>
      </div>
    </div>

    <div class="form-group">
      <div style="display: inline-block"><label class="font-weight-bold" for="query">Query</label></div>
      <div style="display: inline-block; float:right; padding-right:5%"><b>Hint: </b>Use
        <div id="keyboardHint" style="display:inline-block; font-style:italic"></div> to submit</div>
      <div id="query-editor-format" class="border"></div>
      <input class="form-control" type="hidden" id="query" name="query" autofocus/>
    </div>

    <button class="btn btn-primary" type="button" onclick="<#if model.isOnlyImpersonationEnabled()>doSubmitQueryWithUserName()<#else>doSubmitQueryWithAutoLimit()</#if>">
      Submit
    </button>
    &nbsp;&nbsp;&nbsp;
    <button class="btn btn-secondary" type="button" onclick="resetQuery()">
      Reset
    </button>
    &nbsp;&nbsp;&nbsp;
    <input type="checkbox" name="forceLimit" value="limit" <#if model.isAutoLimitEnabled()>checked</#if>>
      Limit results to <input type="text" id="autoLimit" name="autoLimit" min="0" value="${model.getDefaultRowsAutoLimited()?c}" size="6" pattern="[0-9]*">
      rows <span data-toggle="tooltip" class="material-icons" title="Limits the number of records retrieved in the query.
      Ignored if query has a LIMIT clause." style="cursor: help;">info</span>
    &nbsp;&nbsp;&nbsp;
    Default schema:
    <input type="text" name="defaultSchema" id="defaultSchema" list="enabledPlugins" placeholder="schema">
    <datalist id="enabledPlugins">
      <#list model.getEnabledPlugins() as pluginModel>
        <#if pluginModel.getPlugin()?? && pluginModel.getPlugin().enabled() == true>
          <option value="${pluginModel.getPlugin().getName()}">
        </#if>
      </#list>
    </datalist>
     <span data-toggle="tooltip" class="material-icons" title="Set the default schema used to find table names
      and for SHOW FILES and SHOW TABLES." style="cursor: help;">info</span>
    <input type="hidden" name="csrfToken" value="${model.getCsrfToken()}">
  </form>

  <script>
    // Enable the tooltip
    $(function () {
      var options = { delay: { "show" : 100, "hide" : 100 } };
      $('[data-toggle="tooltip"]').tooltip(options);
    });
    // Remember form field values over page reloads
    $("input[type=text],input[type=checkbox],input[type=radio],select").each(function () {
      var $input = $(this);
      var savedKey = "saved_query_" + $input.attr("name");
      var savedValue = sessionStorage.getItem(savedKey);
      if ($input.attr("type") === "checkbox") {
        if (savedValue === "true") {
          $input.prop("checked", true);
        }
        if (savedValue === "false") {
          $input.prop("checked", false);
        }
        $input.change(function () {
          sessionStorage.setItem(savedKey, String($(this).prop("checked")));
        });
      } else if ($input.attr("type") === "radio") {
        var value = $input.val();
        if (savedValue === value) {
          $input.prop("checked", true);
        }
        $input.change(function () {
          sessionStorage.setItem(savedKey, $(this).val());
        });
      } else {
        if (typeof savedValue === "string") {
          $input.val(savedValue);
        }
        $input.change(function () {
          sessionStorage.setItem(savedKey, $(this).val());
        });
      }
    });
    // Hidden text input for form-submission
    var queryText = $('input[name="query"]');
    ace.require("ace/ext/language_tools");
    var editor = ace.edit("query-editor-format");
    editor.getSession().on("change", function () {
      var text = editor.getSession().getValue();
      queryText.val(text);
      sessionStorage.setItem("saved_query_query", text);
    });
    var savedQueryText = sessionStorage.getItem('saved_query_query');
    if (savedQueryText) {
      editor.getSession().setValue(savedQueryText);
    }
    editor.setAutoScrollEditorIntoView(true);
    editor.setOption("maxLines", 25);
    editor.setOption("minLines", 12);
    editor.renderer.setShowGutter(true);
    editor.renderer.setOption('showLineNumbers', true);
    editor.renderer.setOption('showPrintMargin', false);
    editor.getSession().setMode("ace/mode/sql");
    editor.getSession().setTabSize(4);
    editor.getSession().setUseSoftTabs(true);
    editor.setTheme("ace/theme/sqlserver");
    editor.$blockScrolling = "Infinity";
    editor.focus();
    //CSS Formatting
    document.getElementById('query-editor-format').style.fontSize='13px';
    document.getElementById('query-editor-format').style.fontFamily='courier,monospace';
    document.getElementById('query-editor-format').style.lineHeight='1.5';
    document.getElementById('query-editor-format').style.margin='auto';
    editor.setOptions({
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: false
    });

    //Provides hint based on OS
    var browserOS = navigator.platform.toLowerCase();
    if ((browserOS.indexOf("mac") > -1)) {
      document.getElementById('keyboardHint').innerHTML="Meta+Enter";
    } else {
      document.getElementById('keyboardHint').innerHTML="Ctrl+Enter";
    }

    // meta+enter / ctrl+enter to submit query
    document.getElementById('queryForm')
            .addEventListener('keydown', function(e) {
      if (!(e.keyCode == 13 && (e.metaKey || e.ctrlKey))) return;
      if (e.target.form) //Submit [Wrapped] Query
        <#if model.isOnlyImpersonationEnabled()>doSubmitQueryWithUserName()<#else>doSubmitQueryWithAutoLimit()</#if>;
    });
  </script>
</#macro>

<@page_html/>
