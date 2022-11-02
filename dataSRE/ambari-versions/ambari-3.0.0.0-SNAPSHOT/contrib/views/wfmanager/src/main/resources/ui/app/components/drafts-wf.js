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
const { computed } = Ember;

export default Ember.Component.extend({
  "search": "",
  "isBundle": true,
  "isCoordinator": true,
  "isWorkflow": true,
  "sortProp": ['updatedAt:desc'],
  "filteredModels": Ember.computed("recentFiles", "search", "isBundle", "isCoordinator", "isWorkflow", {
    get(key) {
    var score = 0, condition = true, searchTxt = this.get("search").toLowerCase(), isWorkflow = this.get("isWorkflow"), isCoordinator = this.get("isCoordinator"), isBundle = this.get("isBundle");
    return this.get("recentFiles").filter( (role) => {
      score = 0
      if(searchTxt && searchTxt.length) {
        condition = role.get('name') && role.get('name').toLowerCase().indexOf(searchTxt)>-1;
      }
      if(isWorkflow && role.get('type') === 'WORKFLOW') {
        score++;
      }
      if(isCoordinator && role.get('type') === 'COORDINATOR') {
        score++;
      }
      if(isBundle && role.get('type') === 'BUNDLE') {
        score++;
      }
      return condition && score > 0;
    });
    },
    set(key, value) {
      this.set('recentFiles', value);
      var score = 0, condition = true, searchTxt = this.get("search").toLowerCase(), isWorkflow = this.get("isWorkflow"), isCoordinator = this.get("isCoordinator"), isBundle = this.get("isBundle");
      return this.get("recentFiles").filter( (role) => {
        score = 0
        if(searchTxt && searchTxt.length) {
          condition = role.get('name') && role.get('name').toLowerCase().indexOf(searchTxt)>-1;
        }
        if(isWorkflow && role.get('type') === 'WORKFLOW') {
          score++;
        }
        if(isCoordinator && role.get('type') === 'COORDINATOR') {
          score++;
        }
        if(isBundle && role.get('type') === 'BUNDLE') {
          score++;
        }
        return condition && score > 0;
      });
      //return value;
    }
  }),
  resetDetails() {
    this.set("deleteMsg", null);
    this.set("deleteInProgress", false);
  },
  modelSorted : Ember.computed.sort("filteredModels", "sortProp"),
  "isDeleteDraftConformation": false,
  "currentDraft": undefined,
  "deleteInProgress": false,
  "deleteMsg": undefined,
  "currentJobService" : Ember.inject.service('current-job'),
  rendered : function(){
    var self = this;
    this.$("#projectDeleteModal").on('hidden.bs.modal', function () {
      self.set("isDeleteDraftConformation", true);
      self.set("deleteMsg", null);
      self.set("deleteInProgress", false);
    }.bind(this));
    this.$("#projectsList").modal("show");
    Ember.$("#loading").css("display", "none");
  }.on('didInsertElement'),
  store: Ember.inject.service(),
  actions: {
    importActionToEditor ( path, type ) {
      this.$("#projectsList").modal("hide");
      this.sendAction('editWorkflow', path, type);
    },
    confirmDelete (job ){
      this.send("showDeleteConfirmation", job);
    },
    close () {
      this.$("#projectsList").modal("hide");
      this.sendAction('close');
    },
    showActions (job) {
      this.$('.'+job.get("updatedAt")+'Actions').show();
      this.$('.Actions'+job.get("updatedAt")).hide();
    },
    hideActions (job) {
      this.$('.'+job.get("updatedAt")+'Actions').hide();
      this.$('.Actions'+job.get("updatedAt")).show();
    },
    showDeleteConfirmation(job) {
      this.resetDetails();
      this.set('currentDraft', job);
      this.set('showingDeleteConfirmation', true);
      Ember.run.later(()=>{
        this.$("#projectsDeleteConfirmation").modal("show");
      }, 100);
    },
    deleteWorkflow () {
      let job = this.get("currentDraft");
      this.set("deleteInProgress", true);
      var self= this;
      var rec = this.get("store").peekRecord('wfproject', job.id);
      if(rec){
        rec.destroyRecord().then(function () {
          self.get('recentFiles', self.get('store').peekAll("wfproject"));
          self.set("deleteInProgress", false);
          self.set("deleteMsg", "Successfully removed the item from history");
          self.get('store').unloadRecord(rec);
          self.set('filteredModels', self.get('store').peekAll("wfproject"));
          Ember.run.later(()=>{
            self.set('showingDeleteConfirmation', false);
            self.$("#projectsDeleteConfirmation").modal("hide");
          }, 1000);
        }).catch(function (response) {
          self.get('store').unloadRecord(rec);
          self.get('filteredModels', self.get('store').peekAll("wfproject"));
          self.sendAction("showProjectManagerList");
          self.set("deleteInProgress", false);
          self.set("deleteMsg", "There is some problem while deletion.Please try again.");
        });
      }
    }
  }
});
