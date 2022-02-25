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
  hasError: false,
  errorMessage: '',
  isUpdating: false,
  renameService: Ember.inject.service('file-rename'),
  fileSelectionService: Ember.inject.service('files-selection'),
  selectedFiles: Ember.computed.alias('fileSelectionService.files'),
  selected: Ember.computed('selectedFiles', function() {
    return this.get('selectedFiles').objectAt(0);
  }),
  selectionName: Ember.computed.oneWay('selected.name'),
  hasErrorReset: Ember.observer('selectionName', 'selected.name', function() {
    if (this.get('hasError') && (this.get('selectionName') !== this.get('selected.name'))) {
      this.set('hasError', false);
    }
  }),

  validateErrors: function() {
    let suggestedName = this.get('selectionName');
    if (Ember.isBlank(suggestedName)) {
      this.set('hasError', true);
      this.set('errorMessage', 'Name cannot be blank');
      return false;
    }

    if (this.get('selected.name') === suggestedName) {
      this.set('hasError', true);
      this.set('errorMessage', 'Name should be different');
      return false;
    }

    if (suggestedName.length > 255) {
      this.set('hasError', true);
      this.set('errorMessage', `Max limit for length of file name is 255. Length: ${suggestedName.length}`);
      return false;
    }

    return true;
  },

  actions: {
    didOpenModal: function() {
      this.set('selectionName', this.get('selected.name'));
      // This was required as the DOM may not be visible due to animation in bootstrap modal
      Ember.run.later(() => {
        this.$('input').focus();
      }, 500);

    },

    rename: function() {
      if(!this.validateErrors()) {
        return false;
      }

      this.set('isUpdating', true);
      this.get('renameService').rename(this.get('selected.path'), this.get('selectionName'))
      .then((response) => {
        this.set('isUpdating', false);
        this.send('close');
        this.sendAction('refreshAction');
      }, (error) => {
        this.set('isUpdating', false);
        if(error.retry) {
          this.set('hasError', true);
          this.set('errorMessage', error.message);
        } else {
          this.send('close');
        }
      });
    }
  }
});
