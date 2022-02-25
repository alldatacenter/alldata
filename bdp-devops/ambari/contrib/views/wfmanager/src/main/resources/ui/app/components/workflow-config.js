/*
*    Licensed to the Apache Software Foundation (ASF) under one or more
*    contributor license agreements.  See the NOTICE file distributed with
*    this work for additional information regarding copyright ownership.
*    The ASF licenses this file to You under the Apache License, Version 2.0
*    (the "License"); you may not use this file except in compliance with
*    the License.  You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/

import Ember from 'ember';
import Constants from '../utils/constants';
export default Ember.Component.extend({
  systemConfigs : Ember.A([]),
  initialize :function(){
    this.set("configPropsExists",this.get("workflowSubmitConfigs").props.size>0);
    var workflowProps =[];
    var workflowParams = this.get("workflowSubmitConfigs").params;
    if(workflowParams && workflowParams.configuration && workflowParams.configuration.property){
      workflowParams.configuration.property.forEach((param)=>{
        if(param && !param.value){
          var prop= Ember.Object.create({
            name: param.name,
            value: null,
            isRequired : true
          });
          workflowProps.push(prop);
        }
      });
    }
    this.get("workflowSubmitConfigs").props.forEach(function(value) {
      if (value!=="${nameNode}" && value!==Constants.rmDefaultValue){
        var propName = value.trim().substring(2, value.length-1);
        var isRequired = true;
        if(workflowParams && workflowParams.configuration && workflowParams.configuration.property){
          var param = workflowParams.configuration.property.findBy('name', propName);
          if(param && param.value){
            isRequired = false;
          }else {
            isRequired = true;
          }
        }
        var prop= Ember.Object.create({
          name: propName,
          value: null,
          isRequired : isRequired
        });
        workflowProps.push(prop);
      }
    });
    this.set("configMap",workflowProps);
    this.set("workflowXml",this.get("workflowSubmitConfigs").xml);
    this.set('systemConfigs', Ember.A([]));
    this.get('systemConfigs').pushObjects([
      {displayName: 'Run on submit',name : 'runOnSubmit', value: false},
      {displayName: 'Use system lib path', name :'useSystemLibPath', value:true},
      {displayName: 'Rerun on Failure', name : 'rerunOnFailure', value:true}
    ]);
    this.set('filePath', Ember.copy(this.get('workflowFilePath')));
  }.on('init'),
  rendered : function(){
    this.$("#configureWorkfowModal").on('hidden.bs.modal', function () {
      this.sendAction('closeWorkflowSubmitConfigs');
    }.bind(this));
    this.$("#configureWorkfowModal").modal("show");
  }.on('didInsertElement'),
  showingFileBrowser: false,
  workflowXml:"",
  workflowName: "",
  overwriteWorkflowPath: false,
  configMap : Ember.A([]),
  configPropsExists: false,
  savingInProgress: false,
  alertType: "",
  alertMessage:"",
  alertDetails:"",
  filePath : "",
  showNotification(data){
    if (!data){
      return;
    }
    if (data.type==="success"){
      this.set("alertType","success");
    }
    if (data.type==="error"){
      this.set("alertType","danger");
    }
    this.set("alertDetails",data.details);
    this.set("alertMessage",data.message);
  },
  submitWorkflow(){
    var self=this;
    this.set('workflowFilePath', Ember.copy(this.get('filePath')));
    var url = Ember.ENV.API_URL + "/submitWorkflow?app.path=" +this.get("filePath")+"&overwrite="+this.get("overwriteWorkflowPath");
    if (this.get("filePath").trim() === ""){//TODO later proper validations.
      self.showNotification({
        "type": "error",
        "message": "Workflow File Path cannot be empty"
      });
      return;
    }
    var submitConfigs=this.get("configMap");
    var missingConfig=false;
    submitConfigs.forEach(function(item) {
      if (item.isRequired && (!item || !item.value || item.value==="")){
        missingConfig = true;
      }else if(!item.isRequired && (!item || !item.value || item.value==="")){
        return;
      }else{
        url = url + "&config." + item.name + "=" + item.value;
      }
    }, this);
    this.get('systemConfigs').forEach((config)=>{
      url = url + "&oozieconfig." + config.name + "=" + config.value;
    });
    if ( this.get("workflowSubmitConfigs").props.has("${resourceManager}")){
      url= url+"&resourceManager=useDefault";
    }
    if (missingConfig){
      self.showNotification({
        "type": "error",
        "message": "You need to fill all the mandatory job properties."
      });
      return;
    }

    this.set("savingInProgress",true);
    Ember.$.ajax({
      url: url,
      method: "POST",
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function(request) {
        request.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        request.setRequestHeader("X-Requested-By", "workflow-designer");
      },
      data: this.get("workflowXml"),
      success: function(response) {
        var result=JSON.parse(response);
        this.showNotification({
          "type": "success",
          "message": "Workflow saved.",
          "details": "Job id :"+result.id
        });
        this.set("savingInProgress",false);
        var runOnSubmit = this.get('systemConfigs').findBy('name','runOnSubmit');
        if(runOnSubmit.value){
          this.startJob(result.id);
        }
      }.bind(this),
      error: function(response) {
        self.set("savingInProgress",false);
        self.showNotification({
          "type": "error",
          "message": "Error occurred while saving workflow.",
          "details": this.getParsedErrorResponse(response)
        });
      }.bind(this)
    });
  },
  startJob (jobId){
    this.set('startingInProgress', true);
    var url = [Ember.ENV.API_URL,
      "/v2/job/", jobId, "?action=", 'start','&user.name=oozie'
    ].join("");
    Ember.$.ajax({
      url: url,
      method: 'PUT',
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "Ambari");
      }
    }).done(function(){
      this.set('startingInProgress', false);
      this.showNotification({
        "type": "success",
        "message": "Workflow Started",
        "details": jobId
      });
    }.bind(this)).fail(function(response){
      this.set('startingInProgress', false);
      this.showNotification({
        "type": "error",
        "message": "Error occurred while starting workflow.",
        "details": this.getParsedErrorResponse(response)
      });
    }.bind(this));
  },
  getParsedErrorResponse (response){
    var detail;
    if (response.responseText && response.responseText.charAt(0)==="{"){
      var jsonResp=JSON.parse(response.responseText);
      if (jsonResp.status==="workflow.oozie.error"){
        detail="Oozie error. Please check the workflow.";
      }else{
        detail=jsonResp.message;
      }
    }else{
      detail=response;
    }
    return detail;
  },
  actions: {
    selectWorflowFile(){
      this.set("showingFileBrowser",true);
    },
    closeFileBrowser(){
      this.set("showingFileBrowser",false);
    },
    save(){
      this.submitWorkflow();
      return false;
    },
    previewXml(){
      this.set("showingPreview",true);
    },
    closePreview(){
      this.set("showingPreview",false);
    }
  }
});
