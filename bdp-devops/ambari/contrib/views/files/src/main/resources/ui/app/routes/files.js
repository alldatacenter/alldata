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
import FileOperationMixin from '../mixins/file-operation';

export default Ember.Route.extend(FileOperationMixin, {
  logger: Ember.inject.service('alert-messages'),
  fileSelectionService: Ember.inject.service('files-selection'),
  currentPath: '/',
  queryParams: {
    path: {
      refreshModel: true
    },
    filter: {
      refreshModel: true
    }
  },
  model: function(params) {
    this.store.unloadAll('file');
    return this.store.query('file', {path: params.path, nameFilter:params.filter});
  },
  setupController: function(controller, model) {
    this._super(controller, model);
    controller.set('searchText', '');
    this.get('fileSelectionService').reset();
    this.set('currentPath', controller.get('path'));
  },

  actions: {
    refreshCurrentRoute: function() {
      this.refresh();
    },
    searchAction : function(searchText) {
     this.set('controller.filter', searchText);

     this.transitionTo({
       queryParams: {
         path: this.get('currentPath'),
         filter: searchText
       }
     });

    },
    error: function(error, transition) {
      this.get('fileSelectionService').reset();
      let path = transition.queryParams.path;
      var formattedError = this.extractError(error);
      this.get('logger').danger(`Failed to transition to <strong>${path}</strong>`, formattedError);
      // Had to do this as we are unloading all files before transitioning
      this.transitionTo({
        queryParams: {
          path: this.get('currentPath')
        }
      });
    }
  }
});
