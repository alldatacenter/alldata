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

<!--
  Confirmation Modal to use across templates.
  By default, modal is hidden and expected to be populated and activated by relevant JavaScripts
-->
<div class="modal fade" id="confirmationModal" role="dialog" aria-labelledby="configuration">
  <div class="modal-dialog">
    <!-- Modal content-->
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title col-11" id="confirmation" style="color:orange" align="center"><span class="material-icons">warning</span><span id="modalHeader" style="font-family:Helvetica Neue,Helvetica,Arial,sans-serif;white-space:pre">  Warning</span></h3>
        <button type="button" class="close closeX" data-dismiss="modal"><span class="material-icons">close</span></button>
      </div>
      <div class="modal-body" id="modalBody" style="font-size:125%">
      ~ConfirmationMessage~
      </div>
      <div class="modal-footer">
        <button id="confirmationOk" type="button" class="btn btn-success" data-dismiss="modal" style="width:20%">Confirm</button>
        <button id="confirmationCancel" type="button" class="btn btn-primary" data-dismiss="modal" style="width:20%">Cancel</button>
      </div>
    </div>
  </div>
</div>

<script>
    //Populate the confirmation modal with the right message params and show
    function showConfirmationDialog(confirmationMessage, okCallback) {
      let confirmationModal = $('#confirmationModal');
      confirmationModal.find('.modal-body').html(confirmationMessage);
      //Show dialog
      confirmationModal.modal('show');
      $('#confirmationOk').unbind('click')
          .click(okCallback);
      $('#confirmationCancel').focus();
    }
</script>
