/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership. The ASF licenses this file to
 *  You under the Apache License, Version 2.0 (the "License"); you may not use
 *  this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
 *  by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 *  OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */
var userName = null;
//Elements for Timer in LoadingModal
var elapsedTime = 0;
var delay = 1000; //msec
var timeTracker = null; //Handle for stopping watch
var userName = null;

//Show cancellation status
function popupAndWait() {
  elapsedTime=0; //Init
  $("#queryLoadingModal").modal("show");
  var stopWatchElem = $('#stopWatch'); //Get handle on time progress elem within Modal
  //Timer updating
  timeTracker = setInterval(function() {
    elapsedTime = elapsedTime + delay/1000;
    let time = elapsedTime;
    let minutes = Math.floor(time / 60);
    let seconds = time - minutes * 60;
    let prettyTime = ("0" + minutes).slice(-2)+':'+ ("0" + seconds).slice(-2);
    stopWatchElem.text('Elapsed Time : ' + prettyTime);
  }, delay);
}

//Close the cancellation status popup
function closePopup() {
  clearInterval(timeTracker);
  $("#queryLoadingModal").modal("hide");
}

// Wrap & Submit Query (invoked if impersonation is enabled to check for username)
function doSubmitQueryWithUserName() {
    userName = document.getElementById("userName").value;
    if (!userName.trim()) {
        populateAndShowAlert('userNameMissing', null);
        $("#userName").focus();
        return;
    }
    //Wrap and Submit query
    doSubmitQueryWithAutoLimit();
}

//Perform autoLimit check before submitting (invoked directly if impersonation is not enabled)
function doSubmitQueryWithAutoLimit() {
    let origQueryText = $('#query').attr('value');
    if (origQueryText == null || origQueryText.trim().length == 0) {
        populateAndShowAlert("queryMissing", null);
        $("#query").focus();
        return;
    }
    //Wrap if required
    let mustWrapWithLimit = $('input[name="forceLimit"]:checked').length > 0;
    //Clear field when submitting if not mustWrapWithLimit
    if (!mustWrapWithLimit) {
      //Wipe out any numeric entry in the field before
      $('#autoLimit').attr('value', '');
    } else {
      let autoLimitValue=document.getElementById('autoLimit').value;
      let positiveIntRegex = new RegExp("^[1-9]\\d*$");
      let isValidRowCount = positiveIntRegex.test(autoLimitValue.trim());
      if (!isValidRowCount) {
        let alertValues = {'_autoLimitValue_': autoLimitValue.trim() };
        populateAndShowAlert("invalidRowCount", alertValues);
        $('#autoLimit').focus();
        return;
      }
    }
    //Submit query
    submitQuery();
}

//Submit Query
function submitQuery() {
    popupAndWait();
    $("#queryForm").submit();
    $(window).bind("pageshow", function(event) { closePopup();});
}

//Reset Query
function resetQuery() {
	var editor = ace.edit("query-editor-format");
	editor.getSession().setValue("");
	editor.focus();
}
