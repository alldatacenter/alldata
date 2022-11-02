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
var defaultNodeColor = '#fff';
var actionNodeColor = '#f5f5f5';
var killNodeColor='#d43f3a'
var labelFunction=function(target) {
  if (!target.data().node.name) {
    return "";
  } else if (target.data().node.name.length>12){
    return target.data().node.name.slice(0, 12)+"...";
  } else{
    return target.data().node.name;
  }
};

var actionNodeImage = function(target) {
  if (target && target.data() && target.data().node && target.data().node.actionType) {
    return 'assets/' + target.data().node.actionType + '.png';
  }
};

export default Ember.Object.create({
  style: [
    {
      selector: 'node',
      style: {
        shape: 'data(shape)',
        'background-color': defaultNodeColor,
        'border-width': 1,
        'border-color': '#ABABAB',
        //'text-margin-x': 10,
        label: labelFunction,
        'text-valign': 'center',
        'font-size': 14,
        height: 40,
        width: 40
      }
    },
    {
      selector: 'node[type = "fork"]',
      style: {
        'background-image': 'assets/sitemap.png',
        'background-position-x': 10,
        width: 150
      }

    },
    {
      selector: 'node[type = "join"]',
      style: {
        'background-image': 'assets/join.png',
        'background-position-x': 10,
        width: 150
      }
    },
    {
      selector: 'node[type = "decision"]',
      style: {
        height: 60,
        width: 120
      }
    },
    {
      selector: 'node[type = "start"]',
      style: {
        'background-image': 'assets/play.png',
        label: ''
      }
    },
    {
      selector: 'node[type = "end"]',
      style: {
        'background-image': 'assets/stop.png',
        label: ''
      }
    },
    {
      selector: 'node[type = "kill"]',
      style: {
        'color': '#a52a2a'
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
      selector: 'node[type = "action"]',
      style: {
        'background-image': actionNodeImage,
        'background-position-x': 10,
        width: 150
      }
    },
    {
      selector: 'edge',
      style: {
        'curve-style': function(target){
           if (target.data().transition  && target.data().transition.isOnError()){
             return 'unbundled-bezier';
           }else{
             return 'haystack'
           }
        },
        'control-point-distances': 20,
        'control-point-step-size': 10,
				'target-arrow-shape': function(target){
          if (target.data().transition && target.data().transition.getTargetNode(false) && !target.data().transition.getTargetNode(false).isPlaceholder()) {
            return "triangle";
          }else{
            return "none";
          }
        },
        'color': function(target){
              if (!target.data().transition || !target.data().transition.isOnError()) {
                return "black"
              }else{
                return killNodeColor;
              }
        },
        width: 1,
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
  ]
});
