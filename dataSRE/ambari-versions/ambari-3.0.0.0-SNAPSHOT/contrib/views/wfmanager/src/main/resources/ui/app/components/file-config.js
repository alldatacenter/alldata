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
  fileBrowser : Ember.inject.service('file-browser'),
  initialize : function(){
    this.on('fileSelected', function(fileName){
      if(!Ember.isBlank(this.get('filePathModel'))){
        var file = this.get('files').objectAt(this.get('filePathModel'));
        Ember.set(file,"value", fileName);
        this.get('files').replace(this.get('filePathModel'), 1, file);
      }else{
        this.set('file', fileName);
      }
    }.bind(this));
    this.on('bindInputPlaceholder',function () {
      this.set('addUnboundValue', true);
    }.bind(this));
    this.sendAction('register', this, this);
  }.on('init'),
  bindInputPlaceholder :function (){
    if(this.get('addUnboundValue') && !Ember.isBlank(this.get('file'))){
      this.addFile();
    }
  }.on('willDestroyElement'),
  addFile (){
    this.get('files').pushObject({value:this.get('file')});
    this.set('file', "");
  },
  actions : {
    addFile () {
      this.addFile();
    },
    deleteFile(index){
      this.get('files').removeAt(index);
    },
    openFileBrowser(index){
      this.set('filePathModel', index);
      this.sendAction("openFileBrowser", index, this);
    }
  }
});
