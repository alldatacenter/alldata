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
import CytoscapeStyles from '../domain/cytoscape-style';
var CytoscapeRenderer= Ember.Object.extend({
  currentCyNode: null,
  staticNodes: ['start', 'end', 'placeholder'],
  dataNodes: [],
  cyOverflow: {},
  cy: null,
  layoutConfigs: { name: 'dagre', fit: false, edgeSep: 100 },
  _initCY(settings){
    this.get("context").$('#'+this.id).height(settings.canvasHeight);
    this.cy = cytoscape({
      container: this.get("context").$('#'+this.id),
      elements: [],
      style: CytoscapeStyles.style,
      layout: this.get("layoutConfigs"),
      pixelRatio : 1
    });

    // the default values of each option are outlined below:
    var defaults = {
      zoomFactor: 0.01, // zoom factor per zoom tick
      minZoom: 0.1, // min zoom level
      maxZoom: 10, // max zoom level

      // icon class names
      sliderHandleIcon: 'fa fa-minus',
      zoomInIcon: 'fa fa-plus',
      zoomOutIcon: 'fa fa-minus',
      resetIcon: 'fa fa-expand'
    };

    this.cy.panzoom( defaults );
    this._addEvents(this.cy);
    var self = this;
    this.get("context").$('.overlay-transition-content').popover({
      html : true,
      title : "Add Node <button type='button' class='close'>&times;</button>",
      placement: 'right',
      trigger : 'focus',
      content : function(){
        return self.get("context").$('#workflow-actions').html();
      }
    });

    this.get("context").$("#cyRenderer").on('click','.popover .close',function(){
      this.get("context").$('.popover').popover('hide');
    }.bind(this));
  },
  _setCyOverflow() {
    Ember.set(this.get("cyOverflow"), "overflown", this.cy.elements().renderedBoundingBox().y2 > this.cy.height());
  },
  setGraphCenter() {
    if (this.cy && !this.centered){
      Ember.run.later(this, function() {
        this._setGraphCenterOnStartNode();
      },50);
    }
  },
  _setGraphCenterOnStartNode() {
    var startDataNode = this.get("dataNodes").filterBy("data.type", "start");
    if (startDataNode[0] && startDataNode[0].data.id) {
      var startNode = this.cy.$("#" + startDataNode[0].data.id);
      this.cy.center(startNode);
      this.cy.pan({y:50});
    }
  },
  _getShape(nodeType) {
    switch(nodeType) {
      case 'start' :
      case 'end' :
      case 'kill' :
      case 'placeholder' :
      return 'ellipse';
      case 'action' :
      return 'roundrectangle';
      case 'fork' :
      case 'join' :
      return 'roundrectangle';
      case 'decision' :
      return 'diamond';
      default :
      return 'star';
    }
  },

  _getCyDataNodes(workflow){
    this.get('dataNodes').clear();
    var self=this;
    var errorNodeCounter=1;
    workflow.nodeVisitor.process(workflow.startNode, function(node) {
      if (node.type === 'kill') {
        return;
      }

      self.get('dataNodes').pushObject({
        data: {
          id: node.id, name: node.name,
          shape: self._getShape(node.type),
          type : node.type,
          node: node
        },
        dataNodeName: Ember.computed.alias('data.node.name')
      });
      if (node.transitions.length > 0) {
        var counter=0;
        node.transitions.forEach(function(transition){
          if ((transition.isOnError() && transition.getTargetNode().isKillNode())){
            return;
          }
          counter++;
          var targetNodeId=transition.targetNode.id;
          if (transition.targetNode.isKillNode()){
            errorNodeCounter++;
            var errorNode=transition.targetNode;
            targetNodeId=errorNode.id+errorNodeCounter;
            self.get('dataNodes').pushObject({
              data: {
                id: targetNodeId, name: errorNode.name,
                shape: self._getShape(errorNode.type),
                type : errorNode.type,
                node: errorNode
              },
              dataNodeName: Ember.computed.alias('errorNode.node.name')
            });
          }
          self.get('dataNodes').pushObject(
            {
              data: {
                id: transition.sourceNodeId + '_to_' + targetNodeId+"_"+counter,
                source:transition.sourceNodeId,
                target: targetNodeId,
                transition: transition,
                transitionCount: node.getOkTransitionCount()
              }
            }
          );

        });
      }
    });
  },

  _showNodeEditor(node, nodeObj){
    if (nodeObj && this.get("currentCyNode") && (nodeObj.data().id === this.get("currentCyNode").data().id)) {
      if (this.staticNodes.contains(node.data().type)) {
        return;
      }
      this.get("context").$("#"+ node.data().id  + " :input").show();
      this.get("context").$("#"+ node.data().id  + " :input").css({
        top: nodeObj.renderedPosition().y  - (nodeObj.outerHeight()/2),
        left: nodeObj.renderedPosition().x  - (nodeObj.outerWidth()/2)
      });
    }
    this.set("currentCyNode", nodeObj);
  },

  _showNodeTooltip(event){
    var node = event.cyTarget;
    var nodeObj = this.cy.$('#' + node.id());
    if (this.staticNodes.contains(node.data().type)) {
      return;
    }
    this.get("context").$(".overlay-node-label").css({
      top: nodeObj.renderedPosition().y - (nodeObj.outerHeight() - ((node.data().type === 'decision')?20:0)),
      left: nodeObj.renderedPosition().x + (nodeObj.outerWidth()/2 - 30)
    });
    this.get("context").$(".overlay-node-label").text(node.data().node.name);
    this.get("context").$(".overlay-node-label").show();
  },

  _addEvents(cy){
    cy.on('pan', function() {
      this.get("context").$(".overlay_node_editor, .overlay-node-actions, .overlay-fork-icon, .overlay-trash-icon, .overlay-settings-icon").hide();
      this.get("context").$(".overlay-transition-content").hide();
      this._setCyOverflow();
    }.bind(this));

    cy.on('click', function(event) {
      if (event.cyTarget === cy) {
        this.get("context").$(".overlay_node_editor, .overlay-node-actions, .overlay-fork-icon, .overlay-trash-icon, .overlay-settings-icon").hide();
        this.get("context").$(".overlay-transition-content").hide();
      }
    }.bind(this));

    cy.on('mousemove', 'node', function(event) {
      event.cyTarget.css({'border-color': '#5bb75b'});
      this.get("context").actionInfo(event.cyTarget.data().node);
      this._showNodeTooltip(event);
    }.bind(this));

    cy.on('mouseout', 'node',function(event) {
      event.cyTarget.css({'border-color': '#ABABAB'});
      this.get("context").$(".overlay-node-label").hide();
    }.bind(this));

    cy.on('mousemove', 'edge', function(event) {
      event.cyTarget.css({'line-color': '#5bb75b', 'target-arrow-color': '#5bb75b'});
    }.bind(this));

    cy.on('mouseout', 'edge',function(event) {
      event.cyTarget.css({'line-color': '#ABABAB', 'target-arrow-color': '#ABABAB'});
    }.bind(this));

    cy.on('click', 'node', function(event) {
      this.get("context").$(".overlay-node-actions span").hide();
      this.get("context").$(".overlay-transition-content").hide();
      var node = event.cyTarget;
      var nodeObj = cy.$('#' + node.id());
      this._showNodeEditor(node, nodeObj);
      if (!(node.data().type === 'start' || node.data().type === 'end' || node.data().type === 'placeholder' ||  node.data().type === 'kill')) {
        this.get("context").$(".overlay-node-actions, .overlay-trash-icon").show();
      }
      if (node.data().type === 'action' || node.data().type === 'decision') {
        this.get("context").$(".overlay-settings-icon").show();
      }
      if (node.data().type === 'action') {
        this.get("context").$(".overlay-copy-icon, .overlay-cut-icon").show();
        this.get("context").$(".overlay-hdfs-asset-import-icon, .overlay-hdfs-asset-export-icon").show();
        this.get("context").$(".overlay-asset-import-icon, .overlay-asset-export-icon").show();
        if(this.get('context').get('clipboard')){
          this.get("context").$(".overlay-paste-icon").show();
        }
      }
      if (node.data().type === 'fork' || node.data().type === 'decision') {
        this.get("context").$(".overlay-fork-icon").show();
      }
      this.get("context").$(".overlay-node-actions").css({
        top: nodeObj.renderedPosition().y - (nodeObj.outerHeight()) + 20,
        left: nodeObj.renderedPosition().x + (nodeObj.outerWidth()/3) + 50
      });
      this.get("context").$(".overlay-trash-icon, .overlay-fork-icon, .overlay-settings-icon, .overlay-copy-icon, .overlay-paste-icon, .overlay-cut-icon").data("node", node.data().node);
    }.bind(this));

    cy.on('click', 'edge', function(event) {
      this.get("context").$(".decision-condition-label").hide();
      this.get("context").$(".overlay-transition-content").hide();
      this.get("context").$(".overlay_node_editor, .overlay-node-actions, .overlay-fork-icon, .overlay-trash-icon, .overlay-settings-icon").hide();
      this.get("context").$(".overlay-transition-content").show();
      this.get("context").$(".overlay-transition-content").css({
        top: event.originalEvent.offsetY + 10,
        left: event.originalEvent.offsetX + 15
      });
      if (event.cyTarget.data().transitionCount>1){
        this.get("context").$(".overlay-trash-transition-icon").show();
      }else{
        this.get("context").$(".overlay-trash-transition-icon").hide();
      }
      let srcNode = event.cyTarget.source().data("node");
      if(srcNode.type === 'placeholder'){
        let originalSource = event.cyTarget.source().incomers("node").jsons()[0].data.node;
        this.get("context").$(".overlay-transition-content").data("originalSource", originalSource);
      }
      this.get("context").$(".overlay-transition-content").data("sourceNode", srcNode);
      this.get("context").$(".overlay-transition-content").data("targetNode",event.cyTarget.target().data("node"));
      this.get("context").$(".overlay-transition-content").data("transition",event.cyTarget.data().transition);

      if (event.cyTarget.data().transition && event.cyTarget.data().transition.condition) {
        this.get("context").$(".decision-condition-body").html(event.cyTarget.data().transition.condition);
        this.get("context").$(".decision-condition-label").css({
          top: event.originalEvent.offsetY,
          left: event.originalEvent.offsetX + 10
        });
        this.get("context").$(".decision-condition-label").show();
      }
    }.bind(this));

    this.get("context").$('.overlay-plus-icon').off('click');
    this.get("context").$('.overlay-plus-icon').on('click',function(){
      this.get("context").$(".overlay-transition-content").popover("show");
      this.get("context").set('popOverElement', this.get("context").$('.overlay-transition-content'));
      this.get("context").setCurrentTransition({
        transition : this.get("context").$(".overlay-transition-content").data("transition"),
        source : this.get("context").$(".overlay-transition-content").data("sourceNode"),
        originalSource : this.get("context").$(".overlay-transition-content").data("originalSource"),
        target : this.get("context").$(".overlay-transition-content").data("targetNode")
      });
      Ember.run.later(this, function() {
        this.get("context").$('.overlay-transition-content').hide();
      }, 1000);
    }.bind(this));

    this.get("context").$('.overlay-trash-transition-icon').off('click');
    this.get("context").$('.overlay-trash-transition-icon').on('click',function(){
      var tran=this.get("context").$(".overlay-transition-content").data("transition");
      tran.sourceNode= this.get("context").$(".overlay-transition-content").data("sourceNode");
      this.get("context").deleteTransition(tran);
      this.get("context").$('.overlay-transition-content').hide();
    }.bind(this));

    this.get("context").$('.overlay-trash-icon i').off('click');
    this.get("context").$('.overlay-trash-icon i').on('click',function(){
      this.get("context").deleteWorkflowNode(this.get("context").$(".overlay-trash-icon").data("node"), this.getIncomingTransitions(this.get("currentCyNode")));
      this.get("context").$('.overlay-node-actions').hide();
    }.bind(this));

    this.get("context").$('.overlay-copy-icon i').off('click');
    this.get("context").$('.overlay-copy-icon i').on('click',function(){
      this.get("context").copyNode(this.get("context").$(".overlay-copy-icon").data("node"));
      this.get("context").$('.overlay-node-actions').hide();
    }.bind(this));

    this.get("context").$('.overlay-paste-icon i').off('click');
    this.get("context").$('.overlay-paste-icon i').on('click',function(){
      this.get("context").replaceNode(this.get("context").$(".overlay-paste-icon").data("node"));
      this.get("context").$('.overlay-node-actions').hide();
    }.bind(this));

    this.get("context").$('.overlay-cut-icon i').off('click');
    this.get("context").$('.overlay-cut-icon i').on('click',function(){
      this.get("context").cutNode(this.get("context").$(".overlay-cut-icon").data("node"), this.getIncomingTransitions(this.get("currentCyNode")));
      this.get("context").$('.overlay-node-actions').hide();
    }.bind(this));

    this.get("context").$('.overlay-fork-icon i').off('click');
    this.get("context").$('.overlay-fork-icon i').on('click',function(){
      var node = this.get("context").$(".overlay-fork-icon").data("node");

      if (node.isDecisionNode()) {
        this.get("context").openDecisionEditor(this.get("context").$(".overlay-fork-icon").data("node"));
        this.get("context").$("#selector-content").css({
          top: this.get("currentCyNode").renderedPosition().y - (this.get("currentCyNode").outerHeight()),
          left: this.get("currentCyNode").renderedPosition().x + (this.get("currentCyNode").outerWidth()/2)
        });
      } else if (node.isForkNode()) {
        this.get("context").addWorkflowBranch(this.get("context").$(".overlay-fork-icon").data("node"));
      }
      this.get("context").$('.overlay-node-actions').hide();
    }.bind(this));

    this.get("context").$('.overlay-settings-icon i').off('click');
    this.get("context").$('.overlay-settings-icon i').on('click',function(){
      let node = this.get("context").$(".overlay-settings-icon").data("node");
      this.populateOkToandErrorTONodes(node);
      this.get("context").openWorkflowEditor(node);
      this.get("context").$('.overlay-node-actions').hide();
    }.bind(this));
  },
  getIncomingTransitions(node){
    var incomingNodes=node.incomers("node").jsons().mapBy("data.node");
    var transitionList=[];
    var currentNodeId=this.get("currentCyNode").json().data.id;
    incomingNodes.forEach(function(incomingNode){
      incomingNode.transitions.forEach(function(incomingTran){
        if (incomingTran.targetNode.id===currentNodeId){
          incomingTran.sourceNode=incomingNode;
          transitionList=transitionList.concat(incomingTran);
        }
      });
    });
    return transitionList;
  },
  populateOkToandErrorTONodes(node){
    let alternatePathNodes = this.cy.$('#'+node.id).predecessors("node[name][type='decision']").union(this.cy.$('#'+node.id).predecessors("node[name][type='fork']"));
    let descendantNodes = [];
    if(alternatePathNodes.length > 0){
      alternatePathNodes.forEach(childNode =>{
        let childNodeData = childNode.data();
        if(childNodeData.type === 'placeholder'){
          return;
        }
        let successors = this.cy.$(`#${childNodeData.id}`).successors("node[name]").difference(this.cy.$('#'+node.id).incomers("node[name]"));
        descendantNodes.pushObjects(successors.jsons().mapBy('data.node'));
      });
    }else{
      descendantNodes.pushObjects(this.cy.$(`#${node.id}`).successors("node[name]").jsons().mapBy('data.node'));
    }
    let okToNodes = [];
    let errorToNodes = [];
    okToNodes = descendantNodes.reject((descendantNode)=>{
      return descendantNode.get('type') === 'placeholder' || descendantNode.get('type') === 'kill' || descendantNode.id === node.id;
    }, this);
    errorToNodes = descendantNodes.reject((descendantNode)=>{
      return descendantNode.get('type') === 'placeholder' || descendantNode.get('type') === 'kill' || descendantNode.id === node.id;
    }, this);
    node.set('validOkToNodes', okToNodes);
    node.set('validErrorToNodes', errorToNodes);
  },
  getGraph(){
    return this.cy.$('node');
  },
  isWorkflowValid(){
    return this.cy.nodes("node[name][type='start']").successors("node[name]").intersection(this.cy.nodes("node[name][type='end']")).length > 0;
  },
  renderWorkflow(workflow){
    this._getCyDataNodes(workflow);
    this.cy.startBatch();
    this.cy.$('node').remove();
    this.cy.add(this.get('dataNodes'));
    this.cy.endBatch();
    this.cy.layout(this.get("layoutConfigs"));
    this._setCyOverflow();
    this._setGraphCenterOnStartNode();
  },

  initRenderer(callback, settings){
    this.context=settings.context;
    this.dataNodes=settings.dataNodes;
    this.cyOverflow=settings.cyOverflow;
    this.id=settings.id;
    this._initCY(settings);
    callback();
  },

  fitWorkflow() {
    this.cy.fit();
  },

  hideOverlayNodeActions() {
    this.get("context").$('.overlay-node-actions').hide();
  },

  reset(){

  },
  resetLayout() {
    this.cy.layout();
  },
  refresh(){

  },
  onDidUpdate(){
    return true;
  },
  cleanup(){
  },
  resize(){
    if (this.cy){
      Ember.run.later(this, function() {
        this.cy.resize();
      },50);
    }
  },
  getBottomPosition() {
    return {
      top: this.get("context").$('#'+this.id).offset().top + this.get("context").$('#'+this.id).height,
      left: this.get("context").$('#'+this.id).offset().left + 100
    };
  }
});
export {CytoscapeRenderer};
