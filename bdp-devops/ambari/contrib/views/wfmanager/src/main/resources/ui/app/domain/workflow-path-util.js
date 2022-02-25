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
export default Ember.Object.create({
  findPath(source, target){
    var visitedNodes = [];
    var currentPath = [];
    var allPaths = [];
    this._findPath(source, target, visitedNodes, currentPath, 0, allPaths);
    return allPaths;
  },
  _findPath(source, target, visitedNodes, currentPath, pathIndex, allPaths){
    visitedNodes.pushObject(source);
    currentPath[pathIndex++] = source;
    if(source.id === target.id){
      if(!allPaths[allPaths.length]){
        var index = currentPath.indexOf(target);
        allPaths[allPaths.length] = currentPath.slice(0, index+1);
      }
    }
    if(source.hasTransition()){
      source.transitions.forEach((transition)=>{
        var node = transition.targetNode;
        if(node.hasTransition() && !visitedNodes.findBy('id', node.id)){
          this._findPath(node, target, visitedNodes, currentPath, pathIndex, allPaths);
        }
      }, this);
    }
    pathIndex--;
    visitedNodes.removeObject(source);
  },
  _getAllNodes(workflow){
    var workflowNodes = [];
    workflow.nodeVisitor.process(workflow.startNode, (node) =>{
      workflowNodes.pushObject(node);
    });
    return workflowNodes;
  },
  findValidTransitionsTo(workflow, node){
    var validTransitionsTo = [];
    if(!node.hasTransition()){
      return validTransitionsTo;
    }
    var paths = this.findPath(workflow.get('startNode'), node);
    var workflowNodes = this._getAllNodes(workflow);
    validTransitionsTo = workflowNodes.slice();
    workflowNodes.forEach((node)=>{
      paths.forEach((path)=>{
        if(path.contains(node)){
          validTransitionsTo.removeObject(node);
        }
      }, this);
    }, this);
    validTransitionsTo = validTransitionsTo.reject((node)=>{
      return node.get('type') === 'placeholder' || node.get('type') === 'kill';
    }, this);
    return validTransitionsTo;
  }
});
