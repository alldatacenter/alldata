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
import columnConfig from '../config/files-columns';

export default Ember.Controller.extend({
  logger: Ember.inject.service('alert-messages'),
  fileSelectionService: Ember.inject.service('files-selection'),
  lastSelectedFile: Ember.computed.oneWay('fileSelectionService.lastFileSelected'),
  selectedFilesCount: Ember.computed.oneWay('fileSelectionService.filesCount'),
  selectedFolderCount: Ember.computed.oneWay('fileSelectionService.folderCount'),
  isSelected: Ember.computed('selectedFilesCount', 'selectedFolderCount', function() {
    return (this.get('selectedFilesCount') + this.get('selectedFolderCount')) !== 0;
  }),
  queryParams: ['path', 'filter'],
  path: '/',
  filter: '',
  columns: columnConfig,

  currentMessagesCount: Ember.computed.alias('logger.currentMessagesCount'),

  hasHomePath: false,
  hasTrashPath: false,

  currentPathIsTrash: Ember.computed('path', 'trashPath', 'hasTrashPath', function() {
    return this.get('hasTrashPath') && (this.get('path') === this.get('trashPath'));
  }),

  // This is required as the validSearchText will be debounced and will not be
  // called at each change of searchText. searchText is required so that sub
  // components(file search componenet) UI can be cleared from outside.(i.e, from
  // the afterModel of the route when the route changes)
  searchText: '',
  validSearchText: '',

  sortProperty: [],
  sortEnabled: Ember.computed('fileSelectionService.files.length', function() {
    return this.get('fileSelectionService.files.length') === 0;
  }),

  allSelected: Ember.computed('fileSelectionService.files.length', function() {
    return this.get('fileSelectionService.files.length') !== 0 && this.get('fileSelectionService.files.length') === this.get('model.length');
  }),

  parentPath: Ember.computed('path', function() {
    var path = this.get('path');
    var parentPath = path.substring(0, path.lastIndexOf('/'));
    if(Ember.isBlank(parentPath)) {
      parentPath = '/';
    }

    if(path === '/') {
      parentPath = '';
    }
    return parentPath;
  }),

  arrangedContent:  Ember.computed.sort('model', 'sortProperty'),

  metaInfo: Ember.computed('model', function() {
    return this.get('model.meta');
  }),

  selectedFilePathsText: function () {
    var entities = this.get('fileSelectionService.files');
    var multiplePaths = [];

    if (entities.length === 0) {
      return this.get('path');
    } else {
      multiplePaths = entities.map((entity) => {
        return entity.get('path');
      });
      return multiplePaths.join(', ');
    }
  }.property('fileSelectionService.files.[]', 'path'),

  actions: {
    sortFiles: function(sortColumn) {
      if (sortColumn['sortOrder'] !== 0) {
        var sortProperty = sortColumn['key'] + ':' + this._getSortOrderString(sortColumn);
        this.set('sortProperty', [sortProperty]);
      } else {
        this.set('sortProperty', []);
      }
    },

    searchFiles: function(searchText) {
      this.set('validSearchText', searchText);
    },

    /* Selects a single file. Clears previous selection */
    selectSingle: function(file) {
      this.get('fileSelectionService').deselectAll();
      this.get('fileSelectionService').selectFiles([file]);
    },

    /*
      Selects file without clearing the previous selection. If sticky is true
      then shiftkey was pressed while clicking and we should select all the
      files in between
    */
    selectMultiple: function(file, sticky) {
      if(!sticky) {
        if(file.get('isSelected')) {
          this.get('fileSelectionService').deselectFile(file);
        } else {
          this.get('fileSelectionService').selectFiles([file]);
        }
      } else {
        var lastFileSelected = this.get('fileSelectionService.lastFileSelected');
        var indexRange = this._getIndexRangeBetweenfiles(lastFileSelected, file);
        if(indexRange[0] === indexRange[1]) {
          return false;
        }
        var filesInRange = this._getFilesInRange(indexRange[0], indexRange[1]);
        this.get('fileSelectionService').deselectAll();
        this.get('fileSelectionService').selectFiles(filesInRange);
      }
    },

    selectAll: function(selectStatus) {
      this.get('fileSelectionService').deselectAll();
      if(selectStatus === false) {
        this.get('fileSelectionService').selectFiles(this.get('arrangedContent'));
      }
    },

    /* Deselects the current selections */
    deselectAll: function() {
      this.get('fileSelectionService').deselectAll();
    },

    //Context Menu actions
    openFolder: function(path) {
      this.transitionToRoute({queryParams: {path: path, filter:''}});
    }
  },

  _getIndexRangeBetweenfiles: function(startFile, endFile) {
    var startIndex = this.get('arrangedContent').indexOf(startFile);
    var endIndex = this.get('arrangedContent').indexOf(endFile);
    if (startIndex < endIndex) {
      return [startIndex, endIndex];
    } else {
      return [endIndex, startIndex];
    }
  },

  _getFilesInRange: function(startIndex, endIndex) {
    var range = Array.apply(null, Array(endIndex - startIndex + 1)).map(function (_, i) {return startIndex + i;});
    return this.get('arrangedContent').objectsAt(range);
  },

  _getSortOrderString: function(column) {
    if (column['sortOrder'] === -1) {
      return 'desc';
    } else if (column['sortOrder'] === 1) {
      return 'asc';
    } else {
      return '';
    }
  }
});
