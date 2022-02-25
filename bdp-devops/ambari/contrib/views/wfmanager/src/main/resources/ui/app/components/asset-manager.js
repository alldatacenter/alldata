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
  assetManager : Ember.inject.service('asset-manager'),
  assetSearchCriteria: "",
  currentAsset: null,
  filteredAssetList:  Ember.A([]),
  fuseSearchOptions: {
    shouldSort: true,
    threshold: 0.1,
    location: 0,
    distance: 100,
    maxPatternLength: 32,
    keys: [{
      name: 'name',
      weight: 0.3
    }, {
      name: 'type',
      weight: 0.5
    }, {
      name: 'description',
      weight: 0.1
    }, {
      name: 'owner',
      weight: 0.1
    }]
  },
  initialize: function() {
    var self = this;
    this.$('#asset_manager_dialog').modal('show');
    this.$('#asset_manager_dialog').modal().on('hidden.bs.modal', function() {
      this.sendAction('showAssetManager', false);
    }.bind(this));

    self.set("inProgress", true);
    self.set("errorMsg", "");
    var fetchAssetsDefered=self.get("assetManager").fetchMyAssets();
    fetchAssetsDefered.promise.then(function(response){
      self.set('assetList', JSON.parse(response).data);
      self.initializeFuseSearch();
      self.set("inProgress", false);
    }.bind(this)).catch(function(data){
      self.set("errorMsg", "There is some problem while fetching assets. Please try again.");
      self.set("inProgress", false);
    });
  }.on('didInsertElement'),
  initializeFuseSearch() {
     this.set('fuse', new Fuse(this.get("assetList"), this.get('fuseSearchOptions')));
     this.set('filteredAssetList', this.get("assetList"));
   },
   assetSearchCriteriaObserver : Ember.observer('assetSearchCriteria', function(){
     if (this.get("assetSearchCriteria") !== "") {
       this.set('filteredAssetList', this.get('fuse').search(this.get("assetSearchCriteria")));
     } else {
       this.set('filteredAssetList', this.get("assetList"));
     }
   }),
  actions: {
    close() {
      this.$('#asset_manager_dialog').modal('hide');
    },
    deleteAsset() {
      var self=this;
      self.set("inProgress", true);
      self.set("errorMsg", "");
      self.set("successMsg", "");
      var deleteAssetDefered=self.get("assetManager").deleteAsset(self.get("currentAsset").id);
      deleteAssetDefered.promise.then(function(response){
        var fetchAssetsDefered=self.get("assetManager").fetchMyAssets();
        fetchAssetsDefered.promise.then(function(response){
          self.get("assetList").clear();
          self.get("assetList").pushObjects(JSON.parse(response).data);
          if (self.get("assetSearchCriteria") !== "") {
            self.set('filteredAssetList', self.get('fuse').search(self.get("assetSearchCriteria")));
          } else {
            self.set('filteredAssetList', self.get("assetList"));
          }
          self.set("successMsg", "Asset got deleted successfully");
          self.set("inProgress", false);
        }.bind(this)).catch(function(data){
          self.set("errorMsg", "There is some problem while fetching assets. Please try again.");
          self.set("inProgress", false);
        });
      }.bind(this)).catch(function(data){
        self.set("errorMsg", "There is some problem while deleting asset. Please try again.");
        self.set("inProgress", false);
      });
    },
    showDeleteAssetWarning(asset) {
      this.set("currentAsset", asset);
      this.set('showingDeleteAssetWarning', true);
      Ember.run.later(()=>{
        this.$('#ConfirmDialog').modal('show');
      });
    }
  }
});
