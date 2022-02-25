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
import {WorkflowImporter} from '../domain/workflow-importer';
import {ActionTypeResolver} from "../domain/action-type-resolver";
import Constants from '../utils/constants';

export default Ember.Component.extend({
  workflowImporter: WorkflowImporter.create({}),
  actionTypeResolver: ActionTypeResolver.create({}),
  layoutConfigs: { name: 'dagre', fit: false, edgeSep: 100 },
  error : {},
  errorMessage : Ember.computed('error', function() {
    if(this.get('error').status === 400){
      return 'Remote API Failed';
    }else if(this.get('error').status === 401){
      return 'User Not Authorized';
    }
  }),
  id : Ember.computed('model.jobType', function() {
    if (this.get('model.jobType') === 'wf'){
      return this.get('model.id');
    } else if(this.get('model.jobType') === 'coords'){
      return this.get('model.coordJobId');
    } else if(this.get('model.jobType') === 'bundles'){
      return this.get('model.bundleJobId');
    }
  }),
  name : Ember.computed('model.jobType', function() {
    if (this.get('model.jobType') === 'wf'){
      return this.get('model.appName');
    } else if(this.get('model.jobType') === 'coords'){
      return this.get('model.coordJobName');
    } else if(this.get('model.jobType') === 'bundles'){
      return this.get('model.bundleJobName');
    }
  }),
  displayType : Ember.computed('model.jobType', function() {
    if(this.get('model.jobType') === 'wf'){
      return "Workflow";
    }else if(this.get('model.jobType') === 'coords'){
      return "Coordinator";
    }
    else if(this.get('model.jobType') === 'bundles'){
      return "Bundle";
    }
    return "Workflow";
  }),
  configurationProperties : Ember.computed('model', function(){
    var x2js = new X2JS();
    var configurationObj  = x2js.xml_str2json(this.get('model.conf'));
    return configurationObj.configuration.property;
  }),
  initialize : function(){
    if(this.get('currentTab')){
      this.$('.nav-tabs a[href="'+this.get('currentTab').attr("href")+'"]').click();
      if(this.get('model.actions')){
        this.set('model.actionDetails', this.get('model.actions')[0]);
      }
    }
    this.$('.nav-tabs').on('shown.bs.tab', function(event){
      this.sendAction('onTabChange', this.$(event.target));
    }.bind(this));
  }.on('didInsertElement'),

  getShape(nodeType) {
    switch(nodeType) {
      case 'start' :
      case 'end' :
      case 'kill' :
      case 'fork' :
      case 'join' :
      return 'ellipse';
      case 'action' :
      return 'roundrectangle';
      case 'decision' :
      return 'diamond';
      default :
      return 'star';
    }
  },

  getNodeActionByName(workflowActions, nodeName) {
    if (Ember.isArray(workflowActions)) {
      var actionInfo = workflowActions.filter(function (modelAction) {
        return modelAction.name === nodeName;
      });
      return actionInfo.length>0 ? actionInfo[0] : null;
    } else {
      return workflowActions;
    }
  },

  getActionNode(nodeName, nodeType) {
    if (nodeType === 'start') {
      nodeName = ':start:';
    }
    return this.getNodeActionByName(this.get('model.actions'), nodeName);
  },

  getActionStatus(nodeName, nodeType) {
    if (nodeType === 'start') {
      nodeName = ':start:';
    }
    var nodeAction = this.getNodeActionByName(this.get('model.actions'), nodeName);

    if (nodeAction) {
      return nodeAction.status;
    }
    return "Not-Visited";
  },

  getNodeActionType(workflowXmlString, nodeType, nodeName) {
    if (nodeType !== 'action') {
      return nodeType;
    }

    var workflowJson = new X2JS().xml_str2json(workflowXmlString);

    if (Ember.isArray(workflowJson['workflow-app'].action)) {
      var workflowActionJson = workflowJson['workflow-app'].action.filter(function (workflowAction) {
        return workflowAction._name === nodeName;
      });
      if (workflowActionJson.length>0) {
        return this.actionTypeResolver.getActionType(workflowActionJson[0]);
      }
    } else {
      return this.actionTypeResolver.getActionType(workflowJson['workflow-app'].action);
    }
    return '';
  },

  getBgColorBasedOnStatus(nodeStatus) {
    switch(nodeStatus) {
      case 'Not-Visited' :
      return '#ffffff';
      default :
      return '#e6e6e6';
    }
  },

  getFontColorBasedOnStatus(nodeStatus) {
    switch(nodeStatus) {
      case 'ERROR' :
      case 'KILLED' :
      case 'FAILED' :
      return '#a94442';
      default :
      return '#262626';
    }
  },

  getBorderColorBasedOnStatus(nodeStatus) {
    switch(nodeStatus) {
      case 'OK' :
        return '#5bb75b';
      case 'ERROR' :
      case 'KILLED' :
      case 'FAILED' :
        return '#a94442';
      default :
        return '#808080';
    }
  },

  getNodeLabelContent(nodeType) {
    switch(nodeType) {
      case 'fork' :
      return '\uf0e8';
      case 'join' :
      return '\uf0e8';
      default :
      return '';
    }
  },

  getCyDataNodes(workflow){
    var dataNodes = [];
    var self=this;
    workflow.nodeVisitor.process(workflow.startNode, function(node) {
      if (node.type === 'kill' && !(node.forceRenderNode || self.getActionNode(node.name, node.type))) {
        return;
      }
      var nodeActionStatus = self.getActionStatus(node.name, node.type);
      dataNodes.push({ data:
        { id: node.id, name: node.name, type: node.type,
        content: node.name + self.getNodeLabelContent(node.type),
        shape: self.getShape(node.type),
        bgColor: self.getBgColorBasedOnStatus(nodeActionStatus),
        fontColor: self.getFontColorBasedOnStatus(nodeActionStatus),
        borderColor: self.getBorderColorBasedOnStatus(nodeActionStatus) }
      });
        if (node.transitions.length > 0) {
          node.transitions.forEach(function(tran){
            var transitionBorderColor;
            var actionNode = self.getActionNode(node.name, node.type);
            if (tran.targetNode.type === 'kill' &&
              !((actionNode && actionNode.transition===tran.targetNode.name) || (node.isPlaceholder()))) {
              return;
            }
             if (tran.getTargetNode(true).isKillNode()  && !tran.isOnError()){
              tran.targetNode.forceRenderNode = true;
             }
            if (actionNode && (actionNode.transition===tran.targetNode.name ||actionNode.transition==='*' || (tran.targetNode.isPlaceholder() && actionNode.transition===tran.getTargetNode(true).name))) {
              transitionBorderColor = Constants.successfulFlowColor;
              if (tran.targetNode.isPlaceholder()) {
                tran.targetNode.successfulFlow = true;
              }
            }else{
              transitionBorderColor = Constants.defaultFlowColor;
            }
            if (!actionNode){
              transitionBorderColor = Constants.defaultFlowColor;
              if (node.isPlaceholder() && node.successfulFlow) {
                transitionBorderColor = Constants.successfulFlowColor;
              }
            }
            dataNodes.push(
              {
                data: {
                  id: tran.sourceNodeId + '_to_' + tran.targetNode.id,
                  source:tran.sourceNodeId,
                  target: tran.targetNode.id,
                  transition: tran,
                  borderColor: transitionBorderColor
                }
              }
            );
          });
        }
      });
      return dataNodes;
    },
    showActionNodeDetail(node, workflowXmlString){
      var nodeName = node.data().name;
      if (nodeName === 'Start') {
        nodeName = ':start:';
      }
      this.set('model.nodeName', nodeName);
      this.set('model.nodeType', this.getNodeActionType(workflowXmlString, node.data().type, nodeName));
      this.set('model.actionDetails', null);
      var actionInfo = this.getNodeActionByName(this.get('model.actions'), nodeName);
      this.set('model.actionInfo', actionInfo);
    },
    renderDag(xmlString){
      var wfObject = this.get("workflowImporter").importWorkflow(xmlString);
      var workflow = wfObject.workflow;
      var dataNodes=this.getCyDataNodes(workflow);
      if (dataNodes.length > Constants.flowGraphMaxNodeCount) {
        this.set("model.flowGraphMaxNodeCountReached", true);
        this.set("model.inProgress", false);
        return;
      } else {
        this.set("model.flowGraphMaxNodeCountReached", false);
      }
      var cy = cytoscape({
        container: document.getElementById('cy'),
        elements: dataNodes,
        style: [
          {
            selector: 'node',
            style: {
              shape: 'data(shape)',
              'color': 'data(fontColor)',
              'background-color': 'data(bgColor)',
              'border-width': 1,
              'border-color': 'data(borderColor)',
              label: 'data(name)',
              'text-valign': 'center',
              'font-size': 8
            }
          },
          {
            selector: 'node[type = "placeholder"]',
            style: {
              width: 1,
              height: 1,
              label: ''
            }
          },
          {
            selector: 'node[shape = "roundrectangle"]',
            style: {
              width: 100,
              'border-radius': 1,
            }
          },
          {
            selector: 'node[type = "fork"]',
            style: {
              'background-image': 'assets/sitemap.png',
              'text-halign': 'left'
            }
          },
          {
            selector: 'node[type = "join"]',
            style: {
              'background-image': 'assets/join.png',
              'text-halign': 'left'
            }
          },
          {
            selector: 'edge',
            style: {
              width: 1,
              'line-color': 'data(borderColor)',
              'curve-style': 'bezier',
              'target-arrow-shape': function(target){
                if (target.data().transition && target.data().transition.getTargetNode(false) && !target.data().transition.getTargetNode(false).isPlaceholder()) {
                  return "triangle";
                }else{
                  return "none";
                }
              },
              'target-arrow-color': 'data(borderColor)',
              'color': '#262626',
              'font-size': 12,
              label: function(target) {
                if (!target.data().transition || !target.data().transition.condition) {
                  return "";
                }else if (target.data().transition.condition.length>5){
                  return target.data().transition.condition.slice(0, 5)+"...";
                }else{
                  return target.data().transition.condition;
                }
              }
            }
          }
        ],
        layout: this.get("layoutConfigs"),
      });

      // the default values of each option are outlined below:
      var defaults = {
        zoomFactor: 2.0, // zoom factor per zoom tick
        minZoom: 0.1, // min zoom level
        maxZoom: 10, // max zoom level

        // icon class names
        sliderHandleIcon: 'fa fa-minus',
        zoomInIcon: 'fa fa-plus',
        zoomOutIcon: 'fa fa-minus',
        resetIcon: 'fa fa-expand'
      };

      cy.panzoom( defaults );
      cy.pan({x:200,y:50});

      cy.on('click', 'node', function(event) {
        var node = event.cyTarget;
        this.showActionNodeDetail(node, xmlString);
      }.bind(this));

      cy.on('mousemove', 'edge', function(event) {
        this.get("context").$(".overlay-transition-content, .decision-condition-label").hide();
        if (event.cyTarget.data().transition && event.cyTarget.data().transition.condition) {
          this.get("context").$(".decision-condition-body").html(event.cyTarget.data().transition.condition);
          this.get("context").$(".overlay-transition-content").css({
            top: event.originalEvent.offsetY + 10,
            left: event.originalEvent.offsetX + 15
          });
          this.get("context").$(".overlay-transition-content, .decision-condition-label").show();
        }
      }.bind(this));

      cy.on('mouseout', 'edge',function(event) {
        this.get("context").$(".overlay-transition-content").hide();
      }.bind(this));

      this.set("model.inProgress", false);
    },
    importSampleWorkflow (){
      var self=this;
      Ember.$.ajax({
        url: "/sampledata/workflow.xml",
        dataType: "text",
        cache:false,
        success: function(data) {
          self.renderDag(data);
        }.bind(this),
        failure : function(data){
          console.error(data);
        }
      });
    },
    actions : {
      back (jobType, jobId){
        this.sendAction('back', jobType, jobId);
      },
      close : function(){
        this.sendAction('close');
      },
      doRefresh : function(){
        var tab = this.$(this.get('currentTab')).prop('href');
        if (tab) {
          var currentTabHref = tab.substr(tab.indexOf('#')+1);
          if(currentTabHref === 'jobLog'){
            this.send('getJobLog', this.get('logParams'));
          }else if(currentTabHref === 'jobErrorLog'){
            this.send('getErrorLog');
          }else if(currentTabHref === 'jobAuditLog'){
            this.send('getAuditLog');
          }else if(currentTabHref === 'jobDag'){
            this.send('getJobDag');
          }else if(currentTabHref === 'coordActionReruns'){
            this.send('getCoordActionReruns');
          }else{
            this.sendAction('doRefresh');
          }
        } else {
          this.sendAction('doRefresh');
        }
      },
      getJobDefinition : function () {
        Ember.$.get(Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=definition&timezone=GMT',function(response){
          this.set('jobDefinition', (new XMLSerializer()).serializeToString(response).trim());
        }.bind(this)).fail(function(error){
          this.set('error',error);
        }.bind(this));
      },
      showFirstActionDetail : function(){
        this.set('model.actionDetails', this.get('model.actions')[0]);
      },
      getJobLog : function (params){
        this.set('logParams', params);
        var url = Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=log';
        if(params && params.logFilter){
          url = url + '&logfilter=' + params.logFilter;
        }
        if(params && params.logActionList){
          url = url + '&type=action&scope='+ params.logActionList;
        }
        Ember.$.get(url,function(response){
          if(Ember.isBlank(response)){
            response = 'No Logs';
          }
          this.set('model.jobLog', response);
        }.bind(this)).fail(function(error){
          this.set('error', error);
        }.bind(this));
      },
      getErrorLog : function (){
        Ember.$.get(Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=errorlog',function(response){
          if(Ember.isBlank(response)){
            response = 'No Errors';
          }
          this.set('model.errorLog', response);
        }.bind(this)).fail(function(error){
          this.set('error', error);
        }.bind(this));
      },
      getAuditLog : function (){
        Ember.$.get(Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=auditlog',function(response){
          if(Ember.isBlank(response)){
            response = 'No Logs';
          }
          this.set('model.auditLog', response);
        }.bind(this)).fail(function(error){
          this.set('error', error);
        }.bind(this));
      },
      getJobDag : function (){
        this.set("model.inProgress", true);
        //if (true) return this.importSampleWorkflow();
        Ember.$.get(Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=definition&timezone=GMT',function(response){
          var xmlString = (new XMLSerializer()).serializeToString(response).trim();
          this.renderDag(xmlString);
        }.bind(this)).fail(function(error){
          this.set('error',error);
          this.set("model.inProgress", false);
        }.bind(this));
        // this.set('model.jobDag', Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=graph');
      },
      getCoordActionReruns : function () {
        var url = Ember.ENV.API_URL+'/v2/job/'+this.get('id')+'?show=allruns&type=action';
        if(this.get('rerunActionList')){
          url = url + '&scope=' + this.get('rerunActionList');
        }
        Ember.$.get(url, function(response){
          this.set('model.coordActionReruns', response.workflows);
        }.bind(this)).fail(function(error){
          this.set('error', error);
        }.bind(this));
      },
      getActionDetails : function (actionInfo) {
        this.set('model.actionDetails', actionInfo);
      },
      showWorkflow : function(workflowId){
        this.sendAction('showWorkflow', workflowId);
      },
      showCoord : function(coordId){
        this.sendAction('showCoord', coordId);
      },
      editWorkflow(path){
        var x2js = new X2JS();
        var configurationObj  = x2js.xml_str2json(this.get('model.conf'));
        this.sendAction('editWorkflow', path, null, true, {"settings":configurationObj});
      }
    }
  });
