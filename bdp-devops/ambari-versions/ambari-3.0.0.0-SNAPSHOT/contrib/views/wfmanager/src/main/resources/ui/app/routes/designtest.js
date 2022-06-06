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

export default Ember.Route.extend({
  actions:{
    showChart() {
      //       jsPlumb.ready(function() {
      // var a = $("#a");
      // var b = $("#b");
      var connectionColor="#777";
      var lineWidth=1;
      var stateMachineConnector = {
        connector:"Bezier",
        paintStyle:{lineWidth:3,strokeStyle:"#056"},
        hoverPaintStyle:{strokeStyle:"#dbe300"},
        endpoint:"Blank",
        anchor:"Continuous",
        overlays:[ ["PlainArrow", {location:1, width:15, length:12} ]]
      };

      var connection=jsPlumb.connect({
          source:"a",
          target:"b",
          //connector:["Flowchart", { stub:1,alwaysRespectStubs :true,cornerRadius: 5 }],
        //  connector:["Straight"],
        //  connector:["StateMachine",{curviness:0}],
          //connector: ["Bezier"],
          paintStyle:{lineWidth:lineWidth,strokeStyle:connectionColor},
          endpointStyle:{fillStyle:'rgb(243,229,0)'},
          endpoint: ["Dot", {
            radius: 1
          }],
          alwaysRespectStubs:true,
          anchors: [["Bottom"],["Top"]]
        //  anchors: [["Continuous"],["Continuous"]]
      },stateMachineConnector);

      connection.addOverlay([ "Label", {label:"<span>hello</span>", location:0.75, id:"myLabel" } ]);
      // jsPlumb.connect({
      //   source:"b",
      //   target:"a"
      // }, stateMachineConnector);

      jsPlumb.repaintEverything();



  }
}
});
