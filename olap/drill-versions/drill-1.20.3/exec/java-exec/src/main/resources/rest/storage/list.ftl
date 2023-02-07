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

  <#include "*/confirmationModals.ftl">

  <h4 class="col-xs-6 mx-3">Plugin Management</h4>
  <table style="margin: 10px" class="table">
    <tbody>
    <tr>
      <td style="border:none;">
        <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#new-plugin-modal">
          Create
        </button>
        <button type="button" class="btn btn-secondary" name="all" data-toggle="modal" data-target="#pluginsModal">
          Export all
        </button>
      </td>
    </tr>
    </tbody>
  </table>

  <div class="pb-2 mt-4 mb-2 border-bottom" style="margin: 5px;"></div>

  <div class="container-fluid">
    <div class="row">
      <div class="table-responsive col-sm-12 col-md-6 col-lg-5 col-xl-5">
        <h4>Enabled Storage Plugins</h4>
        <table class="table table-hover">
          <tbody>
            <#list model as pluginModel>
              <#if pluginModel.getPlugin()?? && pluginModel.getPlugin().enabled() == true>
                <tr>
                  <td style="border:none; max-width: 200px; overflow: hidden; text-overflow: ellipsis;">
                    ${pluginModel.getPlugin().getName()}
                  </td>
                  <td style="border:none;">
                    <button type="button" class="btn btn-primary" onclick="doUpdate('${pluginModel.getPlugin().getName()}')">
                      Update
                    </button>
                    <button type="button" class="btn btn-warning" onclick="doEnable('${pluginModel.getPlugin().getName()}', false)">
                      Disable
                    </button>
                    <button type="button" class="btn btn-secondary" name="${pluginModel.getPlugin().getName()}" data-toggle="modal"
                            data-target="#pluginsModal">
                      Export
                    </button>
                  </td>
                </tr>
              </#if>
            </#list>
          </tbody>
        </table>
      </div>

      <div class="table-responsive col-sm-12 col-md-6 col-lg-7 col-xl-7">
        <h4>Disabled Storage Plugins</h4>
        <table class="table table-hover">
          <tbody>
            <#list model as pluginModel>
              <#if pluginModel.getPlugin()?? && pluginModel.getPlugin().enabled() == false>
                <tr>
                  <td style="border:none; max-width: 200px; overflow: hidden; text-overflow: ellipsis;">
                    ${pluginModel.getPlugin().getName()}
                  </td>
                  <td style="border:none;">
                    <button type="button" class="btn btn-primary" onclick="doUpdate('${pluginModel.getPlugin().getName()}')">
                      Update
                    </button>
                    <button type="button" class="btn btn-success" onclick="doEnable('${pluginModel.getPlugin().getName()}', true)">
                      Enable
                    </button>
                    <button type="button" class="btn btn-secondary" name="${pluginModel.getPlugin().getName()}" data-toggle="modal"
                            data-target="#pluginsModal">
                      Export
                    </button>
                  </td>
                </tr>
              </#if>
            </#list>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <#-- Modal window for exporting plugin config (including group plugins modal) -->
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
                <!-- Exporting to HOCON is not currently supported, see StorageResources.java. -->
                <input type="radio" name="format" id="hocon" value="conf" disabled>
                HOCON
              </label>
            </div>
          </div>

          <div id="plugin-set" class="" style="display: inline-block; position: relative; float: right;">
            <label for="format">Plugin group</label>
            <div class="radio">
              <label>
                <input type="radio" name="group" id="all" value="all" checked="checked">
                ALL
              </label>
            </div>
            <div class="radio">
              <label>
                <input type="radio" name="group" id="enabled" value="enabled">
                ENABLED
              </label>
            </div>
            <div class="radio">
              <label>
                <input type="radio" name="group" id="disabled" value="disabled">
                DISABLED
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
  <#-- Modal window for exporting plugin config (including group plugins modal) -->

  <#-- Modal window for creating plugin -->
  <div class="modal fade" id="new-plugin-modal" role="dialog" aria-labelledby="configuration">
    <div class="modal-dialog" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title" id="configuration">New Storage Plugin</h4>
          <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        </div>
        <div class="modal-body">

          <form id="createForm" role="form" action="/storage/create_update" method="POST">
            <input type="text" class="form-control" name="name" placeholder="Storage Name">
            <h3>Configuration</h3>
            <div class="form-group">
              <div id="editor" class="form-control"></div>
                <textarea class="form-control" id="config" name="config" data-editor="json" style="display: none;">
                </textarea>
            </div>
            <div style="text-align: right; margin: 10px">
              <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
              <button type="submit" class="btn btn-primary" onclick="doCreate()">Create</button>
            </div>
            <input type="hidden" name="csrfToken" value="${model[0].getCsrfToken()}">
          </form>

          <div id="message" class="d-none alert alert-info">
          </div>
        </div>
      </div>
    </div>
  </div>
  <#-- Modal window for creating plugin -->

  <script>
    function doEnable(name, flag) {
      if (flag) {
        proceed();
      } else {
        showConfirmationDialog('"' + name + '"' + ' plugin will be disabled. Proceed?', proceed);
      }
      function proceed() {
        $.post("/storage/" + encodeURIComponent(name) + "/enable/" + flag, function(data) {
          if (data.result === "Success") {
            location.reload();
          } else {
            populateAndShowAlert('pluginEnablingFailure', {'_pluginName_': name,'_errorMessage_': data.result});
          }
        }).fail(function(response) {
          populateAndShowAlert(
            'pluginEnablingFailure',
            {'_pluginName_': name,'_errorMessage_': response.responseJSON.result}
          );
        });
      }
    }

    function doUpdate(name) {
      window.location.href = "/storage/" + encodeURIComponent(name);
    }

    function doCreate() {
      $("#createForm").ajaxForm({
        dataType: 'json',
        success: serverMessage,
        error: serverMessage
      });
    }

    // Formatting create plugin textarea
    $('#new-plugin-modal').on('show.bs.modal', function() {
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
    });

    // Modal windows management
    let exportInstance; // global variable
    $('#pluginsModal').on('show.bs.modal', function(event) {
      const button = $(event.relatedTarget); // Button that triggered the modal
      const modal = $(this);
      exportInstance = button.attr("name");

      const optionalBlock = modal.find('#plugin-set');
      if (exportInstance === "all") {
        optionalBlock.removeClass('hide');
        modal.find('.modal-title').text('Export all Plugins configs');
      } else {
        modal.find('#plugin-set').addClass('hide');
        modal.find('.modal-title').text(exportInstance.toUpperCase() + ' Plugin config');
      }

      modal.find('#export').click(function() {
        let format;
        if (modal.find('#json').is(":checked")) {
          format = 'json';
        }
        if (modal.find('#hocon').is(":checked")) {
          format = 'conf';
        }
        let url;
        if (exportInstance === "all") {
          let pluginGroup = "";
          if (modal.find('#all').is(":checked")) {
            pluginGroup = 'all';
          } else if (modal.find('#enabled').is(":checked")) {
            pluginGroup = 'enabled';
          } else if (modal.find('#disabled').is(":checked")) {
            pluginGroup = 'disabled';
          }
          url = '/storage/' + pluginGroup + '/plugins/export/' + format;
        } else {
          url = '/storage/' + encodeURIComponent(exportInstance) + '/export/' + format;
        }
        window.open(url);
      });
    });
  </script>
  <#include "*/alertModals.ftl">
</#macro>

<@page_html/>
