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
  showingFileBrowser : false,
  fileBrowser : Ember.inject.service('file-browser'),
  setUp : function(){
    if(this.get('actionModel') === undefined || this.get('actionModel') === null){
      this.set('actionModel',{});
    }
    if(this.get('actionModel.jobXml') === undefined){
      this.set("actionModel.jobXml", Ember.A([]));
    }
    if(this.get('actionModel.configuration') === undefined){
      this.set("actionModel.configuration",{});
      this.set("actionModel.configuration.property", Ember.A([]));
    }
    this.sendAction('register','globalConfigurations',this);
  }.on('init'),
  saveClicked : false,
  initialize : function(){
    this.$('#global_properties_dialog').modal('show');
    this.$('#global_properties_dialog').modal().on('hidden.bs.modal', function() {
      if(this.get('saveClicked')){
        this.sendAction('saveGlobalConfig');
      }else{
        this.sendAction('closeGlobalConfig');
      }
    }.bind(this));
    this.get('fileBrowser').on('fileBrowserOpened',function(context){
      this.get('fileBrowser').setContext(context);
    }.bind(this));
  }.on('didInsertElement'),
  actions : {
    register(component, context){
      this.set('nameValueContext', context);
    },
    close (){
      this.$('#global_properties_dialog').modal('hide');
      this.set('saveClicked', false);
    },
    save(){
      this.$('#global_properties_dialog').modal('hide');
      this.get("nameValueContext").trigger("bindInputPlaceholder");
      this.set('saveClicked', true);
    },
    openFileBrowser(model, context){
      if(!context){
        context = this;
      }
      this.get('fileBrowser').trigger('fileBrowserOpened',context);
      this.set('filePathModel', model);
      this.set('showingFileBrowser',true);
    },
    closeFileBrowser(){
      this.get('fileBrowser').getContext().trigger('fileSelected', this.get('filePath'));
      this.set("showingFileBrowser", false);
    }
  }
});
