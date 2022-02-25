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
  clipboardHasContents : Ember.computed.oneWay('clipboard', function(){
    return !Ember.isEmpty(this.get('clipboard'));
  }),
  initialize : function(){
    this.set('customActionEnabled', Constants.customActionEnabled);
  }.on('init'),
  actions : {
    addAction : function(type){
      this.$(".dr_action").css("background-color", "#fff");
      this.$("[data-type="+type+"]").css("background-color", "#538EC0");
      this.$(this.get('element')).popover('hide');
      this.sendAction("addNode", type);
    },
    pasteNode(){
      this.$(this.get('element')).popover('hide');
      this.sendAction("pasteNode");
    },
    importActionNodeLocalFS(file) {
      this.$(this.get('element')).popover('hide');
      this.sendAction("importActionNodeLocalFS", file);
    },
    showImportActionNodeFileBrowser() {
      this.$(this.get('element')).popover('hide');
      this.sendAction("showImportActionNodeFileBrowser");
    },
    showAssetNodeList() {
      this.$(this.get('element')).popover('hide');
      this.sendAction("showAssetNodeList", true);
    }
  }
});
