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
  closeOnEscape: true,
  fileSelectionService: Ember.inject.service('files-selection'),
  fileOperationService: Ember.inject.service('file-operation'),
  selectedFiles: Ember.computed.alias('fileSelectionService.files'),
  selected: Ember.computed('selectedFiles', function () {
    return this.get('selectedFiles').objectAt(0);
  }),
  selectionName: '/',
  isUpdating: false,
  browseError: false,
  browseErrorMessege: '',
  hasError: false,
  shouldRetry: false,
  currentFailedPath: '',
  currentUnprocessedPaths: [],
  currentFailureMessage: '',

  movePaths: function (paths, destination) {
    this.set('isUpdating', true);

    this.get('fileOperationService').movePaths(paths, destination).then(
      (response) => {
        this.set('isUpdating', false);
        this.send('close');
        this.sendAction('refreshAction');
      }, (error) => {
        this.set('isUpdating', false);
        if (error.unprocessable === true) {
          this.set('hasError', true);
          this.set('currentFailedPath', error.failed);
          this.set('currentFailureMessage', error.message);
          this.set('shouldRetry', error.retry);
          this.set('currentUnprocessedPaths', error.unprocessed);
        } else {
          this.set('isUpdating', false);
          this.get('logger').danger("Failed to delete files and folders.", error);
          this.send('close');
        }
      });
  },
  reset: function () {
    this.set('browseError', false);
    this.set('browseErrorMessege', '');
    this.set('selectionName', '/');
    this.set('hasError', false);
    this.set('shouldRetry', false);
    this.set('isUpdating', false);
    this.set('currentFailedPath', '');
    this.set('currentFailureMessage', '');
    this.set('currentUnprocessedPaths', '');
  },
  actions: {

    didOpenModal: function () {
      this.reset();
      console.log("Move modal opened");
    },

    didCloseModal: function () {
      console.log("Move Modal did close.");
    },

    move: function () {
      var currentPathsToMove = this.get('selectedFiles').map((entry) => {
        return entry.get('path')
      });
      var destinationPath = (this.get('selectionName') !== '') ? this.get('selectionName') : '/';
      this.movePaths(currentPathsToMove, destinationPath);
    },

    retryError: function () {
      var newPaths = [this.get('currentFailedPath')];
      if (Ember.isArray(this.get('currentUnprocessedPaths'))) {
        newPaths.pushObjects(this.get('currentUnprocessedPaths'));
      }
      var destinationPath = (this.get('selectionName') !== '') ? this.get('selectionName') : '/';
      this.movePaths(newPaths, destinationPath);
    },

    skipAndRetry: function () {
      var destinationPath = (this.get('selectionName') !== '') ? this.get('selectionName') : '/';
      this.movePaths(this.get('currentUnprocessedPaths'), destinationPath);
    },

    skipAll: function () {
      this.send('close');
      this.sendAction('refreshAction');
    },

    pathSelected: function (path) {
      console.log(path);
      this.set('selectionName', path);
      this.set('browseError', false);

    },

    browseError: function (error) {
      this.set('browseError', true);
      this.set('browseErrorMessage', error.message);
    }
  }


});
