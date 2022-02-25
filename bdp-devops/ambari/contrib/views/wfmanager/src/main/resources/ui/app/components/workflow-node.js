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

export default Ember.Component.extend(Ember.Evented,{
  actionIcons : {
    "hive": "server",
    "hive2": "server",
    "pig": "product-hunt",
    "sqoop": "database",
    "hdfs": "copy",
    "java": "code",
    "shell": "terminal",
    "distcp": "clone",
    "map-reduce": "cubes",
    "spark": "star",
    "ssh": "terminal",
    "sub-workflow":"share-alt-square",
    "stream": "exchange",
    "email": "envelope",
    "fs":"folder-o"
  },
  icon : Ember.computed('actionIcons',function(){
    return this.get('actionIcons')[this.get('node.actionType')];
  }),
  nodeSpecificClasses : Ember.computed('node.type',function(){
    if(this.get('node.type') === 'start'){
      return "start";
    }else if(this.get('node.type') === 'end'){
      return "end";
    }else if(this.get('node.type') === 'kill'){
      return "kill";
    }else if(this.get('node.type') === 'decision'){
      return "decision_node";
    }else if(this.get('node.type') === 'decision_end'){
      return "decision_end";
    }else if(this.get('node.type') === 'fork'){
      return "fa fa-sitemap fork";
    }else if(this.get('node.type') === 'placeholder'){
      return "placeholder-node";
    }else if(this.get('node.type') === 'join'){
      return "fa fa-sitemap fa-rotate-180 control_flow_node join";
    }else if(this.get('node.type') === 'action'){
      return "action-node";
    }
  }),
  rendered : function(){
    if(this.get('node.type') === 'action') {
      this.$('input[name="actionName"]').focus();
      this.$('input[name="actionName"]').select();
    }
  }.on('didInsertElement'),
  nameChanged : function(){
    this.sendAction("onNameChange");
  }.observes('node.name'),
  actions : {
    registerAddBranchAction(component){
      this.set("addBranchListener",component);
    },
    showNotification(node){
       this.sendAction("showNotification", node);
    },
    // hideNotification(){
    //    this.sendAction("hideNotification");
    // },    
    openEditor (){
      this.sendAction("openEditor", this.get('node'));
    },
    showAddBranch(){
      this.get("addBranchListener").trigger("showBranchOptions");
    },
    addBranch(){
      this.sendAction("addBranch", this.get('node'));
    },
    deleteNode(){
      this.sendAction("deleteNode", this.get('node'));
    },
    addDecisionBranch(settings){
      this.sendAction("addDecisionBranch",settings);
    },
    copyNode(){
      this.sendAction("copyNode", this.get('node'));
    }
  }
});
