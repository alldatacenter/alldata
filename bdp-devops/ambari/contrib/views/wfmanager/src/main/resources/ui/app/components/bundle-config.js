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
import {Bundle} from '../domain/bundle/bundle';
import {BundleGenerator} from '../domain/bundle/bundle-xml-generator';
import {BundleXmlImporter} from '../domain/bundle/bundle-xml-importer';
import { validator, buildValidations } from 'ember-cp-validations';
import Constants from '../utils/constants';
import SchemaVersions from '../domain/schema-versions';

const Validations = buildValidations({
  'bundle.name': validator('presence', {
    presence : true
  }),
  'bundle.coordinators': {
    validators: [
      validator('operand-length', {
        min : 1,
        dependentKeys: ['bundle','bundle.coordinators.[]'],
        message : 'At least one coordinator is required',
        disabled(model, attribute) {
          return !model.get('bundle');
        }
      })
    ]
  }
});

export default Ember.Component.extend(Ember.Evented, Validations, {
  bundle : null,
  schemaVersions : SchemaVersions.create({}),
  propertyExtractor : Ember.inject.service('property-extractor'),
  fileBrowser : Ember.inject.service('file-browser'),
  workspaceManager : Ember.inject.service('workspace-manager'),
  jobConfigProperties: Ember.A([]),
  isDefaultNameForBundleEnabled : false,
  initialize : function(){
    var self = this;
    this.set('errors', Ember.A([]));
    this.get('workspaceManager').restoreWorkInProgress(this.get('tabInfo.id')).promise.then(function(draftBundle){
      self.loadBundle(draftBundle);
    }.bind(this)).catch(function(data){
      self.loadBundle();
    }.bind(this));
    this.get('fileBrowser').on('fileBrowserOpened',function(context){
      this.get('fileBrowser').setContext(context);
    }.bind(this));
    this.on('fileSelected',function(fileName){
      this.set(this.get('filePathModel'), fileName);
    }.bind(this));
    this.set('showErrorMessage', false);
  }.on('init'),
  onDestroy : function(){
    Ember.run.cancel(this.schedulePersistWorkInProgress);
    this.persistWorkInProgress();
  }.on('willDestroyElement'),
  observeFilePath : Ember.observer('bundleFilePath', function(){
    if(!this.get('bundleFilePath') || null === this.get('bundleFilePath')){
      return;
    }else{
      this.sendAction('changeFilePath', this.get('tabInfo'), this.get('bundleFilePath'));
    }
  }),
  nameObserver : Ember.observer('bundle.name', function(){
    if(!this.get('bundle')){
      return;
    }else if(this.get('bundle') && Ember.isBlank(this.get('bundle.name'))){
      if(!this.get('clonedTabInfo')){
        this.set('clonedTabInfo', Ember.copy(this.get('tabInfo')));
      }
      this.sendAction('changeTabName', this.get('tabInfo'), this.get('clonedTabInfo.name'));
    }else{
      this.sendAction('changeTabName', this.get('tabInfo'), this.get('bundle.name'));
    }
  }),
  bundleFilePath : Ember.computed('tabInfo.filePath', function(){
    return this.get('tabInfo.filePath');
  }),
  loadBundle(draftBundle){
    if(draftBundle){
      this.set('bundle', JSOG.parse(draftBundle));
    }else{
      this.set('bundle', this.createBundle());
    }
    if(Ember.isBlank(this.get('bundle.name')) && this.get('isDefaultNameForBundleEnabled')){
      this.set('bundle.name', Ember.copy(this.get('tabInfo.name')));
    }
    this.schedulePersistWorkInProgress();
  },
  schedulePersistWorkInProgress (){
    Ember.run.later(function(){
      this.persistWorkInProgress();
      this.schedulePersistWorkInProgress();
    }.bind(this), Constants.persistWorkInProgressInterval);
  },
  persistWorkInProgress (){
    if(!this.get('bundle')){
      return;
    }
    var json = JSOG.stringify(this.get("bundle"));
    this.get('workspaceManager').saveWorkInProgress(this.get('tabInfo.id'), json);
  },
  createBundle (){
    return Bundle.create({
      name : '',
      kickOffTime : {
        value : '',
        displayValue : '',
        type : 'date'
      },
      coordinators : null,
      schemaVersions : {
        bundleVersion : this.get('schemaVersions').getDefaultVersion('bundle')
      },
      xmlns : "uri:oozie:bundle:" +this.get('schemaVersions').getDefaultVersion('bundle'),
      draftVersion: 'v1'
    });
  },
  importSampleBundle (){
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: "/sampledata/bundle.xml",
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
  importBundle (filePath){
    if (!filePath) {
      return;
    }
    this.set("isImporting", true);
    filePath = this.appendFileName(filePath, 'bundle');
    this.set("bundleFilePath", filePath);
    var deferred = this.getBundleFromHdfs(filePath);
    deferred.promise.then(function(response){
      if(response.type === 'xml'){
        this.getBundleFromXml(response.data);
      }else{
        this.getBundleFromJSON(response.data);
      }
      this.set('bundleFilePath', filePath);
      this.set("isImporting", false);
    }.bind(this)).catch(function(data){
      console.error(data);
      this.set("errorMsg", "There is some problem while importing.");
      this.set("isImporting", false);
      this.set("isImportingSuccess", false);
      this.set("data", data);
    }.bind(this));
  },
  getBundleFromJSON(draftBundle){
    this.set('bundle', JSOG.parse(draftBundle));
  },
  getBundleFromHdfs(filePath){
    var url =  Ember.ENV.API_URL + "/readWorkflow?workflowPath="+filePath+"&jobType=BUNDLE";
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: url,
      method: 'GET',
      dataType: "text",
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "Ambari");
      }
    }).done(function(data, status, xhr){
      var type = xhr.getResponseHeader("response-type") === "xml" ? 'xml' : 'json';
      deferred.resolve({data : data, type : type});
    }).fail(function(e){
      console.error(e);
      deferred.reject(e);
    });
    return deferred;
  },
  getFromHdfs(filePath){
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
    }).fail(function(e){
      console.error(e);
      deferred.reject(e);
    });
    return deferred;
  },
  getBundleFromXml(bundleXml){
    var bundleXmlImporter = BundleXmlImporter.create();
    var bundleObj = bundleXmlImporter.importBundle(bundleXml);
    this.get("errors").clear();
    this.get("errors").pushObjects(bundleObj.errors);
    if (bundleObj.bundle) {
      this.set("bundle", bundleObj.bundle);
    }
  },
  getJobProperties(coordinatorPath){
    var deferred = Ember.RSVP.defer();
    coordinatorPath = this.appendFileName(coordinatorPath, 'coord');
    this.getFromHdfs(coordinatorPath).promise.then((coordinatorXml)=>{
      var x2js = new X2JS();
      var coordProps = this.get('propertyExtractor').getDynamicProperties(coordinatorXml);
      var coordinatorJson = x2js.xml_str2json(coordinatorXml);
      var workflowPath = coordinatorJson['coordinator-app']['action']['workflow']['app-path'];
      if(this.get('propertyExtractor').containsParameters(workflowPath)){
        this.set('containsParameteriedPaths', true);
        deferred.resolve(Array.from(coordProps.values()));
      }else{
        workflowPath = this.appendFileName(workflowPath, 'wf');
        this.getFromHdfs(workflowPath).promise.then((workflowXml)=>{
          var workflowProps = this.get('propertyExtractor').getDynamicProperties(workflowXml);
          deferred.resolve(Array.from(coordProps.values()).concat(Array.from(workflowProps.values())));
        });
      }
    }.bind(this)).catch((e)=>{
      deferred.reject({trace :e, path: coordinatorPath});
    });
    return deferred;
  },
  appendFileName(filePath, type){
    if(filePath.endsWith('.wfdraft')){
      return filePath;
    }else if(!filePath.endsWith('.xml') && type === 'bundle'){
      return filePath = `${filePath}/bundle.xml`;
    }else if(!filePath.endsWith('.xml') && type === 'coord'){
      return filePath = `${filePath}/coordinator.xml`;
    }else if(!filePath.endsWith('.xml') && type === 'wf'){
      return filePath = `${filePath}/workflow.xml`;
    }else{
      return filePath;
    }
  },
  actions : {
    closeFileBrowser(){
      this.set("showingFileBrowser", false);
      this.get('fileBrowser').getContext().trigger('fileSelected', this.get('filePath'));
      if(this.get('filePathModel') === 'bundleFilePath'){
        this.importBundle(Ember.copy(this.get('bundleFilePath')));
        this.set('bundleFilePath', null);
      }
    },
    openFileBrowser(model, context){
      if(!context){
        context = this;
      }
      this.get('fileBrowser').trigger('fileBrowserOpened',context);
      this.set('filePathModel', model);
      this.set('showingFileBrowser', true);
    },
    createCoordinator(){
      this.set('coordinatorEditMode', false);
      this.set('coordinatorCreateMode', true);
      this.set('currentCoordinator',{
        name : undefined,
        appPath : undefined,
        configuration : {
          property : Ember.A([])
        }
      });
    },
    editCoordinator(index){
      this.set('coordinatorEditMode', true);
      this.set('coordinatorCreateMode', false);
      this.set('currentCoordinatorIndex', index);
      this.set('currentCoordinator', Ember.copy(this.get('bundle.coordinators').objectAt(index)));
    },
    addCoordinator(){
      if(!this.get('bundle.coordinators')){
        this.set('bundle.coordinators', Ember.A([]));
      }
      this.get('bundle.coordinators').pushObject(Ember.copy(this.get('currentCoordinator')));
      this.set('coordinatorCreateMode', false);
    },
    updateCoordinator(){
      this.get('bundle.coordinators').replace(this.get('currentCoordinatorIndex'), 1, Ember.copy(this.get('currentCoordinator')));
      this.set('coordinatorEditMode', false);
    },
    deleteCoordinator(index){
      this.get('bundle.coordinators').removeAt(index);
      if(index === this.get('currentCoordinatorIndex')){
        this.set('coordinatorEditMode', false);
      }
    },
    cancelCoordinatorOperation(){
      this.set('coordinatorCreateMode', false);
      this.set('coordinatorEditMode', false);
    },
    confirmReset(){
      this.set('showingResetConfirmation', true);
    },
    resetBundle(){
      this.get('errors').clear();
      this.set('showingResetConfirmation', false);
      if(this.get('bundleFilePath')){
        this.importBundle(this.get('bundleFilePath'));
      }else {
        this.set('bundle', this.createBundle());
      }
    },
    closeBundleSubmitConfig(){
      this.set("showingJobConfig", false);
    },
    submitBundle(){
      if(this.get('validations.isInvalid')) {
        this.set('showErrorMessage', true);
        return;
      }
      var bundleGenerator = BundleGenerator.create({bundle:this.get("bundle")});
      var bundleXml = bundleGenerator.process();
      var propertyPromises = [];
      this.$('#loading').show();
      this.get('bundle.coordinators').forEach((coordinator) =>{
        if(this.get('propertyExtractor').containsParameters(coordinator.appPath)){
          this.set('containsParameteriedPaths', true);
          return;
        }
        var deferred = this.getJobProperties(coordinator.appPath);
        propertyPromises.push(deferred.promise);
      }, this);
      Ember.RSVP.Promise.all(propertyPromises).then(function(props){
        var combinedProps = [];
        var excludedProps = [];
        props.forEach((prop, index)=>{
          var coordinator = this.get('bundle.coordinators').objectAt(index);
          if(coordinator.configuration && coordinator.configuration.property){
            coordinator.configuration.property.forEach((config) => {
              var idx = prop.indexOf('${'+config.name+'}');
              if(idx >= 0){
                excludedProps.push('${'+config.name+'}');
              }
            });
          }
          combinedProps = combinedProps.concat(prop);
        });
        var dynamicProperties = this.get('propertyExtractor').getDynamicProperties(bundleXml);
        combinedProps.forEach((prop)=>{
          if(excludedProps.indexOf(prop) >= 0){
            return;
          }
          dynamicProperties.set(prop, prop);
        });
        this.$('#loading').hide();
        var configForSubmit = {props : Array.from(dynamicProperties.values(), key => key), xml : bundleXml, params : this.get('bundle.parameters')};
        this.set("bundleConfigs", configForSubmit);
        this.set("showingJobConfig", true);
      }.bind(this)).catch(function(e){
        this.$('#loading').hide();
        this.get("errors").pushObject({'message' : 'Could not process coordinator from ' + e.path});
        throw new Error(e);
      }.bind(this));
    },
    preview(){
      if(this.get('validations.isInvalid')) {
        this.set('showErrorMessage', true);
        return;
      }
      this.set("showingPreview", false);
      var bundleGenerator = BundleGenerator.create({bundle:this.get("bundle")});
      var bundleXml = bundleGenerator.process();
      this.set("previewXml", vkbeautify.xml(bundleXml));
      this.set("showingPreview", true);
    },
    closePreview(){
      this.set("showingPreview", false);
    },
    importBundleTest(){
      var deferred = this.importSampleBundle();
      deferred.promise.then(function(data){
        this.getBundleFromXml(data);
      }.bind(this)).catch(function(data){
        console.error(data);
        this.set("errorMsg", "There is some problem while importing.");
        this.set("data", data);
      });
    },
    openTab(type, path){
      this.sendAction('openTab', type, path);
    },
    showVersionSettings(value){
      this.set('showVersionSettings', value);
    },
    save(){
      if (Ember.isBlank(this.$('[name=bundle_title]').val())) {
        return;
      }
      var isDraft = false, bundleXml;
      if(this.get('validations.isInvalid')) {
        isDraft = true;
      }else{
        var bundleGenerator = BundleGenerator.create({bundle:this.get("bundle")});
        bundleXml = bundleGenerator.process();
      }
      var bundleJson = JSOG.stringify(this.get("bundle"));
      this.set("configForSave",{json:bundleJson, xml:bundleXml, isDraft: isDraft});
      this.set("showingSaveWorkflow", true);
    },
    closeSave(){
      this.set("showingSaveWorkflow", false);
    }
  }
});
