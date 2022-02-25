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
import {Workflow} from '../domain/workflow';
import Constants from '../utils/constants';
import {WorkflowGenerator} from '../domain/workflow-xml-generator';
import {WorkflowImporter} from '../domain/workflow-importer';
import {WorkflowJsonImporter} from '../domain/workflow-json-importer';
import {WorkflowContext} from '../domain/workflow-context';
import {CytoscapeRenderer} from '../domain/cytoscape-flow-renderer';
import {FindNodeMixin} from '../domain/findnode-mixin';
import { validator, buildValidations } from 'ember-cp-validations';
import WorkflowPathUtil from '../domain/workflow-path-util';
import {ActionTypeResolver} from '../domain/action-type-resolver';
import CommonUtils from "../utils/common-utils";

const Validations = buildValidations({
  'dataNodes': { /* For Cytoscape */
    validators: [
      validator('duplicate-data-node-name', {
        dependentKeys: ['dataNodes.@each.dataNodeName']
      }),
      validator('workflow-dag', {
        dependentKeys: ['dataNodes.@each.source', 'dataNodes.@each.target']
      })
    ]
  },
  'workflow.killNodes': {
    validators: [
      validator('duplicate-kill-node-name', {
        dependentKeys: ['workflow.killNodes.@each.name']
      })
    ]
  }
});

