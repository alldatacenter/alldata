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

  download: function() {
    var entries = this.get('fileSelectionService.files');
    if(entries.length === 0) {
      return this._downloadEmptyError();
    } else if(entries.length === 1) {
      return this._downloadSingle(entries);
    } else {
      return this._downloadMulti(entries);
    }
  },

  concatenate: function() {
    var entries = this.get('fileSelectionService.files');
    if(entries.length === 0 || entries.length === 1) {
      return this._concatenateNotPossibleError();
    } else {
      return this._concatenateFiles(entries);
    }
  },

  checkIfFileHasReadPermission: function(path) {
    var adapter = this.get('store').adapterFor('file');
    var data = {checkperm: true, path: path};
    return adapter.ajax(this._getDownloadBrowseUrl(), "GET", {data: data});
  },

  _downloadSingle: function(entries) {
    var entry = entries[0];
    if(entry.get('isDirectory')) {
      // There is no difference between downloading a single directory
      // or multiple directories and file.
      return this._downloadMulti(entries);
    }

    return new Ember.RSVP.Promise((resolve, reject) => {
      this.checkIfFileHasReadPermission(entry.get('path')).then(
        (response) => {
          if(response.allowed) {
            window.location.href = this._getDownloadUrl(entry.get('path'));
            resolve();
          }
        }, (rejectResponse) => {
          var error = this.extractError(rejectResponse);
          this.get('logger').danger("Failed to download file.", error);
          reject(error);
        });
    });
  },

  _downloadMulti: function(entries) {
    var entryPaths = entries.map((entry) => {
      return entry.get('path');
    });
    var data = {download: true, entries: entryPaths};
    var adapter = this.get('store').adapterFor('file');
    return new Ember.RSVP.Promise((resolve, reject) => {
      adapter.ajax(this._getDownloadGenLinkUrl(), "POST", {data: data}).then(
        (response) => {
          var downloadZipLink = this._getDownloadZipUrl(response.requestId);
          window.location.href = downloadZipLink;
          resolve();
        }, (rejectResponse) => {
          //TODO: Need to do alerts and logging.
          var error = this.extractError(rejectResponse);
          this.get('logger').danger("Failed to download Zip.", error);
          reject(error);
        });
    });
  },

  _concatenateFiles: function(entries) {
    var entryPaths = entries.map((entry) => {
      return entry.get('path');
    });

    var data = {download: true, entries: entryPaths};
    var adapter = this.get('store').adapterFor('file');
    return new Ember.RSVP.Promise((resolve, reject) => {
      adapter.ajax(this._getConcatGenLinkUrl(), "POST", {data: data}).then(
        (response) => {
          var downloadConcatLink = this._getDownloadConcatUrl(response.requestId);
          window.location.href = downloadConcatLink;
          resolve();
        }, (rejectResponse) => {
          //TODO: Need to do alerts and logging.
          var error = this.extractError(rejectResponse);
          this.get('logger').danger("Failed to concatenate files.", error);
          reject(error);
        });
    });
  },

  _downloadEmptyError: function() {
    return new Ember.RSVP.Promise(function(resolve, reject) {
      reject("No files to download.");
    });
  },
  _concatenateNotPossibleError: function() {
    return new Ember.RSVP.Promise(function(resolve, reject) {
      reject("Cannot concatenate zero or single file.");
    });
  },

  _getDownloadGenLinkUrl: function() {
    var urlFragments = this._getBaseURLFragments();
    return urlFragments.slice(0, urlFragments.length - 2).join('/') + "/download/zip/generate-link";
  },

  _getDownloadZipUrl: function(requestId) {
    var genLinkUrl = this._getDownloadGenLinkUrl();
    return genLinkUrl.substring(0, genLinkUrl.lastIndexOf('/')) + "?requestId=" + requestId;
  },

  _getDownloadBrowseUrl: function() {
    var urlFragments = this._getBaseURLFragments();
    return urlFragments.slice(0, urlFragments.length - 2).join('/') + "/download/browse";
  },

  _getDownloadUrl: function(path) {
    let params = Ember.$.param({path: path, download: true});
    return this._getDownloadBrowseUrl() + "?" + params;
  },

  _getConcatGenLinkUrl: function() {
    var urlFragments = this._getBaseURLFragments();
    return urlFragments.slice(0, urlFragments.length - 2).join('/') + "/download/concat/generate-link";
  },

  _getDownloadConcatUrl: function(requestId) {
    var genLinkUrl = this._getConcatGenLinkUrl();
    return genLinkUrl.substring(0, genLinkUrl.lastIndexOf('/')) + "?requestId=" + requestId;
  },

  _logError: function(message, error) {
    this.get('logger').danger(message, error);
  }

});
