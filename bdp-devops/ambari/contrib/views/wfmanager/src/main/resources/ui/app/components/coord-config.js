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
import {Coordinator} from '../domain/coordinator/coordinator';
import {CoordinatorGenerator} from '../domain/coordinator/coordinator-xml-generator';
import {CoordinatorXmlImporter} from '../domain/coordinator/coordinator-xml-importer';
import {SlaInfo} from '../domain/sla-info';
import SchemaVersions from '../domain/schema-versions';
import Constants from '../utils/constants';
import { validator, buildValidations } from 'ember-cp-validations';

const Validations = buildValidations({
  'coordinator.name': validator('presence', {
    presence : true
  }),
  'coordinator.workflow.appPath': validator('presence', {
    presence : true
  }),
  'coordinator.frequency.value': validator('presence', {
    presence : true
  }),
  'coordinator.frequency.type': validator('presence', {
    presence : true
  }),
  'coordinator.timezone': validator('presence', {
    presence : true
  })
});

export default Ember.Component.extend(Validations, Ember.Evented, {
  coordinator : null,
  errors: Ember.A([]),
  schemaVersions : SchemaVersions.create({}),
  childComponents : new Map(),
  fileBrowser : Ember.inject.service('file-browser'),
  propertyExtractor : Ember.inject.service('property-extractor'),
  workspaceManager : Ember.inject.service('workspace-manager'),
  showErrorMessage: Ember.computed.alias('saveAttempted'),
  jobConfigProperties: Ember.A([]),
  isDefaultNameForCoordinatorEnabled : false,
  datasetsForInputs : Ember.computed('coordinator.datasets.[]','coordinator.dataOutputs.[]',function(){
    var datasetsForInputs = Ember.copy(this.get('coordinator.datasets'));
    this.get('coordinator.dataOutputs').forEach((dataOutput)=>{
      var existing = datasetsForInputs.findBy('name', dataOutput.dataset);
      if(existing){
        datasetsForInputs = datasetsForInputs.without(existing);
      }
    }.bind(this));
    return datasetsForInputs;
  }),
  datasetsForOutputs : Ember.computed('coordinator.datasets.[]','coordinator.dataInputs.[]',function(){
    var datasetsForOutputs = Ember.copy(this.get('coordinator.datasets'));
    this.get('coordinator.dataInputs').forEach((dataInput)=>{
      var existing = datasetsForOutputs.findBy('name', dataInput.dataset);
      if(existing){
        datasetsForOutputs = datasetsForOutputs.without(existing);
      }
    }.bind(this));
    return datasetsForOutputs;
  }),
  onDestroy : function(){
    Ember.run.cancel(this.schedulePersistWorkInProgress);
    this.persistWorkInProgress();
  }.on('willDestroyElement'),
  initialize : function(){
    var self = this;
    this.set('errors', Ember.A([]));
    this.get('workspaceManager').restoreWorkInProgress(this.get('tabInfo.id')).promise.then(function(draftCoordinator){
      self.loadCoordinator(draftCoordinator);
    }.bind(this)).catch(function(data){
      self.loadCoordinator();
    }.bind(this));

    this.get('fileBrowser').on('fileBrowserOpened',function(context){
      this.get('fileBrowser').setContext(context);
    }.bind(this));
    this.on('fileSelected',function(fileName){
      this.set(this.get('filePathModel'), fileName);
    }.bind(this));
    this.set('childComponents', new Map());
  }.on('init'),
  conditionalDataInExists :false,
  elementsInserted : function(){
    this.$("input[name=dataInputType][value=" + this.get('coordinator.dataInputType') + "]").prop('checked','checked');
  }.on('didInsertElement'),
  observeXmlAppPath : Ember.observer('xmlAppPath', function(){
    if(!this.get('xmlAppPath') || null === this.get('xmlAppPath')){
      return;
    } else {
      this.showExistingWorkflow();
    }
  }),
  observeFilePath : Ember.observer('coordinatorFilePath', function(){
    if(!this.get('coordinatorFilePath') || null === this.get('coordinatorFilePath')){
      return;
    }else{
      this.sendAction('changeFilePath', this.get('tabInfo'), this.get('coordinatorFilePath'));
    }
  }),
  nameObserver : Ember.observer('coordinator.name', function(){
    if(!this.get('coordinator')){
      return;
    }else if(this.get('coordinator') && Ember.isBlank(this.get('coordinator.name'))){
      if(!this.get('clonedTabInfo')){
        this.set('clonedTabInfo', Ember.copy(this.get('tabInfo')));
      }
      this.sendAction('changeTabName', this.get('tabInfo'), this.get('clonedTabInfo.name'));
    }else{
      this.sendAction('changeTabName', this.get('tabInfo'), this.get('coordinator.name'));
    }
  }),
  loadCoordinator(draftCoordinator){
    if(draftCoordinator){
      this.set('coordinator', JSOG.parse(draftCoordinator));
    }else{
      this.set('coordinator', this.createNewCoordinator());
    }
    this.set('timeUnitOptions',Ember.A([]));
    this.get('timeUnitOptions').pushObject({value:'',displayName:'Select'});
    this.get('timeUnitOptions').pushObject({value:'months',displayName:'Months'});
    this.get('timeUnitOptions').pushObject({value:'endOfMonths',displayName:'End of Months'});
    this.get('timeUnitOptions').pushObject({value:'days',displayName:'Days'});
    this.get('timeUnitOptions').pushObject({value:'endOfDays',displayName:'End of Days'});
    this.get('timeUnitOptions').pushObject({value:'hours',displayName:'Hours'});
    this.get('timeUnitOptions').pushObject({value:'minutes',displayName:'Minutes'});
    this.get('timeUnitOptions').pushObject({value:'cron',displayName:'Cron'});
    this.set('coordinator.slaInfo', SlaInfo.create({}));
    this.set('coordinatorControls',[
      {'name':'timeout', 'displayName':'Timeout', 'value':''},
      {'name':'concurrency', 'displayName':'Concurrency', 'value':''},
      {'name':'execution', 'displayName':'Execution', 'value':''},
      {'name':'throttle', 'displayName':'Throttle', 'value':''}
    ]);
    this.set('timezoneList', Ember.copy(Constants.timezoneList));
    if(Ember.isBlank(this.get('coordinator.name')) && this.get('isDefaultNameForCoordinatorEnabled')){
      this.set('coordinator.name', Ember.copy(this.get('tabInfo.name')));
    }
    this.schedulePersistWorkInProgress();
  },
  coordinatorFilePath : Ember.computed('tabInfo.filePath', function(){
    return this.get('tabInfo.filePath');
  }),
  schedulePersistWorkInProgress (){
    Ember.run.later(function(){
      this.persistWorkInProgress();
      this.schedulePersistWorkInProgress();
    }.bind(this), Constants.persistWorkInProgressInterval);
  },
  persistWorkInProgress(){
    if(!this.get('coordinator')){
      return;
    }
    var json = JSOG.stringify(this.get("coordinator"));
    this.get('workspaceManager').saveWorkInProgress(this.get('tabInfo.id'), json);
  },
  showExistingWorkflow  : function(){
    if(!this.get('xmlAppPath')){
      return;
    }
    var workflowXmlPath = this.get("xmlAppPath"), relXmlPath = "", tempArr;
    if(workflowXmlPath.indexOf("://") === -1 && workflowXmlPath.indexOf(":") === -1){
      relXmlPath = workflowXmlPath;
    } else{
      tempArr = workflowXmlPath.split("//")[1].split("/");
      tempArr.splice(0, 1);
      relXmlPath = "/" + tempArr.join("/");
      if(relXmlPath.indexOf(".xml") !== relXmlPath.length-4) {
        if(relXmlPath.charAt(relXmlPath.length-1) !== "/"){
          relXmlPath = relXmlPath+ "/" +"workflow.xml";
        } else{
          relXmlPath = relXmlPath+"workflow.xml";
        }
      }
    }
    this.importCoordinator(relXmlPath);
  }.on('didInsertElement'),
  createNewCoordinator(){
    return Coordinator.create({
      parameters : {
        configuration :{
          property : Ember.A([])
        }
      },
      controls : Ember.A([]),
      datasets : Ember.A([]),
      dataInputs : Ember.A([]),
      inputLogic : null,
      dataOutputs : Ember.A([]),
      workflow : {
        appPath : undefined,
        configuration :{
          property : Ember.A([])
        }
      },
      frequency : {
        type : undefined,
        value : undefined
      },
      start : {
        value : undefined,
        displayValue : undefined,
        type : 'date'
      },
      end : {
        value : undefined,
        displayValue : undefined,
        type : 'date'
      },
      timezone : 'UTC',
      dataInputType : 'simple',
      slaInfo : SlaInfo.create({}),
      schemaVersions : {
        coordinatorVersion : this.get('schemaVersions').getDefaultVersion('coordinator')
      },
      xmlns : "uri:oozie:coordinator:" +this.get('schemaVersions').getDefaultVersion('coordinator'),
      draftVersion: 'v1'
    });
  },
  importSampleCoordinator (){
    var self = this;
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: "/sampledata/coordinator.xml",
      dataType: "text",
      cache:false,
      success: function(data) {
        var coordinatorXmlImporter = CoordinatorXmlImporter.create({});
        var coordinator = coordinatorXmlImporter.importCoordinator(data);
        deferred.resolve(coordinator);
      }.bind(this),
      failure : function(data){
        deferred.reject(data);
      }
    });
    return deferred;
  },
  importSampleWorkflow (){
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: "/sampledata/workflow.xml",
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
  importCoordinator (filePath){
    if (!filePath) {
      return;
    }
    this.set("isImporting", true);
    filePath = this.appendFileName(filePath, 'coord');
    this.set("coordinatorFilePath", filePath);
    var deferred = this.readCoordinatorFromHdfs(filePath);
    deferred.promise.then(function(response){
      if(response.type === 'xml'){
        this.getCoordinatorFromXml(response.data);
      }else {
        this.getCoordinatorFromJSON(response.data);
      }
      this.set('coordinatorFilePath', filePath);
      this.set("isImporting", false);
    }.bind(this)).catch(function(data){
      console.error(data);
      this.set("errorMsg", "There is some problem while importing.");
      this.set("isImporting", false);
      this.set("isImportingSuccess", false);
      this.set("data", data);
    }.bind(this));
  },
  getCoordinatorFromJSON(draftCoordinator){
    this.set('coordinator', JSOG.parse(draftCoordinator));
    this.$('input[name="dataInputType"][value="'+ this.get('coordinator.dataInputType')+'"]').prop('checked', true);
    if(this.get('coordinator.dataInputType') === 'logical'){
      this.set('conditionalDataInExists', true);
    }
    if(this.get('coordinator.inputLogic')){
      this.set('inputLogicExists', true);
      this.set('inputLogicEnabled', true);
    }
  },
  readCoordinatorFromHdfs(filePath){
    var url =  Ember.ENV.API_URL + "/readWorkflow?workflowPath="+filePath+"&jobType=COORDINATOR";
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
      deferred.reject(e);
    });
    return deferred;
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
    }).fail(function(e){
      deferred.reject(e);
    });
    return deferred;
  },
  appendFileName(filePath, type){
    if(filePath.endsWith('.wfdraft')){
      return filePath;
    }else if(!filePath.endsWith('.xml') && type === 'coord'){
      return filePath = `${filePath}/coordinator.xml`;
    }else if(!filePath.endsWith('.xml') && type === 'wf'){
      return filePath = `${filePath}/workflow.xml`;
    }else{
      return filePath;
    }
  },
  getCoordinatorFromXml(coordinatorXml){
    var coordinatorXmlImporter = CoordinatorXmlImporter.create({});
    var coordinatorObj = coordinatorXmlImporter.importCoordinator(coordinatorXml);
    this.get("errors").clear();
    this.get("errors").pushObjects(coordinatorObj.errors);
    var coordinator = coordinatorObj.coordinator;
    if (coordinator === null) {
      return;
    }
    this.set("coordinator", coordinator);
    this.$('input[name="dataInputType"][value="'+ coordinator.get('dataInputType')+'"]').prop('checked', true);
    if(coordinator.get('dataInputType') === 'logical'){
      this.set('conditionalDataInExists', true);
    }
    if(coordinator.get('inputLogic')){
      this.set('inputLogicExists', true);
      this.set('inputLogicEnabled', true);
    }
  },
  validateChildComponents(showErrorMessage){
    var isChildComponentsValid = true;
    this.get('childComponents').forEach((context)=>{
      if(context.get('validations') && context.get('validations.isInvalid')){
        isChildComponentsValid =  false;
        context.set('showErrorMessage', showErrorMessage);
      }
    }.bind(this));
    return isChildComponentsValid;
  },
  actions : {
    registerChild(key, context){
      this.get('childComponents').set(key, context);
    },
    deregisterChild(key){
      this.get('childComponents').delete(key);
    },
    createDataset(){
      this.set('datasetEditMode', false);
      this.set('datasetCreateMode', true);
      this.set('currentDataset',{});
    },
    editDataset(index){
      this.set('datasetEditMode', true);
      this.set('datasetCreateMode', false);
      this.set('currentDatasetIndex', index);
      this.set('currentDataset', Ember.copy(this.get('coordinator.datasets').objectAt(index)));
    },
    addDataset(){
      this.get('coordinator.datasets').pushObject(Ember.copy(this.get('currentDataset')));
      this.set('datasetCreateMode', false);
    },
    updateDataset(){
      this.get('coordinator.datasets').replace(this.get('currentDatasetIndex'), 1, Ember.copy(this.get('currentDataset')));
      this.set('datasetEditMode', false);
    },
    cancelDatasetOperation(){
      this.set('datasetCreateMode', false);
      this.set('datasetEditMode', false);
    },
    deleteDataset(index){
      this.get('coordinator.datasets').removeAt(index);
      if(index === this.get('currentDatasetIndex')){
        this.set('datasetEditMode', false);
      }
    },
    createDataInput(){
      this.set('dataInputEditMode', false);
      this.set('dataInputCreateMode', true);
      this.set('currentDataInput', {});
    },
    addDataInput(){
      this.get('coordinator.dataInputs').pushObject(Ember.copy(this.get('currentDataInput')));
      this.set('dataInputCreateMode', false);
    },
    editDataInput(index){
      this.set('dataInputCreateMode', false);
      this.set('dataInputEditMode', true);
      this.set('currentDataInputIndex', index);
      this.set('currentDataInput', Ember.copy(this.get('coordinator.dataInputs').objectAt(index)));
    },
    updateDataInput(){
      this.get('coordinator.dataInputs').replace(this.get('currentDataInputIndex'), 1, Ember.copy(this.get('currentDataInput')));
      this.set('dataInputEditMode', false);
    },
    deleteDataInput(index){
      this.get('coordinator.dataInputs').removeAt(index);
      if(index === this.get('currentDataInputIndex')){
        this.set('dataInputEditMode', false);
      }
    },
    cancelDataInputOperation(){
      this.set('dataInputEditMode', false);
      this.set('dataInputCreateMode', false);
    },
    createDataOutput(){
      this.set('dataOutputEditMode', false);
      this.set('dataOutputCreateMode', true);
      this.set('currentDataOutput', {});
    },
    addDataOutput(){
      this.get('coordinator.dataOutputs').pushObject(Ember.copy(this.get('currentDataOutput')));
      this.set('dataOutputCreateMode', false);
    },
    editDataOutput(index){
      this.set('dataOutputCreateMode', false);
      this.set('dataOutputEditMode', true);
      this.set('currentDataOutputIndex', index);
      this.set('currentDataOutput', Ember.copy(this.get('coordinator.dataOutputs').objectAt(index)));
    },
    updateDataOutput(){
      this.get('coordinator.dataOutputs').replace(this.get('currentDataOutputIndex'), 1, Ember.copy(this.get('currentDataOutput')));
      this.set('dataOutputEditMode', false);
    },
    deleteDataOutput(index){
      this.get('coordinator.dataOutputs').removeAt(index);
      if(index === this.get('currentDataOutputIndex')){
        this.set('dataOutputEditMode', false);
      }
    },
    cancelDataOutputOperation(){
      this.set('dataOutputEditMode', false);
      this.set('dataOutputCreateMode', false);
    },
    dryrunCoordinator(){
      this.set('dryrun', true);
      this.send('submit');
    },
    submitCoordinator(){
      this.set('dryrun', false);
      this.send('submit');
    },
    submit(){
      var isChildComponentsValid = this.validateChildComponents(true);
      if(this.get('validations.isInvalid') || !isChildComponentsValid) {
        this.set('showErrorMessage', true);
        return;
      }
      var coordGenerator = CoordinatorGenerator.create({coordinator:this.get("coordinator")});
      var coordinatorXml = coordGenerator.process();
      var configForSubmit={props:Ember.A([]), xml:coordinatorXml,params:this.get('coordinator.parameters'), errors:Ember.A([])};
      this.set("coordinatorConfigs", configForSubmit);
      this.set("showingJobConfig", true);
      var dynamicProperties = this.get('propertyExtractor').getDynamicProperties(coordinatorXml);
      this.get('coordinatorConfigs.props').pushObjects(Array.from(dynamicProperties.values(), key => key));
      var path = this.get('coordinator.workflow.appPath');
      if(this.get('propertyExtractor').containsParameters(path)){
        this.set('parameterizedWorkflowPath', path);
        this.set('containsParameteriedPaths', true);
        return;
      }
      this.send('extractProperties', path);
    },
    extractProperties(workflowPath){
      this.set("coordinatorConfigs.errors", Ember.A([]));
      workflowPath = this.appendFileName(workflowPath, 'wf');
      var deferred = this.readFromHdfs(workflowPath);
      deferred.promise.then(function(data){
        var x2js = new X2JS();
        var workflowJson = x2js.xml_str2json(data);
        this.set('workflowName', workflowJson["workflow-app"]._name);
        var workflowProps = this.get('propertyExtractor').getDynamicProperties(data);
        var dynamicProperties = this.get('coordinatorConfigs.props');
        workflowProps.forEach((prop)=>{
          var name = prop.trim().substring(2, prop.length-1);
          if(dynamicProperties.indexOf(prop)>=0 || this.get('coordinator.workflow.configuration.property') && this.get('coordinator.workflow.configuration.property').findBy('name', name)){
              return;
          }
          dynamicProperties.pushObject(prop);
        }, this);
        this.set('containsParameteriedPaths', false);
      }.bind(this)).catch(function(e){
        this.get("coordinatorConfigs.errors").pushObject({'message' : 'Could not process workflow from ' + this.get('coordinator.workflow.appPath')});
        throw new Error(e);
      }.bind(this));
    },
    closeCoordSubmitConfig(){
      this.set("showingJobConfig", false);
    },
    closeFileBrowser(){
      this.set("showingFileBrowser", false);
      this.get('fileBrowser').getContext().trigger('fileSelected', this.get('filePath'));
      if(this.get('filePathModel') === 'coordinatorFilePath'){
        this.importCoordinator(Ember.copy(this.get('coordinatorFilePath')));
        this.set('coordinatorFilePath', null);
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
    createCondition(){
      this.set('coordinator.conditionalDataInput', {type:'condition', operator:'and'});
      this.set('conditionalDataInExists', true);
    },
    deleteCondition(index){
      this.set('coordinator.conditionalDataInput', undefined);
      this.set('conditionalDataInExists', false);
    },
    toggleDataTnput(type){
      this.set('coordinator.dataInputType', type);
    },
    createInputLogic(){
      this.set('coordinator.inputLogic', {type:'condition', operator:'and'});
      this.set('inputLogicExists', true);
    },
    deleteInputLogic(index){
      this.set('coordinator.inputLogic', undefined);
      this.set('inputLogicExists', false);
    },
    preview(){
      var isChildComponentsValid = this.validateChildComponents(true);
      if(this.get('validations.isInvalid') || !isChildComponentsValid) {
        this.set('showErrorMessage', true);
        return;
      }
      this.set("showingPreview", false);
      var coordGenerator = CoordinatorGenerator.create({coordinator:this.get("coordinator")});
      var coordinatorXml = coordGenerator.process();
      this.set("previewXml", vkbeautify.xml(coordinatorXml));
      this.set("showingPreview", true);
    },
    closePreview(){
      this.set("showingPreview", false);
    },
    confirmReset(){
      this.set('showingResetConfirmation', true);
    },
    resetCoordinator(){
      this.get("errors").clear();
      this.set('showingResetConfirmation', false);
      if(this.get('coordinatorFilePath')){
        this.importCoordinator(this.get('coordinatorFilePath'));
      }else{
        this.set('coordinator', this.createNewCoordinator());
      }
    },
    importCoordinatorTest(){
      var deferred = this.importSampleCoordinator();
      deferred.promise.then(function(data){
        this.get("errors").clear();
        this.get("errors").pushObjects(data.errors);
        if (data.coordinator === null) {
          return;
        }
        this.set("coordinator", data.coordinator);
        this.$('input[name="dataInputType"][value="'+ data.coordinator.get('dataInputType')+'"]').prop('checked', true);
        if(data.coordinator.get('dataInputType') === 'logical'){
          this.set('conditionalDataInExists', true);
        }
      }.bind(this)).catch(function(data){
        console.error(data);
        this.set("errorMsg", "There is some problem while importing.");
        this.set("data", data);
      });
    },
    openTab(type, path){
      this.set('errorMsg', '');
      var path = this.appendFileName(path, type);
      var deferred = this.readFromHdfs(path);
      deferred.promise.then(function(data){
        this.sendAction('openTab', type, path);
      }.bind(this)).catch(function(data){
        this.set('errorMsg', 'There is some problem while importing.');
        this.set('data', data);
      }.bind(this));
    },
    showParameterSettings(value){
      if(this.get('coordinator.parameters') !== null){
        this.set('parameters', Ember.copy(this.get('coordinator.parameters')));
      }else{
        this.set('parameters', {});
      }
      this.set('showParameterSettings', value);
    },
    closeWorkFlowParam(){
      this.set("showParameterSettings", false);
    },
    saveWorkFlowParam(){
      this.set('coordinator.parameters', Ember.copy(this.get('parameters')));
      this.set("showParameterSettings", false);
    },
    showControlConfig(){
      if(this.get('coordinator.controls')){
        this.get('coordinatorControls').forEach((control)=>{
          var coordControl = this.get('coordinator.controls').findBy('name', control.name);
          if(coordControl){
            Ember.set(control, 'value', coordControl.value);
          }else{
            Ember.set(control, 'value', '');
          }
        }, this);
      }
      this.set('showControlConfig', true);
    },
    saveCoordControls(){
      this.get('coordinatorControls').forEach((control)=>{
        var coordControl = this.get('coordinator.controls').findBy('name', control.name);
        if(coordControl){
          Ember.set(coordControl, 'value', control.value);
        }else{
          this.get('coordinator.controls').pushObject({'name':control.name, 'value':control.value});
        }
      }, this);
      this.set('showControlConfig', false);
    },
    showWorkflowName(){
      this.set('workflowName', null);
      this.set('errorMsg', "");
      var path = this.appendFileName(this.get('coordinator.workflow.appPath'), 'wf');
      if (this.get('propertyExtractor').containsParameters(path)) {
        this.set('containsParameteriedPaths', true);
        this.set('parameterizedPathWarning', 'Workflow path contains variables');
        return;
      } else {
        this.set('containsParameteriedPaths', false);
        this.set('parameterizedPathWarning', '');
      }
      var deferred = this.readFromHdfs(path);
      deferred.promise.then(function(data){
        var x2js = new X2JS();
        var workflowJson = x2js.xml_str2json(data);
        this.set('workflowName', workflowJson["workflow-app"]._name);
      }.bind(this)).catch(function(data){
        console.error(data);
        this.set('workflowName', null);
        this.set('errorMsg', "There is some problem while fetching workflow name.");
        this.set("data", data);
      }.bind(this));
    },
    showVersionSettings(value){
      this.set('showVersionSettings', value);
    },
    save(){
      if (Ember.isBlank(this.$('[name=coord_title]').val())) {
        return;
      }
      var isDraft = false, coordinatorXml;
      var isChildComponentsValid = this.validateChildComponents(false);
      if(this.get('validations.isInvalid') || !isChildComponentsValid) {
        isDraft = true;
      }else{
        var coordGenerator = CoordinatorGenerator.create({coordinator:this.get("coordinator")});
        coordinatorXml = coordGenerator.process();
      }
      var coordinatorJson = JSOG.stringify(this.get("coordinator"));
      this.set("configForSave",{json:coordinatorJson, xml:coordinatorXml,isDraft: isDraft});
      this.set("showingSaveWorkflow", true);
    },
    closeSave(){
      this.set("showingSaveWorkflow", false);
    },
    toggleIO(){
      this.$('#collapse').collapse('toggle');
    }
  }
});
