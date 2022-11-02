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

export default Ember.Component.extend({
  initialize : function() {
    this.set('killNode', {});
    this.set('editMode', false);
  }.on('init'),

  rendered : function(){
    this.$('#killnode-manager-dialog').modal({
      backdrop: 'static',
      keyboard: false
    });
    this.$('#killnode-manager-dialog').modal('show');
    this.$('#killnode-manager-dialog').modal().on('hidden.bs.modal', function() {
      this.sendAction('closeKillNodeManager');
    }.bind(this));
  }.on('didInsertElement'),
  actions: {
    deleteNode(index) {
      this.get('killNodes').removeAt(index);
      this.set('editMode', false);
    },
    addNode() {
      this.set('createKillnodeError',null);
      this.set('editMode', false);
      this.set('addKillNodeMode', true);
      this.set('killNode', {});
    },
    editNode(index) {
      this.set('createKillnodeError', null);
      this.set('editMode', true);
      this.set('currentKillNodeIndex', index);
      var selectedKillNode = this.get('killNodes').objectAt(index);
      this.set('killNode', {name : selectedKillNode.name, killMessage: selectedKillNode.killMessage});
    },
    updateNode(){
      this.set('editMode', false);
      this.get('killNodes').objectAt(this.get('currentKillNodeIndex')).set('killMessage', this.get('killNode').killMessage);
      this.set('killNode', {});
    },
    createKillNode() {
      this.sendAction('createKillNode', this.get("killNode"));
    }
  }
});
