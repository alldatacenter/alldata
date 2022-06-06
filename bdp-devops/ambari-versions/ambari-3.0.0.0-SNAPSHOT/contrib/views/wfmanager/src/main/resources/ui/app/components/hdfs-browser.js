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
import HdfsViewerConfig from '../utils/hdfsviewer';
export default Ember.Component.extend({
  config: HdfsViewerConfig.create(),
  uploaderService : Ember.inject.service('hdfs-file-uploader'),
  userInfo : Ember.inject.service('workspace-manager'),
  initialize:function(){
    var self=this;
    self.$("#filediv").modal("show");
    self.$("#filediv").on('hidden.bs.modal', function (e) {
      self.sendAction('closeWorkflowSubmitConfigs');
      self.sendAction("closeFileBrowser");
    });
  }.on('didInsertElement'),
  setUserData : function() {
    this.set("homeDirectory", "/user/"+this.get("userInfo").getUserName());
    this.set("selectedPath", "/user/"+this.get("userInfo").getUserName());
    this.set("filePath", "/user/"+this.get("userInfo").getUserName());
  }.on("init"),
  selectFileType: "all",//can be all/file/folder
  selectedPath:"",
  isDirectory:false,
  callback: null,
  alertMessage:null,
  alertDetails:null,
  alertType:null,
  uploadSelected: false,
  isFilePathInvalid: Ember.computed('selectedPath',function() {
    return this.get("selectedPath").indexOf("<")>-1;
  }),
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
  isUpdated : function(){
    if(this.get('showUploadSuccess')){
      this.$('#success-alert').fadeOut(5000, ()=>{
        this.set("showUploadSuccess", false);
      });
    }
  }.on('didUpdate'),
  actions: {
    viewerError(error) {
      if (error.responseJSON && error.responseJSON.message && error.responseJSON.message.includes("Permission")) {
        this.showNotification({"type": "error", "message": "Permission denied"});
      }
    },
    createFolder(){
      var self=this;
      var $elem=this.$('input[name="selectedPath"]');
      //$elem.val($elem.val()+"/");
      var folderHint="<enter folder here>";
      this.set("selectedPath",this.get("selectedPath")+"/"+folderHint);
      setTimeout(function(){
        $elem[0].selectionStart = $elem[0].selectionEnd = self.get("selectedPath").length-folderHint.length;
      },10);

      $elem.focus();

    },
    viewerSelectedPath(data) {
      this.set("selectedPath",data.path);
      this.set("filePath",data.path);
      this.set("isDirectory",data.isDirectory);
      this.set("alertMessage",null);
    },
    selectFile(){
      if (this.get("selectedPath")===""){
        this.showNotification( {"type": "error", "message": "Please fill the settings value"});
        return false;
      }
      if (this.get("selectFileType")==="folder" && !this.get("isDirectory")){
        this.showNotification( {"type": "error", "message": "Only folders can be selected"});
        return false;
      }
      this.set("filePath",this.get("selectedPath"));
      this.$("#filediv").modal("hide");
    },
    uploadSelect(){
      this.set("uploadSelected",true);
    },

    closeUpload(){
      this.set("uploadSelected",false);
    },
    uploadSuccess(e){
      this.get('uploaderService').trigger('uploadSuccess');
      this.set('uploadSelected', false);
      this.set('showUploadSuccess', true);
    },
    uploadFailure(textStatus,errorThrown){
      this.showNotification({
        "type": "error",
        "message": "Upload Failed",
        "details":textStatus,
        "errorThrown":errorThrown
      });
    },
    uploadProgress(e){
    },
    uploadValidation(e){
      this.showNotification({
        "type": "error",
        "message": "Upload Failed",
        "details":e
      });
    }
  }
});
