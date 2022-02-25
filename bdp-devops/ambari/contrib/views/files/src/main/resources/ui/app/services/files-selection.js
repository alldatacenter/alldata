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

export default Ember.Service.extend({
  files: [],
  lastFileSelected: null,
  filesCount: Ember.computed('files.[]', function() {
    return this.get('files').filterBy('isDirectory', false).length;
  }),
  folderCount: Ember.computed('files.[]', 'filesCount', function() {
    return this.get('files.length') - this.get('filesCount');
  }),

  selectFiles: function(files) {
    files.forEach((file) => {
      file.set('isSelected', true);
      this.get('files').pushObject(file);
      this.set('lastFileSelected', file);
    });
  },

  deselectFile: function(file) {

    if (file.get('isSelected')) {
        file.set('isSelected', false);
    }

    this.set('files', this.get('files').without(file));
    if(file === this.get('lastFileSelected')) {
      this.set('lastFileSelected', this.get('files').objectAt(this.get('files.length') - 1));
    }

  },

  deselectAll: function() {
    this.get('files').forEach((file) => {
      file.set('isSelected', false);
    });
    this.set('files', []);
    this.set('lastFileSelected');
  },

  reset: function() {
    this.set('files', []);
    this.set('lastFileSelected');
  }
});
