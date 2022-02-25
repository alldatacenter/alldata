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
import { validator, buildValidations } from 'ember-cp-validations';

const Validations = buildValidations({
  'coordinator.name': validator('presence', {
    presence : true
  }),
  'coordinator.appPath': validator('presence', {
    presence : true
  })
});
export default Ember.Component.extend(Validations, {
  propertyExtractor : Ember.inject.service('property-extractor'),
  initialize : function(){
    this.on('fileSelected',function(fileName){
      this.set(this.get('filePathModel'), fileName);
    }.bind(this));
    this.set('showErrorMessage', false);
  }.on('init'),
  isValid(){
      if(this.get('validations.isInvalid')) {
        this.set('showErrorMessage', true);
        return false;
      }
      return true;
  },
  readFromHdfs(filePath){
    var url =  Ember.ENV.API_URL + "/readWorkflowXml?workflowXmlPath="+filePath;
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: url,
      method: 'GET',
      dataType: "text",
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "Ambari");
      }
    }).done(function(data){
      deferred.resolve(data);
    }).fail(function(data){
      deferred.reject(data);
    });
    return deferred;
  },
  importSampleCoordinator (){
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: "/sampledata/coordinator.xml",
      dataType: "text",
      cache:false,
      success: function(data) {
        deferred.resolve(data);
      }.bind(this),
      failure : function(data){
        deferred.reject(data);
      }
    });
    return deferred;
  },
  actions : {
    openFileBrowser(model){
      this.set('filePathModel', model);
      this.sendAction("openFileBrowser", model, this);
    },
    addCoordinator(){
      if(this.isValid()){
        this.sendAction('add');
      }
    },
    updateCoordinator(){
      if(this.isValid()){
        this.sendAction('update');
      }
    },
    cancelCoordinatorOperation(){
      this.sendAction('cancel');
    },
    openTab(type, path){
      this.sendAction('openTab', type, path);
    },
    showCoordinatorName(){
      this.set('coordinatorName', null);
      this.set('errorMsg', '');
      if (this.get('coordinator.appPath') === '') {
        return;
      }
      var path = this.get('appendFileName')(this.get('coordinator.appPath'), 'coord');
      if (this.get('propertyExtractor').containsParameters(path)) {
        this.set('containsParameteriedPaths', true);
        this.set('parameterizedPathWarning', 'Coordinator path contains variables');
        return;
      } else {
        this.set('containsParameteriedPaths', false);
        this.set('parameterizedPathWarning', '');
      }
      this.set('coordNameFetchingInProgress', true);
      var deferred = this.readFromHdfs(path);
      deferred.promise.then(function(data){
        var x2js = new X2JS();
        var coordJson = x2js.xml_str2json(data);
        this.set('coordinator.name', coordJson["coordinator-app"]._name);
        this.set('coordNameFetchingInProgress', false);
      }.bind(this)).catch(function(data){
        console.error(data);
        this.set('coordinatorName', null);
        this.set('errorMsg', 'There is some problem while fetching coordinator name.');
        this.set('data', data);
        this.set('coordNameFetchingInProgress', false);
      }.bind(this));
    }
  }
});
