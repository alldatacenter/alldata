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
var FindNodeMixin= Ember.Mixin.create({
  findNodeById(startNode,id){
    return this._findNodeById(startNode,id);
  },
  findNodeByType(startNode,type){
    return this._findNodeByAttr(startNode,type,"type");
  },
  findTransition(startNode,sourceId,targetId){
    return this._findTransition(startNode,sourceId,targetId);
  },
  _findTransition(node,sourceId,targetId){
    if (!node.transitions){
      return null;
    }
    var res;
    for (var i = 0; i < node.transitions.length; i++) {
      var tran= node.transitions[i];
      if (node.id===sourceId && tran.getTargetNode(false).id===targetId){
        res=tran;
      }else{
        res=this._findTransition(tran.getTargetNode(false),sourceId,targetId);
      }
      if (res){
        break;
      }
    }
    return res;
  },
  findTransitionTo(startNode,nodeid){
    return this._findTransitionTo(startNode,nodeid);
  },
  _findTransitionTo(node,nodeid){
    if (!node.transitions){
      return null;
    }
    var res;
    for (var i = 0; i < node.transitions.length; i++) {
      var tran= node.transitions[i];
      if (tran.getTargetNode(false).id===nodeid){
        res=tran;
      }else{
        res=this._findTransitionTo(tran.getTargetNode(false),nodeid);
      }
      if (res){
        break;
      }
    }
    return res;
  },
  _findNodeById(node,id){
    return this._findNodeByAttr(node,id,"id");
  },
  _findNodeByAttr(node,id,attrType){
    var self=this;
    if (node.get(attrType)===id){
      return node;
    }else{
      if (node.transitions){
        var res;
        for (var i = 0; i < node.transitions.length; i++) {
          var transition=node.transitions[i];
          res= self._findNodeByAttr(transition.getTargetNode(false),id,attrType);
          if (res){
            break;
          }
        }
        return res;
      }else{
        return null;
      }
    }
  },
  getOKToNode(node){
    var okToNode;
    if (!node){
      okToNode = null;
    }else{
      var transitions = node.transitions;
      transitions.forEach(function(trans){
        if (!trans.condition){
          okToNode = trans.targetNode;
          return;
        }
      });
    }
    return okToNode;
  },
  getDesendantNodes(node, ignoreEndNode){
    if (!node){
      return null;
    }
    var currNode=null;
    var nodes = [], nxtPath = node.getTargets();
    for(var i =0; i< nxtPath.length; i++){
      currNode = nxtPath[i];
      do {
        if(this._insertUniqueNodes(currNode, nodes) && currNode){
          nodes.push(currNode);
        }
        var nodesList = currNode.getTargets();
        if(nodesList.length > 1){
          for(var j=0; j<nodesList.length; j++) {
            if(nodesList[j].getTargets().length>1){
              var tmp = this.getDesendantNodes(nodesList[j]);
              if(tmp.length){
                nodes = nodes.concat(tmp);
              }
            } else if(this._insertUniqueNodes(nodesList[j], nodes) && nodesList[j]){
              nodes.push(nodesList[j]);
              currNode = nodesList[j];
            } else {
              currNode = nodesList[j];
            }
          }
        } else {
          currNode = nodesList[0];
        }
      } while(currNode && currNode.get("id") && currNode.get("id") !== "node-end");
    }
    if(!ignoreEndNode && currNode){
      nodes.push(currNode);
    }
    return nodes;
  },
  _insertUniqueNodes(currNode, nodes){
    if(nodes.indexOf(currNode) > -1){
    } else {
      if (!( currNode.isKillNode() || currNode.isPlaceholder() || currNode.isJoinNode() || currNode.isDecisionEnd())){
        return true;
      }
    }
  },
  _findCommonTargetNodeId(node){
    var nodeIds = {}, targ, decPath = node.getTargets(), tempId = 0;
    for(var i =0; i< decPath.length; i++){
      var currNode = decPath[i];
      do {
        if(nodeIds.hasOwnProperty(currNode.get("id"))){
          nodeIds[currNode.get("id")] = nodeIds[currNode.get("id")] + 1;
        } else {
          nodeIds[currNode.get("id")] = 1;
        }
        if(currNode.get("id") === "node-end"){
          break;
        }
        currNode = currNode.getTargets()[0];
      } while(currNode && currNode.get("id"));
    }
    for(var j in nodeIds){
      if(tempId < nodeIds[j]){
        targ = j;
        tempId = nodeIds[j];
      }
    }
    return targ;
  },
  _findCommonTargetNode(node){
    var nodeIds = {}, targ, decPath = node.getTargets(), tempId = 0;
    for(var i =0; i< decPath.length; i++){
      var currNode = decPath[i];
      do {
        if(nodeIds.hasOwnProperty(currNode.get("id"))){
          nodeIds[currNode.get("id")] = nodeIds[currNode.get("id")] + 1;
        } else {
          nodeIds[currNode.get("id")] = 1;
        }
        if(currNode.get("id") === "node-end"){
          break;
        }
        currNode = currNode.getTargets()[0];
      } while(currNode && currNode.get("id"));
    }
    for(var j in nodeIds){
      if(tempId < nodeIds[j]){
        targ = j;
        tempId = nodeIds[j];
      }
    }
    return targ;
  },
  findCommonTargetNode(startNode,node){
    var commonTargetId=this._findCommonTargetNodeId(node);
    return this.findNodeById(startNode,commonTargetId);
  }
});
export{FindNodeMixin};
