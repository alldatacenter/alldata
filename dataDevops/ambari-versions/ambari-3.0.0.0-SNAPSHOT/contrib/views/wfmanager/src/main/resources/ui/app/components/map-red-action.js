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
  hasStaticProps : true,
  fileBrowser : Ember.inject.service('file-browser'),
  staticProps : Ember.A([]),
  setUp : function(){
    if(this.get('actionModel.files') === undefined){
      this.set("actionModel.files", Ember.A([]));
    }
    if(this.get('actionModel.jobXml') === undefined){
      this.set("actionModel.jobXml", Ember.A([]));
    }
    if(this.get('actionModel.archives') === undefined){
      this.set("actionModel.archives", Ember.A([]));
    }
    if(this.get('actionModel.prepare') === undefined){
      this.set("actionModel.prepare", Ember.A([]));
    }
    if(this.get('actionModel.configuration') === undefined){
      this.set("actionModel.configuration",{});
      this.set("actionModel.configuration.property", Ember.A([]));
    }
    this.get('staticProps').clear();
    this.get('staticProps').pushObjects([
      {name:'mapred.mapper.class',displayName:'Mapper class', value:'', belongsTo:'actionModel.configuration.property'},
      {name:'mapred.reducer.class',displayName:'Reducer class', value:'', belongsTo:'actionModel.configuration.property'},
      {name:'mapred.map.tasks',displayName:'No of Tasks', value:'', belongsTo:'actionModel.configuration.property'},
      {name:'mapred.input.dir',displayName:'Input dir', value:'', belongsTo:'actionModel.configuration.property'},
      {name:'mapred.output.dir',displayName:'Output dir', value:'', belongsTo:'actionModel.configuration.property'}
    ]);
    this.get('staticProps').forEach((property)=>{
      var propertyExists = this.get(property.belongsTo);
      if(!propertyExists){
        return;
      }
      var existingStaticProp = this.get(property.belongsTo).findBy('name',property.name);
      if(existingStaticProp){
        this.get(property.belongsTo).removeObject(existingStaticProp);
        Ember.set(property,'value',existingStaticProp.value);
        Ember.set(existingStaticProp,'static', true);
      }
    });
  }.on('init'),
  initialize : function(){
    this.on('fileSelected',function(fileName){
      this.set(this.get('filePathModel'), fileName);
    }.bind(this));
    this.sendAction('register','mapRedAction', this);
  }.on('didInsertElement'),
  observeError :function(){
    if(this.$('#collapseOne label.text-danger').length > 0 && !this.$('#collapseOne').hasClass("in")){
      this.$('#collapseOne').collapse('show');
    }
  }.on('didUpdate'),
  actions : {
    openFileBrowser(model, context){
      if(undefined === context){
        context = this;
      }
      this.set('filePathModel', model);
      this.sendAction('openFileBrowser', model, context);
    },
    register (name, context){
      this.sendAction('register',name , context);
    }
  }
});
