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
  logger: Ember.inject.service('alert-messages'),
  chmod: function (path, permission) {
    var adapter = this.get('store').adapterFor('file');
    var data = {mode: permission, path: path};
    return new Ember.RSVP.Promise((resolve, reject) => {
      adapter.ajax(this._getFileOperationUrl('chmod'), "POST", {data: data}).then(
        (response) => {
          this.get('logger').success(`Successfully changed permission of ${path}`, {}, {flashOnly: false});
          return resolve(response);
        }, (responseError) => {
          var error = this.extractError(responseError);
          this.get('logger').danger(`Failed to modify permission of ${path}`, error);
          return reject(error);
        });
    });
  },

  createNewFolder: function (srcPath, folderName) {
    var path = (srcPath === '/') ? '' : srcPath;

    if (folderName.slice(0, 1) === '/') {
      folderName = folderName.slice(0, folderName.length);
    }
    var adapter = this.get('store').adapterFor('file');
    var data = {path: `${path}/${folderName}`};
    return new Ember.RSVP.Promise((resolve, reject) => {
      adapter.ajax(this._getFileOperationUrl('mkdir'), "PUT", {data: data}).then(
        (response) => {
          this.get('logger').success(`Successfully created <strong>${path}/${folderName}`, {flashOnly: true});
          return resolve(response);
        }, (responseError) => {
          var error = this.extractError(responseError);
          this.get('logger').danger(`Failed to create ${path}/${folderName}`, error);
          return reject(error);
        });
    });
  },

  deletePaths: function (paths, deletePermanently = false) {
    var opsUrl;
    if (deletePermanently) {
      opsUrl = this._getFileOperationUrl('remove');
    } else {
      opsUrl = this._getFileOperationUrl('moveToTrash');
    }
    var data = {
      paths: paths.map((path) => {
        return {path: path, recursive: true};
      })
    };
    var adapter = this.get('store').adapterFor('file');
    return new Ember.RSVP.Promise((resolve, reject) => {
      adapter.ajax(opsUrl, "POST", {data: data}).then(
        (response) => {
          return resolve(response);
        }, (rejectResponse) => {
          var error = this.extractError(rejectResponse);
          if (this.isInvalidError(error)) {
            return reject(this._prepareUnprocessableErrorResponse(error));
          } else {
            return reject(Ember.merge({retry: false, unprocessable: false}, error));
          }
        });
    });
  },

  listPath: function (queryPath, onlyDirectory = true) {
    let baseUrl = this._getFileOperationUrl('listdir');
    let url = `${baseUrl}?${queryPath}`;
    var adapter = this.get('store').adapterFor('file');
    return new Ember.RSVP.Promise((resolve, reject) => {
      adapter.ajax(url, "GET").then(
        (response) => {
          if (onlyDirectory) {
            return resolve(response.files.filter((entry) => {
              return entry.isDirectory;
            }));
          } else {
            return resolve(response.files);
          }
        }, (responseError) => {
          var error = this.extractError(responseError);
          return reject(error);
        });
    });
  },

  movePaths: function (srcPath, destName) {
    return new Ember.RSVP.Promise((resolve, reject) => {
      var adapter = this.get('store').adapterFor('file');
      var baseURL = adapter.buildURL('file');
      var moveUrl = baseURL.substring(0, baseURL.lastIndexOf('/')) + "/move";
      var data = {sourcePaths: srcPath, destinationPath: destName};
      adapter.ajax(moveUrl, "POST", {data: data}).then((response) => {
        this.get('logger').success(`Successfully moved to ${destName}.`, {}, {flashOnly: true});
        resolve(response);
      }, (rejectResponse) => {
        var error = this.extractError(rejectResponse);
        if (this.isInvalidError(error)) {
          return reject(this._prepareUnprocessableErrorResponse(error));
        } else {
          return reject(Ember.merge({retry: false, unprocessable: false}, error));
        }
      });
    });
  },

  copyPaths: function (srcPath, destName) {
    return new Ember.RSVP.Promise((resolve, reject) => {
      var adapter = this.get('store').adapterFor('file');
      var baseURL = adapter.buildURL('file');
      var moveUrl = baseURL.substring(0, baseURL.lastIndexOf('/')) + "/copy";
      var data = {sourcePaths: srcPath, destinationPath: destName};
      adapter.ajax(moveUrl, "POST", {data: data}).then((response) => {
        this.get('logger').success(`Successfully copied to ${destName}.`, {}, {flashOnly: true});
        resolve(response);
      }, (rejectResponse) => {
        var error = this.extractError(rejectResponse);
        if (this.isInvalidError(error)) {
          return reject(this._prepareUnprocessableErrorResponse(error));
        } else {
          return reject(Ember.merge({retry: false, unprocessable: false}, error));
        }
      });
    });
  },

  _checkIfDeleteRetryIsRequired: function (error) {
    return error.unprocessed.length >= 1;
  },

  _prepareUnprocessableErrorResponse: function (error) {
    var response = {};
    response.unprocessable = true;
    if (this._checkIfDeleteRetryIsRequired(error)) {
      response.retry = true;
      response.failed = error.failed[0];
      response.message = error.message;
      response.unprocessed = error.unprocessed;
    } else {
      response.retry = false;
      response.failed = error.failed[0];
      response.message = error.message;
    }

    return response;
  },

  getHome: function () {
    var adapter = this.get('store').adapterFor('file');
    return adapter.ajax(this._getMiscUrl("/user/home"), "GET");
  },

  getTrash: function () {
    var adapter = this.get('store').adapterFor('file');
    return adapter.ajax(this._getMiscUrl("/user/trashDir"), "GET");
  },

  _getMiscUrl: function (segment) {
    var urlFragments = this._getBaseURLFragments();
    return urlFragments.slice(0, urlFragments.length - 2).join('/') + segment;
  },

  getUploadUrl: function () {
    return this._getMiscUrl("/upload");
  },

  _getFileOperationUrl: function (pathFragment) {
    var adapter = this.get('store').adapterFor('file');
    var baseURL = adapter.buildURL('file');
    return baseURL.substring(0, baseURL.lastIndexOf('/')) + `/${pathFragment}`;
  },

  isExistsInCurrentPath: function (name) {
    return this.get('store').peekAll('file').isAny('name', name);
  }
});
