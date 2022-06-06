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
import { validator, buildValidations } from 'ember-cp-validations';

const Validations = buildValidations({
  'assetModel.name': validator('presence', {
    presence : true
  })
});

export default Ember.Component.extend(Validations, {
  assetManager : Ember.inject.service('asset-manager'),
  initialize: function(){
    this.$('#asset_config_dialog').modal('show');
    this.$('#asset_config_dialog').modal().on('hidden.bs.modal', function() {
      this.sendAction('showAssetConfig', false);
    }.bind(this));
  }.on('didInsertElement'),
  actions: {
    close() {
      this.$('#asset_config_dialog').modal('hide');
    },
    save() {
      if(this.get('validations.isInvalid')) {
        this.set('showErrorMessage', true);
        return;
      }
      this.set("inProgress", true);
      var assetNameAvailableDefered=this.get("assetManager").assetNameAvailable(this.get("assetModel.name"));
      assetNameAvailableDefered.promise.then(function(data){
        this.set("inProgress", false);
        if (data === "false") {
          this.set("assetErrorMsg", "Asset name already exists");
          return;
        } else {
          this.$('#asset_config_dialog').modal('hide');
          this.sendAction('saveAssetConfig');
        }
      }.bind(this)).catch(function(data){
        this.set("inProgress", false);
        this.set("assetErrorMsg", "There is some problem while checking asset name availability. Please try again.");
      }.bind(this));
    }
  }
});
