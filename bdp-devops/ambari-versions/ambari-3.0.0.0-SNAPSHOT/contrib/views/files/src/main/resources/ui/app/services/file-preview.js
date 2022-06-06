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

export default Ember.Service.extend(FileOperationMixin, {
  fileSelectionService: Ember.inject.service('files-selection'),
  logger: Ember.inject.service('alert-messages'),
  selectedFiles: Ember.computed.alias('fileSelectionService.files'),
  selected: Ember.computed('selectedFiles', function () {
    return this.get('selectedFiles').objectAt(0);
  }),
  selectedFilePath: Ember.computed('selected.path', function () {
    return this.get('selected.path');
  }),
  filesDownloadService: Ember.inject.service('files-download'),
  fileContent: '',
  startIndex: 0,
  offset: 5000,
  path: '',
  isLoading: false,
  fileFetchFinished: false,
  endIndex: function () {
    return this.get('startIndex') + this.get('offset');
  }.property('startIndex'),

  reset: function () {
    this.set('fileContent', '');
    this.set('startIndex', 0);
    this.set('offset', 5000);
    this.set('path', '');
    this.set('isLoading', false);
    this.set('hasError', false);
    this.set('fileFetchFinished', false);
  },

  getNextContent: function () {
    return this._getContent();
  },

  _getContent: function () {
    var _self = this;

    if (this.get('fileFetchFinished')) {
      return false;
    }

    var adapter = this.get('store').adapterFor('file');
    var baseURL = adapter.buildURL('file');
    var renameUrl = baseURL.substring(0, baseURL.lastIndexOf('/'));
    var previewUrl = renameUrl.substring(0, renameUrl.lastIndexOf('/')) + "/preview/file";
    var queryParams = Ember.$.param({
      path: this.get('selected.path'),
      start: this.get('startIndex'),
      end: this.get('endIndex')});

    var currentFetchPath = previewUrl + "?" + queryParams;
    this.set('isLoading', true);

    return new Ember.RSVP.Promise((resolve, reject) => {
      adapter.ajax(currentFetchPath, "GET").then(
        (response) => {

          _self.set('fileContent', _self.get('fileContent') + response.data);
          _self.set('fileFetchFinished', response.isFileEnd);
          _self.set('isLoading', false);
          _self.set('startIndex', (_self.get('startIndex') + _self.get('offset')));
          return resolve(response);
        }, (responseError) => {
          _self.set('isLoading', false);
          var error = this.extractError(responseError);
          this.get('logger').danger("Failed to preview file.", error);
          return reject(responseError);
        });
    });
  },

  download: function (event) {
    this.get('filesDownloadService').download();
  }

});

