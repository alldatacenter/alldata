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
  modalEventBus: Ember.inject.service('modal-event-bus'),
  fileOperationService: Ember.inject.service('file-operation'),
  closeOnEscape: true,
  tagName: 'span',
  name: 'ctx-new-directory',
  hasError: false,
  errorMessage: '',
  folderName: '',
  didInitAttrs: function() {
    this.get('modalEventBus').registerModal("ctx-new-directory");
  },
  willDestroyElement() {
    this.get('modalEventBus').resetModal("ctx-new-directory");
  },
  resetError: Ember.observer('folderName', function() {
    this.set('hasError', false);
    this.set('errorMessage', '');
  }),
  setError: function(message) {
    this.set('hasError', true);
    this.set('errorMessage', message);
  },
  validateFolderName: function(folderName) {
    if(Ember.isBlank(folderName)) {
      this.setError('Cannot be empty');
      return false;
    }

    if(this.get('fileOperationService').isExistsInCurrentPath(folderName)) {
      this.setError('Name already exists');
      return false;
    }

    if(folderName.length > 255) {
      this.setError(`Max limit for length of folder name is 255. Length: ${folderName.length}`);
      return false;
    }

    return true;
  },

  actions: {
    didOpenModal: function() {
      this.set('folderName');
      Ember.run.later(() => {
        this.$('input').focus();
      }, 500);
    },
    create: function() {

      if(!this.validateFolderName(this.get('folderName'))) {
        return false;
      }

      this.get('fileOperationService').createNewFolder(this.get('path'), this.get('folderName')).then(
        (response) => {
          this.send('close');
          this.sendAction('refreshAction');
        }, (error) => {
          this.send('close');
        });
    },
    openModal : function() {
      this.get('modalEventBus').showModal('ctx-new-directory');
    }
  }
});
