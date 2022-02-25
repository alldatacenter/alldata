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
var DefaultLayoutManager= Ember.Object.extend({
  doDagreLayout(component, nodes,edges){
    var g = new dagre.graphlib.Graph();
    g.setGraph({rankdir:"TB", nodesep:100,edgesep:200,marginx:40,ranksep:130});
    g.setDefaultEdgeLabel(function() { return {}; });

    for (var i = 0; i < nodes.length; i++) {
      var n = component.$(nodes[i]);
      g.setNode(n.attr("id"), {width: n.width(), height: n.height()});
    }

    for (var i = 0; i < edges.length; i++) {
      var c = edges[i];
      g.setEdge(c.source,c.target);
    }
    dagre.layout(g);
    return g;
  },
  doLayout(component,nodes,edges){
    var g=this.doDagreLayout(component, nodes,edges);
    g.nodes().forEach(function(v) {
      try{
        var nodeWidth=component.$("#" + v).width();
        var displacement=150-Math.floor(nodeWidth/2);
        component.$("#" + v).css("left", g.node(v).x+displacement + "px");
        component.$("#" + v).css("top",g.node(v).y+ "px");
      }catch(err){
      }
    });
  }
});
export {DefaultLayoutManager};
