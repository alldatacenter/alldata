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

export default Ember.Component.extend({
  mkdirORdeleteORtouchz: true,
  mkdir: 1,
  delete: 0,
  touchz: 0,
  chmod: 0,
  move: 0,
  chgrp: 0,
  multivalued: true,
  prepareType: 'mkdir',
  fileBrowser: Ember.inject.service('file-browser'),
  initialize: function() {
    this.on('fileSelected', (fileName)=>{
      var filePathModel = this.get('filePathModel');
      if(filePathModel && filePathModel.hasOwnProperty("index") && filePathModel.hasOwnProperty("property")){
        var fileOperation = this.get('fsOps').objectAt(filePathModel.index);
        Ember.set(fileOperation, filePathModel.property, fileName);
      }else{
        this.set(this.get('filePathModel'), fileName);
      }
    }.bind(this));
    this.on('bindInputPlaceholder', function() {
      this.set('addUnboundValue', true);
    }.bind(this));
    this.sendAction('register', 'fsOps', this);
  }.on('init'),
  bindInputPlaceholder: function() {
    let type = this.get("prepareType");
    if (this.validateOperations(type)) {
      let value = this.get("prepareType");
      if (value === "chgrp" && this.get('path') && this.get('group')) {
        this.addPrepare();
      } else if (value === "move" && this.get('source') && this.get('target')) {
        this.addPrepare();
      } else if (value === "chmod" && this.get('path')) {
        this.addPrepare();
      } else if (value === "mkdir" || value === "delete" || value === "touchz" && this.get('path')) {
        this.addPrepare();
      }
    }
  }.on('willDestroyElement'),
  formPermissions(r, w, e, type){
    var perm = 0, permObj = {};
    if(r){
      perm = perm+4;
      permObj[type+"read"] = true;
    }
    if(w){
      perm = perm+2;
      permObj[type+"write"] = true;
    }
    if(e){
      perm = perm+1;
      permObj[type+"execute"] = true;
    }
    permObj[type+"perm"] = perm;
    return permObj;
  },
  addPrepare: function() {

    let value = this.get("prepareType");
    switch (value) {
      case "mkdir":
      case "delete":
      case "touchz":
      this.get('fsOps').pushObject({
        path :this.get('path'),
        type: value
      });
      break;
      case "chmod":
      var oPerm = this.formPermissions(this.get("oread"), this.get("owrite"), this.get("oexecute"), "o");
      var gPerm = this.formPermissions(this.get("gread"), this.get("gwrite"), this.get("gexecute"), "g");
      var rPerm = this.formPermissions(this.get("rread"), this.get("rwrite"), this.get("rexecute"), "r");
      var permissionsObj = {};
      permissionsObj = Ember.$.extend(true, oPerm, gPerm);
      permissionsObj = Ember.$.extend(true, permissionsObj, rPerm);
      var perm = oPerm.operm + ""+ gPerm.gperm + ""+ rPerm.rperm;
      this.get('fsOps').pushObject({
          path: this.get('path'),
          permissions: perm,
          permissionsObj: permissionsObj,
          recursive: this.get('recursive'),
          dirfiles: this.get('dirFiles'),
        type: value
      });
      break;
      case "chgrp":
      this.get('fsOps').pushObject({
          path: this.get('path'),
          group: this.get('group'),
          recursive: this.get('recursive'),
          dirfiles: this.get('dirFiles'),
        type: value
      });
      break;
      case "move":
      this.get('fsOps').pushObject({
          source: this.get('source'),
          target: this.get('target'),
        type: value
      });
      break;
    }
    this.resetFields();
  },
  resetFields: function() {
    this.set('prepareType', "mkdir");
    this.set('path', "");
    this.set('source', "");
    this.set('target', "");
    this.set('group', "");
    this.set('permissions', "");
    this.set('recursive', false);
    this.set('dirFiles', false);
  },
  toggleAllFields: function() {
    this.set("mkdir", 0);
    this.set("delete", 0);
    this.set("chmod", 0);
    this.set("touchz", 0);
    this.set("chmod", 0);
    this.set("move", 0);
    this.set("chgrp", 0);
  },
  validateOperations: function(type) {
    var flag = true;
    switch (type) {
      case "mkdir":
      case "delete":
      case "touchz":
      if (!this.get('path')) {
        flag = false;
      }
      break;
      case "chmod":
      if (!this.get('path')) {
        flag = false;
      }
      break;
      case "chgrp":
      if (!this.get('path') || !this.get('group')) {
        flag = false;
      }
      break;
      case "move":
      if (!this.get('source') || !this.get('target')) {
        flag = false;
      }
      break;
    }
    return flag;
  },
  actions: {
    onPrepareTypeChange(value) {
      this.set('prepareType', value);
      this.toggleAllFields();
      this.set(value, 1);
      if (value === "mkdir" || value === "delete" || value === "touchz") {
        this.set("mkdirORdeleteORtouchz", true);
      } else {
        this.set("mkdirORdeleteORtouchz", false);
      }
    },
    addPrepare() {
      this.addPrepare();
    },
    deletePrepare(index) {
      this.get('fsOps').removeAt(index);
    },
    openFileBrowser(model) {
      this.set('filePathModel', model);
      this.sendAction("openFileBrowser", model, this);
    },
    openFileBrowserForListItem(index, property){
      this.set('filePathModel',{index:index, property:property});
      this.sendAction("openFileBrowser", this.get('filePathModel'), this);
    }
  }
});