export default Ember.Component.extend(FindNodeMixin, Validations, {
  workflowContext : WorkflowContext.create({}),
  workflowTitle:"",
  previewXml:"",
  supportedActionTypes:["java", "hive", "pig", "sqoop", "shell", "spark", "map-reduce", "hive2", "sub-workflow", "distcp", "ssh", "FS"],
  workflow:null,
  flowRenderer:null,
  hoveredWidget:null,
  showingConfirmationNewWorkflow:false,
  showingWorkflowConfigProps:false,
  workflowSubmitConfigs:{},
  showingPreview:false,
  currentTransition:null,
  currentNode:null,
  domain:{},
  showActionEditor : false,
  flattenedNodes: [],
  dataNodes: [], /* For cytoscape */
  counterMap : {},
  hoveredAction: null,
  workflowImporter:WorkflowImporter.create({}),
  actionTypeResolver: ActionTypeResolver.create({}),
  propertyExtractor : Ember.inject.service('property-extractor'),
  clipboardService : Ember.inject.service('workflow-clipboard'),
  workspaceManager : Ember.inject.service('workspace-manager'),
  assetManager : Ember.inject.service('asset-manager'),
  showGlobalConfig : false,
  showParameterSettings : false,
  showNotificationPanel : false,
  globalConfig : {},
  assetConfig : {},
  parameters : {},
  validationErrors : Ember.computed('validations.attrs.dataNodes.message', 'validations.attrs.workflow.killNodes.message', {
    get(key){
      var errors = [];
      if(this.get('validations.attrs.dataNodes.message')){
        errors.pushObject({message : this.get('validations.attrs.dataNodes.message')});
      }
      if(this.get('validations.attrs.workflow.killNodes.message')){
        errors.pushObject(this.get('validations.attrs.dataNodes.message'));
      }
      return errors;
    },
    set(key, value){
      if(!value){
        this.set(key, Ember.A([]));
      }
    }
  }),
  showingFileBrowser : false,
  killNode : {},
  isWorkflowImporting: false,
  isAssetPublishing: false,
  errorMsg: "",
  data : {
    "responseText": ""
  },
  shouldPersist : false,
  useCytoscape: Constants.useCytoscape,
  cyOverflow: {},
  clipboard : Ember.computed.alias('clipboardService.clipboard'),
  showingStreamImport:false,
  fileInfo:Ember.Object.create(),
  isDraft: false,
  jobConfigProperties: Ember.A([]),
  saveJobService : Ember.inject.service('save-job'),
  isDefaultNameForWFEnabled : false,
  initialize : function(){
    var id = 'cy-' + Math.ceil(Math.random() * 1000);
    this.set('cyId', id);
    this.sendAction('register', this.get('tabInfo'), this);
    this.set('flowRenderer',CytoscapeRenderer.create());
    this.set('workflow',Workflow.create({}));
    CommonUtils.setTestContext(this);
    this.set('dataNodes', Ember.A([]));
    this.set('errors', Ember.A([]));
    this.set('validationErrors', Ember.A([]));
  }.on('init'),
  elementsInserted :function(){
    this.setConentWidth();
    if(this.get("xmlAppPath")){
      this.showExistingWorkflow();
      return;
    } else {
      this.workflow.initialize();
      this.initAndRenderWorkflow();
      this.$('#wf_title').focus();
      if (Constants.autoRestoreWorkflowEnabled){
        this.restoreWorkflow();
      }
    }
    /*
       This block will enable/disable giving default name for workflow
    */
    if(Ember.isBlank(this.get('workflow.name')) && this.get('isDefaultNameForWFEnabled')) {
      this.set('workflow.name', Ember.copy(this.get('tabInfo.name')));
    }
  }.on('didInsertElement'),
  restoreWorkflow(){
    if (!this.get("isNew")){
      this.getDraftWorkflow().promise.then(function(draftWorkflow){
        if (draftWorkflow){
          this.resetDesigner();
          this.set("workflow",draftWorkflow);
          this.rerender();
          this.doValidation();
          this.generateCounterMap();
        }
      }.bind(this)).catch(function(data){
      });
    }
  },

  importWorkflowFromProjManager(path){
    this.importWorkflow(path);
  },

  observeXmlAppPath : Ember.observer('xmlAppPath', function(){
    if(!this.get('xmlAppPath') || null === this.get('xmlAppPath')){
      return;
    }else{
      this.showExistingWorkflow();
    }
  }),
  observeFilePath : Ember.observer('workflowFilePath', function(){
    if(!this.get('workflowFilePath') || null === this.get('workflowFilePath')){
      return;
    }else{
      this.sendAction('changeFilePath', this.get('tabInfo'), this.get('workflowFilePath'));
    }
  }),
  nameObserver : Ember.observer('workflow.name', function(){
    if(!this.get('workflow')){
      return;
    }else if(this.get('workflow') && Ember.isBlank(this.get('workflow.name'))){
      if(!this.get('clonedTabInfo')){
        this.set('clonedTabInfo', Ember.copy(this.get('tabInfo')));
      }
      this.sendAction('changeTabName', this.get('tabInfo'), this.get('clonedTabInfo.name'));
    }else{
      this.sendAction('changeTabName', this.get('tabInfo'), this.get('workflow.name'));
    }
  }),
  workflowFilePath : Ember.computed('tabInfo.filePath', function(){
    return this.get('tabInfo.filePath');
  }),
  showParentWorkflow(type, path){
    this.sendAction('openTab', type, path);
  },
  showExistingWorkflow(){
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
    this.importWorkflowFromProjManager(relXmlPath);
  },
  setConentWidth(){
    var offset = 120;
    if (Ember.ENV.instanceInfo) {
      offset = 0;
    }
    Ember.$(window).resize(function() {
      return;
    });
  },
  workflowXmlDownload(workflowXml){
    let link = document.createElement("a"), val = this.get('workflow.name');
    if(Ember.isBlank(val)) {
      link.download = "workflow.xml";
    } else {
      link.download = val.replace(/\s/g, '_');
    }
    link.href = "data:text/xml,"+encodeURIComponent(vkbeautify.xml(workflowXml));
    link.click();
  },
  nodeRendered: function(){
    if (this.get("isNew")){
      this.set("isNew",false);
      return;
    }
    this.doValidation();
    this.resize();
    this.persistWorkInProgress();
  }.on('didUpdate'),
  renderTransitions : function(){
    this.flowRenderer.onDidUpdate(this,this.get("workflow").startNode,this.get("workflow"));
    this.layout();
  },
  resize(){
    this.flowRenderer.resize();
  },
  centerGraph(){
    this.flowRenderer.setGraphCenter();
  },
  cleanupFlowRenderer:function(){
    this.set('renderNodeTransitions',false);
    this.flowRenderer.cleanup();
  }.on('willDestroyElement'),
  initAndRenderWorkflow(){
    var panelOffset=this.$(".designer-panel").offset();
    var canvasHeight=Ember.$(window).height()-panelOffset.top-25;
    this.flowRenderer.initRenderer(function(){
      this.renderWorkflow();
    }.bind(this),{context:this,id : this.get('cyId'),flattenedNodes:this.get("flattenedNodes"),dataNodes:this.get("dataNodes"), cyOverflow:this.get("cyOverflow"),canvasHeight:canvasHeight});
    this.generateCounterMap();
  },
  generateCounterMap() {
    let len = 0, id = 0, val = null, self = this;
    this.get('dataNodes').forEach(function(item){
      if(item.data.node) {
        if(item.data.node.type === "action") {
          let keyMap = self.get("counterMap"), type = item.data.node.actionType;
          if(keyMap.hasOwnProperty(type)){
            keyMap[type] = parseInt(keyMap[type])+1;
          } else {
            keyMap[type] = 1;
          }
        }
      }
    });
  },
  renderWorkflow(){
    this.set('renderNodeTransitions', true);
    this.flowRenderer.renderWorkflow(this.get("workflow"));
    this.doValidation();
    this.renderTransitions();
  },
  rerender(callback){
    this.set('showStatus', true);
    Ember.run.later(() => {
      this.flowRenderer.cleanup();
      this.renderWorkflow(this.get("workflow"));
      this.set('showStatus', false);
      if(callback){
        callback();
      }
    }.bind(this));
  },
  setCurrentTransition(transitionInfo){
    this.set("currentTransition", {
      transition : transitionInfo.transition,
      source : transitionInfo.source,
      target : transitionInfo.target,
      originalSource : transitionInfo.originalSource
    });
  },
  actionInfo(node){
    this.send("showNotification", node);
  },
  deleteTransition(transition,sourceNode){
    this.createSnapshot();
    this.get("workflow").deleteTransition(transition);
    this.rerender(function(){
      this.showUndo('transition');
    }.bind(this));
  },
  showWorkflowActionSelect(element){
    var self=this;
    this.$('.popover').popover('destroy');
    Ember.$(element).parents(".jsplumb-overlay").css("z-index", "4");
    this.$(element).attr('data-toggle','popover');
    this.$(element).popover({
      html : true,
      title : "Add Node <button type='button' class='close'>&times;</button>",
      placement: 'right',
      trigger : 'focus',
      content : function(){
        return self.$('#workflow-actions').html();
      }
    });
    this.$(element).popover("show");
    this.$('.popover .close').on('click',function(){
      Ember.$(".jsplumb-overlay").css("z-index", "");
      this.$('.popover').popover('destroy');
    }.bind(this));
  },

  layout(){
    this.flowRenderer.refresh();
  },
  doValidation(){
    this.validate();
  },
  importWorkflow(filePath){
    var self = this;
    this.set("isWorkflowImporting", true);

    var workflowXmlDefered=this.getWorkflowFromHdfs(filePath);
    workflowXmlDefered.promise.then(function(response){
      if(response.type === 'xml'){
        this.importWorkflowFromString(response.data);
      }else {
        this.importWorkflowFromJSON(response.data);
      }
      this.set("isWorkflowImporting", false);
      this.set("workflowFilePath", filePath);
    }.bind(this)).catch(function(data){
      console.error(data);
      self.set("errorMsg", "There is some problem while importing.");
      self.set("data", data);
      self.set("isWorkflowImporting", false);
    });
  },
  importWorkflowFromString(data){
    this.showSparkMasterFieldError(data);

    var wfObject=this.get("workflowImporter").importWorkflow(data);
    this.set("errors", wfObject.errors);
    if (wfObject.workflow === null) {
      this.rerender();
      return;
    }
    this.resetDesigner();
    if(this.get('workflow')){
      this.resetDesigner();
      this.set("workflow",wfObject.workflow);
      this.initAndRenderWorkflow();
      this.rerender();
      this.doValidation();
    }else{
      this.workflow.initialize();
      this.set("workflow",wfObject.workflow);
      this.initAndRenderWorkflow();
      this.$('#wf_title').focus();
    }
  },
  importWorkflowFromJSON(data){
    this.showSparkMasterFieldError(data);
    var workflowImporter=WorkflowJsonImporter.create({});
    var workflow=workflowImporter.importWorkflow(data);
    this.resetDesigner();
    this.set("workflow", workflow);
    this.initAndRenderWorkflow();
    this.rerender();
    this.doValidation();
  },
  migrateActionObjectToCollection(data) {
    if(Ember.isArray(data)) {
      return data;
    } else {
      let ArrayData = [];
      ArrayData.push(data);
      return ArrayData;
    }
  },
  showSparkMasterFieldError(data) {
    let x2js = new X2JS();
    let actionSettingsObj = x2js.xml_str2json(data);
    let sparkActionList, sparkActionArray = [];
    if(actionSettingsObj["workflow-app"] && actionSettingsObj["workflow-app"].action) {
      sparkActionList = actionSettingsObj["workflow-app"].action;
      sparkActionArray = this.migrateActionObjectToCollection(sparkActionList);
      if(sparkActionArray.findBy('spark') && this.migrateActionObjectToCollection(sparkActionArray.findBy('spark')).find(function(item){return item.spark.master === "yarn-client"})) {
        this.set('isSparkUnSupportedPropsAvailable', true);
      }
    }
  },
  getWorkflowFromHdfs(filePath){
    var url = Ember.ENV.API_URL + "/readWorkflow?workflowPath="+filePath+'&jobType=WORKFLOW';
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
    }).fail(function(data){
      deferred.reject(data);
    });
    return deferred;
  },
  getAssetFromHdfs(filePath){
    var url = Ember.ENV.API_URL + "/readAsset?assetPath="+filePath;
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

  importActionSettingsFromString(actionSettings) {
    var x2js = new X2JS();
    var actionSettingsObj = x2js.xml_str2json(actionSettings);
    var actionSettingsObjType = Object.keys(actionSettingsObj)[0];
    var currentActionNode = this.flowRenderer.currentCyNode.data().node;
    if (actionSettingsObjType === currentActionNode.actionType) {
      var actionJobHandler = this.actionTypeResolver.getActionJobHandler(currentActionNode.actionType);
      actionJobHandler.handleImport(currentActionNode, actionSettingsObj[currentActionNode.actionType]);
      this.flowRenderer.hideOverlayNodeActions();
    } else {
      this.set("errorMsg", actionSettingsObjType + " action settings can't be imported to " + currentActionNode.actionType + " action");
    }
  },
  importActionNodeFromString(actionNodeXmlString) {
    var x2js = new X2JS();
    var actionNodeXml = x2js.xml_str2json(actionNodeXmlString);
    var actionNodeType = Object.keys(actionNodeXml)[0];
    this.createSnapshot();
    this.generateUniqueNodeId(actionNodeType);
    var actionNode = this.get("workflow").addNode(this.getTransitionInfo(),actionNodeType, {}, "");
    this.rerender();
    this.doValidation();
    this.scrollToNewPosition();
    var actionJobHandler = this.actionTypeResolver.getActionJobHandler(actionNodeType);
    actionJobHandler.handleImport(actionNode, actionNodeXml[actionNodeType]);
  },
  getRandomDataToDynamicProps(dynamicProperties) {
    var wfDynamicProps = [];
    var wfParams = this.get('workflow.parameters');
    dynamicProperties.forEach(function(property) {
      if (property!=="${nameNode}" && property!==Constants.rmDefaultValue) {
        var propName = property.trim().substring(2, property.length-1);
        var propValue;
        if (wfParams && wfParams.configuration && wfParams.configuration.property) {
          var param = wfParams.configuration.property.findBy('name', propName);
          if (!(param && param.value)) {
            propValue = param.value;
          }
        }
        var prop = Ember.Object.create({
          name: propName,
          value: propValue ? propValue : Math.random().toString(36).slice(2)
        });
        wfDynamicProps.push(prop);
      }
    });
    return wfDynamicProps;
  },
  exportActionNodeXml() {
    var self = this;
    self.set("isAssetPublishing", true);
    self.set("errorMsg", "");
    var workflowGenerator = WorkflowGenerator.create({workflow:this.get("workflow"), workflowContext:this.get('workflowContext')});
    var actionNodeXml = workflowGenerator.getActionNodeXml(this.flowRenderer.currentCyNode.data().name, this.flowRenderer.currentCyNode.data().node.actionType);
    var dynamicProperties = this.get('propertyExtractor').getDynamicProperties(actionNodeXml);

    var exportActionNodeXmlDefered=this.get("assetManager").publishAsset(this.get('exportActionNodeFilePath'), actionNodeXml, this.getRandomDataToDynamicProps(dynamicProperties));
    exportActionNodeXmlDefered.promise.then(function(data){
      self.set("isAssetPublishing", false);
    }.bind(this)).catch(function(data){
      self.set("errorMsg", "There is some problem while publishing asset.");
      self.set("data", data);
      self.set("isAssetPublishing", false);
    });
  },
  resetDesigner(){
    this.set("xmlAppPath", null);
    this.set('errors',[]);
    this.set('errorMsg',"");
    this.set('validationErrors',[]);
    this.get("workflow").resetWorfklow();
    this.set('globalConfig', {});
    this.set('parameters', {});
    this.set('counterMap', {});
    this.set("undoAvailable", false);
    this.set("showingConfirmationNewWorkflow", false);
    if(this.get('workflow.parameters') !== null){
      this.set('workflow.parameters', {});
    }
    this.set('parameters', {});
    this.flowRenderer.reset();
  },
  resetZoomLevel(){
    this.set("zoomLevel", 1);
  },
  incZoomLevel(){
    this.set("zoomLevel", this.get("zoomLevel")+0.1);
  },
  decZoomLevel(){
    this.set("zoomLevel", this.get("zoomLevel")-0.1);
  },
  importSampleWorkflow (){
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: "/sampledata/workflow.xml",
      dataType: "text",
      cache:false,
      success: function(data) {
        var wfObject=this.get("workflowImporter").importWorkflow(data);
        deferred.resolve(wfObject);
      }.bind(this),
      failure : function(data){
        deferred.reject(data);
      }
    });
    return deferred;
  },
  saveAsDraft(){
    var self = this, url = Ember.ENV.API_URL + "/saveWorkflowDraft?app.path=" + this.get("workflowFilePath") + "&overwrite=" + this.get("overwritePath");
    Ember.$.ajax({
      url: url,
      method: "POST",
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function(request) {
        request.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        request.setRequestHeader("X-Requested-By", "workflow-designer");
      },
      data: self.getWorkflowAsJson(),
      success: function(response) {
        //deferred.resolve(response);
      }.bind(this),
      error: function(response) {
        //deferred.reject(response);
      }.bind(this)
    });
  },
  persistWorkInProgress(){
   var json=this.getWorkflowAsJson();
   this.get('workspaceManager').saveWorkInProgress(this.get('tabInfo.id'), json);
  },
  getWorkflowAsJson(){
    return this.getWorkflowAsJsonJsoGImpl();
  },
  getWorkflowAsNativeJsonImpl(){
    try{
     var json=JSON.stringify(this.get("workflow")), self = this;
     var actionVersions = JSON.stringify(CommonUtils.toArray(this.get("workflow").schemaVersions.actionVersions));
     var workflow = JSON.parse(json);
     workflow.schemaVersions.actionVersions = actionVersions
     return JSON.stringify(workflow);
   }catch(err){
    console.error(err);
     this.isCyclic(this.get("workflow"));
   }
  },
  getWorkflowAsJsonJsoGImpl(){
   try{
    var json=JSOG.stringify(this.get("workflow")), self = this;
    var actionVersions = JSOG.stringify(CommonUtils.toArray(this.get("workflow").schemaVersions.actionVersions));
    var workflow = JSOG.parse(json);
    workflow.schemaVersions.actionVersions = actionVersions
    return JSOG.stringify(workflow);
  }catch(err){
   console.error(err);
    this.isCyclic(this.get("workflow"));
  }
},
  isCyclic (obj) {
    var seenObjects = [];
    function detect (obj) {
      if (typeof obj === 'object') {
        if (seenObjects.indexOf(obj) !== -1) {
          return true;
        }
        seenObjects.push(obj);
        for (var key in obj) {
          if (obj.hasOwnProperty(key) && detect(obj[key])) {
            return true;
          }
        }
      }
      return false;
    }
    return detect(obj);
  },
  getDraftWorkflowData(path){
    var deferred = Ember.RSVP.defer();
    //var path = encodeURIComponent("/user/ambari-qa/examples/demo/draft");
    var self = this, url = Ember.ENV.API_URL + "/readWorkflowDraft?workflowXmlPath=" + path;
    Ember.$.ajax({
      url: url,
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function(request) {
        request.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        request.setRequestHeader("X-Requested-By", "workflow-designer");
      },
      success: function(response) {
        deferred.resolve(response);
      }.bind(this),
      error: function(response) {
        deferred.reject(response);
      }.bind(this)
    });
    return deferred;
  },
  getDraftWorkflow(){
    var deferred = Ember.RSVP.defer();

    this.get('workspaceManager').restoreWorkInProgress(this.get('tabInfo.id')).promise.then(function(drafWorkflowJson){
          var workflowImporter=WorkflowJsonImporter.create({});
          var workflow=workflowImporter.importWorkflow(drafWorkflowJson);
          deferred.resolve(workflow);
        }.bind(this)).catch(function(data){
          deferred.resolve("");
        });
    return deferred;
  },
  createSnapshot() {
    this.set('undoAvailable', false);
    this.set('workflowSnapshot', this.getWorkflowAsJson());
  },
  showUndo (type){
    this.set('undoAvailable', true);
    this.set('undoType', type);
  },
  deleteWorkflowNode(node,transitionslist){
    this.createSnapshot();
    if(node.isKillNode()){
      var result=this.get("workflow").deleteKillNode(node);
      if (result && result.status===false){
        this.get('validationErrors').pushObject({node : node ,message :result.message});
      }
    } else {
      this.get("workflow").deleteNode(node,transitionslist);
    }
    this.rerender(function(){
      this.doValidation();
      this.showUndo('nodeDeleted');
    }.bind(this));
  },
  addWorkflowBranch(node){
    this.createSnapshot();
    this.get("workflow").addBranch(node);
    this.rerender();
  },
  openWorkflowEditor(node){
    this.createSnapshot();
    this.set('showActionEditor', true);
    this.set('currentAction', node.actionType);
    var domain = node.getNodeDetail();
    this.set('flowGraph', this.flowRenderer.getGraph());
    node.set("domain", domain);
    this.set('currentNode', node);
  },
  openDecisionEditor(node) {
    this.get("addBranchListener").trigger("showBranchOptions", node);
  },

  copyNode(node){
    this.get('clipboardService').setContent(node, 'copy');
  },
  cutNode(node, transitionsList){
    this.get('clipboardService').setContent(node, 'cut');
    this.deleteWorkflowNode(node, transitionsList);
  },
  replaceNode(node){
    var clipboardContent = this.get('clipboardService').getContent();
    Ember.set(node, 'name', clipboardContent.name+'-copy');
    Ember.set(node, 'domain', clipboardContent.domain);
    Ember.set(node, 'actionType', clipboardContent.actionType);
    this.rerender();
    this.doValidation();
  },
  scrollToNewPosition(){
    if (Constants.useCytoscape){
      return;
    }
    var scroll = Ember.$(window).scrollTop();
    Ember.$('html, body')
    .animate({
      scrollTop: scroll+200
    }, 1000);
  },
  openSaveWorkflow() {
    this.get('workflowContext').clearErrors();
    if(Ember.isBlank(this.$('[name=wf_title]').val())) {
      this.set('errors',[{"message":"Workflow name is mandatory"}]);
      return;
    }
    var workflowGenerator = WorkflowGenerator.create({workflow:this.get("workflow"), workflowContext:this.get('workflowContext')});
    var workflowXml = workflowGenerator.process();
    var workflowJson = this.getWorkflowAsJson();
    this.set('errors', []);
    var isDraft = this.get('workflowContext').hasErrors()? true: false;
    this.set("configForSave", {json : workflowJson, xml : workflowXml,isDraft : isDraft});
    this.set("showingSaveWorkflow",true);
  },
  openJobConfig () {
    this.get('workflowContext').clearErrors();
    var workflowGenerator = WorkflowGenerator.create({workflow:this.get("workflow"), workflowContext:this.get('workflowContext')});
    var workflowXml = workflowGenerator.process();
    if(this.get('workflowContext').hasErrors() || (this.get("validationErrors") && this.get("validationErrors").length)){
      this.set('errors',this.get('workflowContext').getErrors());
    }else{
      var dynamicProperties = this.get('propertyExtractor').getDynamicProperties(workflowXml);
      var configForSubmit={props:Array.from(dynamicProperties.values(), key => key),xml:workflowXml,params:this.get('workflow.parameters')};
      this.set("workflowSubmitConfigs",configForSubmit);
      this.set("showingWorkflowConfigProps",true);
    }
  },
  isDraftExists(path){
    var deferred = Ember.RSVP.defer(), url, self = this;
    if(!path){
      path = this.get("workflowFilePath");
    }
    url = Ember.ENV.API_URL + "/readWorkflowDetail?workflowXmlPath=" + path;
    Ember.$.ajax({
      url: url,
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function(request) {
        request.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        request.setRequestHeader("X-Requested-By", "workflow-designer");
      },
      success: function(response) {
        deferred.resolve(response);
      }.bind(this),
      error: function(response) {
        deferred.reject(response);
      }.bind(this)
    });
    return deferred;

  },
  importWorkflowFromFile(dataStr){
    this.resetDesigner();
    this.importWorkflowFromString(dataStr);
    this.send("hideStreamImport");
  },
  generateUniqueNodeId(type){
    let keyMap = this.get("counterMap");
    if(keyMap.hasOwnProperty(type)){
      keyMap[type] = ++keyMap[type];
      return keyMap[type];
    } else {
      keyMap[type] = 1;
      return 1;
    }
  },
  getTransitionInfo(){
    var currentTransition=this.get("currentTransition.transition");
    var transition = {};
    if(this.get("currentTransition").source.type === 'placeholder'){
      transition = this.get("currentTransition").originalSource.transitions.findBy('targetNode.id',this.get("currentTransition").source.id);
      transition.source=this.get("currentTransition").originalSource;
    }else{
      transition = this.get("currentTransition").source.transitions.findBy('targetNode.id',currentTransition.targetNode.id);
      transition.source=this.get("currentTransition").source;
    }
    return transition;
  },
  actions:{
    importWorkflowStream(dataStr){
      this.importWorkflowFromFile(dataStr);
    },
    saveFileinfo(path, overWritePath){
      this.get("fileInfo").set("path", path);
      this.get("fileInfo").set("overWritePath", overWritePath);
    },
    showStreamImport() {
      this.set("showingStreamImport", true);
    },
    hideStreamImport() {
      this.set("showingStreamImport", false);
    },
    fileLoaded(file){
      var self = this;
      function importWorkflowFromFile(dataStr){
          self.importWorkflowFromFile(dataStr);
      }
      var reader = new FileReader();
      reader.addEventListener("load", function (event) {
          importWorkflowFromFile(event.target.result);
      });
      reader.readAsText(file);
    },
    importActionSettings(file){
      var self = this;
      var reader = new FileReader();
      reader.addEventListener("load", function (event) {
        var actionSettings = event.target.result;
        var x2js = new X2JS();
        var actionNode = self.flowRenderer.currentCyNode.data().node;
        var actionJobHandler = self.actionTypeResolver.getActionJobHandler(actionNode.actionType);
        actionJobHandler.handleImport(actionNode, x2js.xml_str2json(actionSettings)[actionNode.actionType]);
        self.flowRenderer.hideOverlayNodeActions();
      });
      reader.readAsText(file);
    },
    importActionNodeLocalFS(file){
      var self = this;
      var reader = new FileReader();
      reader.addEventListener("load", function (event) {
        self.importActionNodeFromString(event.target.result);
      });
      reader.readAsText(file);
    },
    showWorkflowSla (value) {
      this.set('showWorkflowSla', value);
    },
    showCreateKillNode (value){
      this.set('showKillNodeManager', value);
      this.set('addKillNodeMode', true);
      this.set('editMode', false);
    },
    showKillNodeManager (value){
      this.set('showKillNodeManager', value);
      this.set('addKillNodeMode', false);
    },
    closeKillNodeManager(){
      this.set("showKillNodeManager", false);
    },
    showVersionSettings(value){
      this.set('showVersionSettings', value);
    },
    showingParameterSettings(value){
      if(this.get('workflow.parameters') !== null){
        this.set('parameters', Ember.copy(this.get('workflow.parameters')));
      }else{
        this.set('parameters', {});
      }
      this.set('showParameterSettings', value);
    },
    showCredentials(value){
      this.set('showCredentials', value);
    },
    createKillNode(killNode){
      this.set("killNode", killNode);
      this.set("createKillnodeError",null);
      var existingKillNode= this.get('workflow').get("killNodes").findBy("name",this.get('killNode.name')) || this.get('dataNodes').findBy("dataNodeName", this.get('killNode.name'));
      if (existingKillNode){
        this.set("createKillnodeError","Node with same name already exists");
        return;
      }
      if (Ember.isBlank(this.get('killNode.name'))){
        this.set("createKillnodeError","The kill node cannot be empty");
        return;
      }
      this.get("workflow").createKillNode(this.get('killNode.name'),this.get('killNode.killMessage'));
      this.set('killNode',{});
      this.$("#kill-node-dialog").modal("hide");
      this.set('showCreateKillNode', false);
    },
    addAction(type){
      this.createSnapshot();
      let temp = this.generateUniqueNodeId(type);
      this.get("workflow").addNode(this.getTransitionInfo(), type, {}, temp);
      this.rerender(function(){
        this.doValidation();
        this.scrollToNewPosition();
        this.showUndo('nodeAdded');
      }.bind(this));
    },
    nameChanged(){
      this.doValidation();
    },
    copyNode(node){
      this.copyNode(node);
    },
    pasteNode(){
      var clipboardContent = this.get('clipboardService').getContent();
      var node = this.get("workflow").addNode(this.getTransitionInfo(), clipboardContent.actionType);
      if(clipboardContent.operation === 'cut'){
        node.name = clipboardContent.name;
      }else{
        node.name = clipboardContent.name + '-copy';
      }
      node.domain = clipboardContent.domain;
      node.actionType = clipboardContent.actionType;
      this.rerender(function () {
        this.doValidation();
        this.scrollToNewPosition();
      }.bind(this));
    },
    deleteNode(node){
      this.deleteWorkflowNode(node);
    },
    openEditor(node){
      this.openWorkflowEditor(node);
    },
    showNotification(node){
      this.set("showNotificationPanel", true);
    },
    hideNotification(){
      this.set("showNotificationPanel", false);
    },
    addBranch(node){
      this.addWorkflowBranch(node);
    },
    addDecisionBranch(settings){
      this.createSnapshot();
      this.get("workflow").addDecisionBranch(settings);
      this.rerender();
    },
    setNodeTransitions(transition){
      var currentNode= this.get("currentNode");
      if(transition.errorNode && transition.errorNode.isNew){
        this.get("workflow").addKillNode(currentNode,transition.errorNode);
        this.get("workflow.killNodes").push(transition.errorNode);
      }else {
        this.set('currentNode.errorNode', transition.errorNode);
      }
      currentNode.transitions.forEach((trans)=>{
        if(transition.okToNode && trans.condition !== 'error'){
          if(trans.targetNode.id !== transition.okToNode.id){
            trans.targetNode = transition.okToNode;
          }
        }
      }, this);
    },
    submitWorkflow(){
      if(this.get('validationErrors') && this.get('validationErrors').length > 0){
        return;
      }
      this.set('dryrun', false);
      this.openJobConfig();
    },
    saveWorkflow(action){
      this.openSaveWorkflow();
    },
    previewWorkflow(){
      if(this.get('validationErrors') && this.get('validationErrors').length > 0){
        return;
      }
      this.set("showingPreview",false);
      this.get('workflowContext').clearErrors();
      var workflowGenerator=WorkflowGenerator.create({workflow:this.get("workflow"),
      workflowContext:this.get('workflowContext')});
      var workflowXml=workflowGenerator.process();
      if(this.get('workflowContext').hasErrors()){
        this.set('errors',this.get('workflowContext').getErrors());
      }else{
        this.set("previewXml",vkbeautify.xml(workflowXml));
        this.set("showingPreview",true);
      }
    },
    closePreview(){
      this.set("showingPreview", false);
    },
    downloadWorkflowXml(){
      this.get('workflowContext').clearErrors();
      var workflowGenerator=WorkflowGenerator.create({workflow:this.get("workflow"),
      workflowContext:this.get('workflowContext')});
      var workflowXml=workflowGenerator.process();
      if(this.get('workflowContext').hasErrors()){
        this.set('errors',this.get('workflowContext').getErrors());
      }else{
        this.workflowXmlDownload(workflowXml);
      }
    },
    closeWorkflowSubmitConfigs(){
      this.set("showingWorkflowConfigProps",false);
    },
    closeSaveWorkflow(){
      this.set("showingSaveWorkflow",false);
    },
    importWorkflowTest(){
      var deferred = this.importSampleWorkflow();
      deferred.promise.then(function(data){
        this.get("errors").clear();
        this.get("errors").pushObjects(data.errors);
        if (data.workflow === null) {
          return;
        }
        this.resetDesigner();
        this.set("workflow",data.workflow);
        this.rerender();
        this.doValidation();
      }.bind(this)).catch(function(data){
        console.error(data);
        this.set("errorMsg", "There is some problem while importing.");
        this.set("data", data);
      });
    },
    closeFileBrowser(){
      var self = this, path = this.get('workflowFilePath');
      this.set("showingFileBrowser",false);
      if(path){
          self.importWorkflow(path);
      }
    },
    showFileBrowser(){
      this.set('showingFileBrowser', true);
    },
    closeActionSettingsFileBrowser() {
      var self = this;
      this.set("showingActionSettingsFileBrowser", false);
      if(this.get('actionSettingsFilePath')){
        self.set("errorMsg", "");
        var actionSettingsXmlDefered=this.getAssetFromHdfs(this.get('actionSettingsFilePath'));
        actionSettingsXmlDefered.promise.then(function(data){
          this.importActionSettingsFromString(data);
        }.bind(this)).catch(function(data){
          self.set("errorMsg", "There is some problem while importing asset.");
          self.set("data", data);
        });
      }
    },
    showActionSettingsFileBrowser() {
      this.set('showingActionSettingsFileBrowser', true);
    },
    closeImportActionNodeFileBrowser() {
      var self = this;
      this.set("showingImportActionNodeFileBrowser", false);
      if(this.get('actionNodeFilePath')){
        self.set("errorMsg", "");
        var actionSettingsXmlDefered=this.getAssetFromHdfs(this.get('actionNodeFilePath'));
        actionSettingsXmlDefered.promise.then(function(data){
          this.importActionNodeFromString(data);
        }.bind(this)).catch(function(data){
          self.set("errorMsg", "There is some problem while importing asset.");
          self.set("data", data);
        });
      }
    },
    showImportActionNodeFileBrowser() {
      this.set('showingImportActionNodeFileBrowser', true);
    },
    closeExportActionNodeFileBrowser() {
      var self = this;
      this.set("showingExportActionNodeFileBrowser", false);
      if(this.get('exportActionNodeFilePath')){
        self.exportActionNodeXml();
      }
    },
    showExportActionNodeFileBrowser() {
      this.set('showingExportActionNodeFileBrowser', true);
    },
    createNewWorkflow(){
      if(Ember.isBlank(this.get('workflowFilePath'))){
        this.resetDesigner();
        this.rerender();
        this.set("workflowFilePath", "");
        this.$('#wf_title').focus();
      }else{
        this.importWorkflow(this.get('workflowFilePath'));
      }
    },
    conirmCreatingNewWorkflow(){
      this.set('showingConfirmationNewWorkflow', true);
    },
    showWorkflowGlobalProps(){
      if(this.get('workflow.globalSetting') !== null){
        this.set('globalConfig', Ember.copy(this.get('workflow.globalSetting')));
      }else{
        this.set('globalConfig', {});
      }
      this.set("showGlobalConfig", true);
    },
    closeWorkflowGlobalProps(){
      this.set("showGlobalConfig", false);
    },
    saveGlobalConfig(){
      this.set('workflow.globalSetting', Ember.copy(this.get('globalConfig')));
      this.set("showGlobalConfig", false);
    },
    closeWorkFlowParam(){
      this.set("showParameterSettings", false);
    },
    saveWorkFlowParam(){
      this.set('workflow.parameters', Ember.copy(this.get('parameters')));
      this.set("showParameterSettings", false);
    },
    zoomIn(){
      if(!this.get("zoomLevel")){
        this.resetZoomLevel();
      }
      this.decZoomLevel();
      var lev = this.get("zoomLevel") <= 0 ? 0.1 : this.get("zoomLevel");
      this.$("#flow-designer").css("transform", "scale(" + lev + ")");
    },
    zoomOut(){
      if(!this.get("zoomLevel")){
        this.resetZoomLevel();
      }
      this.incZoomLevel();
      var lev = this.get("zoomLevel") >= 1 ? 1 : this.get("zoomLevel");
      this.$("#flow-designer").css("transform", "scale(" + lev + ")");
    },
    zoomReset(){
      this.resetZoomLevel();
      this.$("#flow-designer").css("transform", "scale(" + 1 + ")");
    },
    resetLayout() {
      this.flowRenderer.resetLayout();
    },
    validateWorkflow(promise){
      this.currentNode.onSave();
      this.rerender();
      if(this.get('flowRenderer').isWorkflowValid()){
        promise.resolve();
      }else{
        promise.reject();
        this.send('undo');
        this.rerender();
      }
    },
    closeActionEditor (isSaved){
      this.send("hideNotification");
      if(isSaved){
        this.currentNode.onSave();
        this.doValidation();
        this.rerender();
      }	else {
        this.send('undo');
      }
      this.set('showActionEditor', false);
      var newGraph = this.flowRenderer.getGraph();
      if(!this.get('flowGraph').same(newGraph)){
        this.showUndo('transition');
      }
    },
    saveDraft(){
      this.persistWorkInProgress();
    },

    undo () {
      var workflowImporter = WorkflowJsonImporter.create({});
      var workflow = workflowImporter.importWorkflow(this.get('workflowSnapshot'));
      this.resetDesigner();
      this.set("workflow", workflow);
      this.rerender();
      this.doValidation();
      this.set('undoAvailable', false);
    },

    registerAddBranchAction(component){
      this.set("addBranchListener",component);
    },
    dryRunWorkflow(){
      this.set('dryrun', true);
      this.openJobConfig();
    },
    scheduleWorkflow(){
      if(!this.get('workflowFilePath')){
        console.error("Workflow doesnot exists");
        return;
      }
      this.sendAction('openTab', 'coord', this.get('workflowFilePath'));
    },
    showAssetConfig(value) {
      this.set('assetConfig', {});
      this.set('showingAssetConfig', value);
    },
    saveAssetConfig() {
      var self=this;
      self.set("isAssetPublishing", true);
      self.set("errorMsg", "");
      var workflowGenerator = WorkflowGenerator.create({workflow:self.get("workflow"), workflowContext:self.get('workflowContext')});
      var actionNodeXml = workflowGenerator.getActionNodeXml(self.flowRenderer.currentCyNode.data().name, self.flowRenderer.currentCyNode.data().node.actionType);
      var dynamicProperties = self.get('propertyExtractor').getDynamicProperties(actionNodeXml);
      self.set('assetConfig.type', self.flowRenderer.currentCyNode.data().node.actionType);
      self.set('assetConfig.definition', actionNodeXml);
      var saveAssetConfigDefered=self.get("assetManager").saveAsset(self.get('assetConfig'), self.getRandomDataToDynamicProps(dynamicProperties));
      saveAssetConfigDefered.promise.then(function(data){
        self.set("isAssetPublishing", false);
      }.bind(this)).catch(function(data){
        self.set("errorMsg", "There is some problem while saving asset.");
        self.set("data", data);
        self.set("isAssetPublishing", false);
      });
    },
    showAssetList(value) {
      var self=this;
      self.set('showingAssetList', value);
      self.set('assetListType', self.flowRenderer.currentCyNode.data().node.actionType);
    },
    importAsset(asset) {
      var self=this;
      self.set("isAssetImporting", true);
      self.set("errorMsg", "");
      var importAssetDefered=self.get("assetManager").importAssetDefinition(asset.id);
      importAssetDefered.promise.then(function(response){
        var importedAsset = JSON.parse(response).data;
        self.importActionSettingsFromString(importedAsset.definition);
        self.set("isAssetImporting", false);
      }.bind(this)).catch(function(data){
        self.set("errorMsg", "There is some problem while importing asset.");
        self.set("data", data);
        self.set("isAssetImporting", false);
      });
    },
    showAssetNodeList(value) {
      var self=this;
      self.set('showingAssetNodeList', value);
      self.set('assetListType', "");
    },
    importAssetNode(asset) {
      var self=this;
      self.set("isAssetImporting", true);
      self.set("errorMsg", "");
      var importAssetDefered=self.get("assetManager").importAssetDefinition(asset.id);
      importAssetDefered.promise.then(function(response){
        var importedAsset = JSON.parse(response).data;
        self.importActionNodeFromString(importedAsset.definition);
        self.set("isAssetImporting", false);
      }.bind(this)).catch(function(data){
        self.set("errorMsg", "There is some problem while importing asset.");
        self.set("data", data);
        self.set("isAssetImporting", false);
      });
    },
    closeInfo(flag) {
      this.set(flag, false);
    }
  }
});
