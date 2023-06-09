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
<style>
  .modalHeaderAlert, .closeX {
    color:red !important;
    text-align: center;
    font-size: 16px;
  }
</style>
  <!--
    Alert Modal to use across templates.
    By default, modal is hidden and expected to be populated and activated by relevant JavaScripts
  -->
  <div class="modal" id="errorModal" role="dialog" data-backdrop="static" data-keyboard="false" style="display: none;" aria-hidden="true">
    <div class="modal-dialog">
      <!-- Modal content-->
      <div class="modal-content">
        <div class="modal-header modalHeaderAlert">
          <h4 class="modal-title col-11"><span class="material-icons" style="font-size:125%">warning</span><span id="modalHeader" style="font-family:Helvetica Neue,Helvetica,Arial,sans-serif;white-space:pre">~ErrorMessage~ Title</span></h4>
          <button type="button" class="close closeX" data-dismiss="modal"><span class="material-icons" style="color:red;font-size:125%">close</span></button>
        </div>
        <div class="modal-body" id="modalBody" style="line-height:3">
        ~ErrorMessage Details~
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
        </div>
      </div>
    </div>
  </div>
<script>
    //Populate the alert modal with the right message params and show
    function populateAndShowAlert(errorMsg, inputValues) {
      let errorModal=$('#errorModal');
      let title;
      let body;
      if (!(errorMsg in errorMap)) {
        //Using default errorId to represent message
        title=errorMsg;
        body="[Auto Description] "+JSON.stringify(inputValues);
      } else {
        title=errorMap[errorMsg].msgHeader;
        body=errorMap[errorMsg].msgBody;
      }
      //Check if substitutions are needed
      if (inputValues != null) {
        let inputValuesKeys = Object.keys(inputValues);
        for (i=0; i<inputValuesKeys.length; ++i) {
          let currKey=inputValuesKeys[i];
          body=body.replace(currKey, escapeHtml(inputValues[currKey]));
        }
      }
      errorModal.find('.modal-title').text(title);
      errorModal.find('.modal-body').html(body);
      //Show Alert
      errorModal.modal('show');
    }

    function escapeHtml(str) {
        return str.replace(/&/g,'&amp;')
            .replace(/</g,'&lt;')
            .replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;')
            .replace(/'/g,'&#x27;')
            .replace(/\//g,'&#x2F;');
    }

    //Map of error messages to populate the alert modal
    var errorMap = {
        "userNameMissing": {
            msgHeader:"   ERROR: Username Needed",
            msgBody:"Please provide a user name. The field cannot be empty.<br>Username is required since impersonation is enabled"
        },
        "passwordMissing": {
            msgHeader:"   ERROR: Password Needed",
            msgBody:"Please provide a password. The field cannot be empty."
        },
        "invalidRowCount": {
            msgHeader:"   ERROR: Invalid RowCount",
            msgBody:"\"<span style='font-family:courier;white-space:pre'>_autoLimitValue_</span>\" is not a number. Please fill in a valid number.",
        },
        "invalidProfileFetchSize": {
            msgHeader:"   ERROR: Invalid Fetch Size",
            msgBody:"\"<span style='font-family:courier;white-space:pre'>_fetchSize_</span>\" is not a valid fetch size.<br>Please enter a valid number of profiles to fetch (1 to 100000)",
        },
        "invalidOptionValue": {
            msgHeader:"   ERROR: Invalid Option Value",
            msgBody:"\"<span style='font-family:courier;white-space:pre'>_numericOption_</span>\" is not a valid numeric value for <span style='font-family:courier;white-space:pre'>_optionName_</span><br>Please enter a valid number to update the option.",
        },
        "queryMissing": {
            msgHeader:"   ERROR: No Query to execute",
            msgBody:"Please provide a query. The query textbox cannot be empty."
        },
        "pluginEnablingFailure": {
            msgHeader:"   ERROR: Unable to enable/disable plugin",
            msgBody:"<span style='font-family:courier;white-space:pre'>_pluginName_</span>: _errorMessage_"
        },
        "errorId": {
            msgHeader:"~header~",
            msgBody:"Description unavailable"
        }
    }
</script>
