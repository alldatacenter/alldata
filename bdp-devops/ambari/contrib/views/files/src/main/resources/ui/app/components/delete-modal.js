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
import OperationModal from '../mixins/operation-modal';

export default Ember.Component.extend(OperationModal, {
  fileSelectionService: Ember.inject.service('files-selection'),
  fileOperationService: Ember.inject.service('file-operation'),
  logger: Ember.inject.service('alert-messages'),
  closeOnEscape: true,
  deletePermanently: false,
  deletePermanentlyAlways: false,
  showDeletePermanentCheckbox: true,
  selectedFiles: Ember.computed.alias('fileSelectionService.files'),
  filesCount: Ember.computed.oneWay('fileSelectionService.filesCount'),
  folderCount: Ember.computed.oneWay('fileSelectionService.folderCount'),
  hasFiles: Ember.computed('filesCount', function() {
    return this.get('filesCount') > 0;
  }),
  hasFolders: Ember.computed('folderCount', function() {
    return this.get('folderCount') > 0;
  }),
  hasError: false,
  shouldRetry: false,
  currentFailedPath: '',
  currentUnprocessedPaths: [],
  currentFailureMessage: '',
  currentServerFailureMessage: '',
  isDeleting: false,

  setTrashSettings: Ember.on('init', Ember.observer('currentPathIsTrash', function() {
    if(this.get('currentPathIsTrash')) {
      this.set('deletePermanentlyAlways', true);
      this.set('showDeletePermanentCheckbox', false);
    } else {
      this.set('deletePermanentlyAlways', false);
      this.set('showDeletePermanentCheckbox', true);
    }

  })),

  disableCloseOnEscape: Ember.observer('isDeleting', function() {
    if (this.get('isDeleting') === true) {
      this.set('closeOnEscape', false);
    } else {
      this.set('closeOnEscape', true);
    }
  }),

  deletePaths: function(paths) {
    this.set('isDeleting', true);
    let deletePermanently = this.get('deletePermanently');
    if(this.get('deletePermanentlyAlways')) {
      deletePermanently = true;
    }
    this.get('fileOperationService').deletePaths(paths, deletePermanently).then(
      (response) => {
        this.set('isDeleting', false);
        this.send('close');
        this.sendAction('refreshAction');
      }, (error) => {
        this.set('isDeleting', false);
        if (error.unprocessable === true) {
          this.set('hasError', true);
          this.set('currentFailedPath', error.failed);
          this.set('currentServerFailureMessage', error.message);
          this.set('currentFailureMessage', `Failed to delete <strong>${error.failed}</strong>.`);
          this.set('shouldRetry', error.retry);
          this.set('currentUnprocessedPaths', error.unprocessed);
        } else {
          this.set('isDeleting', false);
          this.get('logger').danger("Failed to delete files and folders.", error);
          this.send('close');
        }
      });
  },
  reset: function() {
    this.set('deletePermanently', false);
    this.set('hasError', false);
    this.set('shouldRetry', false);
    this.set('isDeleting', false);
    this.set('currentFailedPath', '');
    this.set('currentFailureMessage', '');
    this.set('currentUnprocessedPaths', '');
  },
  actions: {
    didOpenModal: function() {
      this.reset();
      console.log("Delete modal opened");
    },

    didCloseModal: function() {
      console.log("Delete Modal closed");
    },
    delete: function() {
      var currentPathsToDelete = this.get('selectedFiles').map((entry) => { return entry.get('path');});
      this.deletePaths(currentPathsToDelete);
    },
    retryError: function() {
      var newPaths = [this.get('currentFailedPath')];
      if (Ember.isArray(this.get('currentUnprocessedPaths'))) {
        newPaths.pushObjects(this.get('currentUnprocessedPaths'));
      }
      this.deletePaths(newPaths);
    },
    skipAndRetry: function() {
      this.deletePaths(this.get('currentUnprocessedPaths'));
    },
    skipAll: function() {
      this.send('close');
      this.sendAction('refreshAction');
    }
  }
});
