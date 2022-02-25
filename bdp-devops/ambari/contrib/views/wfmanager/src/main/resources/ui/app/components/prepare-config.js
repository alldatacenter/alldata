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
  multivalued: true,
  prepareType : 'mkdir',
  fileBrowser : Ember.inject.service('file-browser'),
  initialize : function(){
    this.on('fileSelected',function(fileName){
      if(!Ember.isBlank(this.get('filePathModel'))){
        var prepareObj = this.get('prepare').objectAt(this.get('filePathModel'));
        Ember.set(prepareObj,"path", fileName);
        this.get('prepare').replace(this.get('filePathModel'), 1, prepareObj);
      }else{
        this.set('preparePath', fileName);
      }
    }.bind(this));
    this.on('bindInputPlaceholder',function () {
      this.set('addUnboundValue', true);
    }.bind(this));
    this.sendAction('register', 'prepare', this);
  }.on('init'),
  bindInputPlaceholder : function(){
    if(this.get('addUnboundValue') && this.get('prepareType') && this.get('preparePath')){
      this.addPrepare();
    }
  }.on('willDestroyElement'),
  addPrepare : function (){
    this.get('prepare').pushObject({type:this.get('prepareType'),path:this.get('preparePath')});
    this.set('prepareType', "mkdir");
    this.set('preparePath', "");
    this.$('#prepare-type-select').prop('selectedIndex', 0);
  },
  actions : {
    onPrepareTypeChange (value) {
      this.set('prepareType', value);
    },
    addPrepare () {
      this.addPrepare();
    },
    deletePrepare(index){
      this.get('prepare').removeAt(index);
    },
    openFileBrowser(model){
      this.set('filePathModel', model);
      this.sendAction("openFileBrowser", model, this);
    }
  }
});
