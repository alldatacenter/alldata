/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Ember from 'ember';

export default Ember.Route.extend({
  fileOperationService: Ember.inject.service('file-operation'),
  model: function() {
    var promise = {
      homeDir: this.get('fileOperationService').getHome(),
      trashDir:  this.get('fileOperationService').getTrash()
    };

    return Ember.RSVP.hashSettled(promise).then(function(hash) {
      var response = {
        homeDir: {path: '', hasError: true},
        trashDir: {path: '', hasError: true}
      };

      if(hash.homeDir.state === 'fulfilled'){
        response.homeDir.path = hash.homeDir.value.path;
        response.homeDir.hasError = false;
      }

      if(hash.trashDir.state === 'fulfilled'){
        response.trashDir.path = hash.trashDir.value.path;
        response.trashDir.hasError = false;
      }

      return response;
    });
  },
  setupController: function(controller, hash) {
    this._super(controller, hash);
    if(hash.homeDir.hasError === false) {
      this.controllerFor('files').set('homePath', hash.homeDir.path);
      this.controllerFor('files').set('hasHomePath', true);
    }

    if(hash.trashDir.hasError === false) {
      this.controllerFor('files').set('trashPath', hash.trashDir.path);
      this.controllerFor('files').set('hasTrashPath', true);
    }
  },

  actions: {
    loading(transition, route) {
      let startTime = moment();
      let appController = this.controllerFor('application');
      // when the application loads up we want the loading template to be
      // rendered and not the loading spinner in the application template
      if(appController.get('firstLoad') === false) {
        appController.set('isLoading', true);
      }
      transition.promise.finally(() => {
        console.log("Loaded in " + (moment() - startTime) + "ms");
        appController.set('isLoading', false);
        appController.set('firstLoad', false);
      });
      return appController.get('firstLoad');
    }
  }
});
