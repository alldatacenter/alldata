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
  <!-- Ace Libraries for Syntax Formatting -->
  <script src="/static/js/ace-code-editor/ace.js" type="text/javascript" charset="utf-8"></script>
  <script src="/static/js/ace-code-editor/theme-eclipse.js" type="text/javascript" charset="utf-8"></script>
  <script src="/static/js/serverMessage.js"></script>
</#macro>

<#macro page_body>
  <h3>Configuration</h3>
  <form id="updateForm" role="form" action="/storage/create_update" method="POST">
    <input type="hidden" name="name" value="${model.getPlugin().getName()}" />
    <input type="hidden" name="pluginType" value="${model.getType()}" />
    <div class="form-group">
      <div id="editor" class="form-control"></div>
      <textarea class="form-control" id="config" name="config" data-editor="json" style="display: none;" >
      </textarea>
    </div>
    <a class="btn btn-secondary" href="/storage">Back</a>
    <button class="btn btn-primary" type="submit" onclick="doUpdate();">Update</button>
  <#if model.getPlugin().enabled()>
      <a id="enabled" class="btn btn-warning">Disable</a>
  <#else>
      <a id="enabled" class="btn btn-success text-white">Enable</a>
  </#if>
  <#if model.getType() == "HttpStoragePluginConfig" && model.getPlugin().isOauth() >
      <a id="getOauth" class="btn btn-success text-white">Authorize</a>
  </#if>
    <button type="button" class="btn btn-secondary export" name="${model.getPlugin().getName()}" data-toggle="modal"
  data-target="#pluginsModal">
  Export
    </button>
    <a id="del" class="btn btn-danger text-white" onclick="deleteFunction()">Delete</a>
    <input type="hidden" name="csrfToken" value="${model.getCsrfToken()}">
  </form>
  <br>
  <div id="message" class="d-none alert alert-info">
  </div>

  <#include "*/confirmationModals.ftl">

<#-- Modal window-->
  <div class="modal fade" id="pluginsModal" tabindex="-1" role="dialog" aria-labelledby="exportPlugin" aria-hidden="true">
    <div class="modal-dialog modal-sm" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title" id="exportPlugin">Plugin config</h4>
          <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        </div>
        <div class="modal-body">
          <div id="format" style="display: inline-block; position: relative;">
            <label for="format">Format</label>
            <div class="radio">
              <label>
                <input type="radio" name="format" id="json" value="json" checked="checked">
  JSON
              </label>
            </div>
            <div class="radio">
              <label>
                <input type="radio" name="format" id="hocon" value="conf">
  HOCON
              </label>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
          <button type="button" id="export" class="btn btn-primary">Export</button>
        </div>
      </div>
    </div>
  </div>

  <script>
    const editor = ace.edit("editor");
    const textarea = $('textarea[name="config"]');

    editor.setAutoScrollEditorIntoView(true);
    editor.setOption("maxLines", 25);
    editor.setOption("minLines", 10);
    editor.renderer.setShowGutter(true);
    editor.renderer.setOption('showLineNumbers', true);
    editor.renderer.setOption('showPrintMargin', false);
    editor.getSession().setMode("ace/mode/json");
    editor.setTheme("ace/theme/eclipse");

    // copy back to textarea on form submit...
    editor.getSession().on('change', function(){
      textarea.val(editor.getSession().getValue());
    });

    $.get("/storage/" + encodeURIComponent("${model.getPlugin().getName()}") + ".json", function(data) {
      $("#config").val(JSON.stringify(data.config, null, 2));
      editor.getSession().setValue( JSON.stringify(data.config, null, 2) );
    });


    $("#enabled").click(function() {
      const enabled = ${model.getPlugin().enabled()?c};
      if (enabled) {
        showConfirmationDialog('"${model.getPlugin().getName()}"' + ' plugin will be disabled. Proceed?', proceed);
      } else {
        proceed();
      }
      function proceed() {
        $.post("/storage/" + encodeURIComponent("${model.getPlugin().getName()}") + "/enable/" + !enabled, function(data) {
          if (serverMessage(data)) {
              setTimeout(function() { location.reload(); }, 800);
          }
        }).fail(function(response) {
          if (serverMessage(response.responseJSON)) {
              setTimeout(function() { location.reload(); }, 800);
          }
        });
      }
    });

  <#if model.getType() == "HttpStoragePluginConfig" >
    $("#getOauth").click(function() {
      var field = document.getElementById("config");
      try {
        var storage_config = JSON.parse(config.value);

        // Construct the Callback URL
        var clientID = storage_config.credentialsProvider.credentials.clientID;
       if (clientID.length == 0) {
         window.alert("Invalid client ID.");
         return false;
        }

        var callbackURL = storage_config.oAuthConfig.callbackURL;
        if (callbackURL.length == 0) {
          window.alert("Invalid callback URL.");
          return false;
        }

        var authorizationURL = storage_config.oAuthConfig.authorizationURL;
        if (authorizationURL) {
          finalURL = authorizationURL + "?client_id=" + clientID + "&redirect_uri=" + callbackURL;
        } else {
          window.alert("Invalid authorization URL.")
          return false;
        }

        // Add scope(s) if populated
        var scope = storage_config.oAuthConfig.scope;
        if (scope) {
          var encodedScope = encodeURIComponent(scope);
          finalURL = finalURL + "&scope=" + encodedScope;
        }

        // Add any additional parameters to the oAuth configuration string
        var params = storage_config.oAuthConfig.authorizationParams
        if (params) {
          var param;
          for (var key in storage_config.oAuthConfig.authorizationParams) {
            param = params[key];
            finalURL = finalURL + "&" + key + "=" + encodeURIComponent(param);
          }
        }
        console.log(finalURL);
        var tokenGetterWindow = window.open(finalURL, 'Authorize Drill', "toolbar=no,menubar=no,scrollbars=yes,resizable=yes,top=500,left=500,width=450,height=600");

        var timer = setInterval(function () {
        if (tokenGetterWindow.closed) {
          clearInterval(timer);
          window.location.reload(); // Refresh the parent page
        }
      }, 1000);
    } catch (error) {
      console.error(error);
      window.alert("Cannot parse JSON.");
    }
  });
  </#if>

    function doUpdate() {
      $("#updateForm").ajaxForm({
        dataType: 'json',
        success: serverMessage,
        error: serverMessage
      });
    }

    function deleteFunction() {
      showConfirmationDialog('"${model.getPlugin().getName()}"' + ' plugin will be deleted. Proceed?', function() {
        $.ajax({
            url: '/storage/' + encodeURIComponent('${model.getPlugin().getName()}') + '.json',
            method: 'DELETE',
            contentType: 'application/json',
            success: serverMessage,
            error: function(request,msg,error) {
              serverMessage({ errorMessage: 'Error while trying to delete.' })
            }
        });
      });
    }

    // Modal window management
    $('#pluginsModal').on('show.bs.modal', function (event) {
      const button = $(event.relatedTarget); // Button that triggered the modal
      let exportInstance = button.attr("name");
      const modal = $(this);
      modal.find('.modal-title').text(exportInstance.toUpperCase() +' Plugin configs');
      modal.find('.btn-primary').click(function(){
        let format = "";
        if (modal.find('#json').is(":checked")) {
          format = 'json';
        }
        if (modal.find('#hocon').is(":checked")) {
          format = 'conf';
        }

        let url = '/storage/' + encodeURIComponent(exportInstance) + '/export/' + format;
        window.open(url);
      });
    })
  </script>
</#macro>

<@page_html/>
