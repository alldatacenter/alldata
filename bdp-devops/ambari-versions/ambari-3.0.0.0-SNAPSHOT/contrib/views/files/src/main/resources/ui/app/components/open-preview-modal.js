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
  filePreviewService: Ember.inject.service('file-preview'),
  selectedFilePath: '',
  modalGuardChanged: Ember.observer('modalGuard', function () {
    if (this.get('modalGuard')) {
      console.log("Modal Guard set");
    } else {
      console.log("Modal Guard not set");
    }
  }),

  actions: {
    // Actions to preview modal HTML.
    didOpenModal: function () {
      var _self = this;
      this.set('selectedFilePath', this.get('filePreviewService.selectedFilePath'));
      this.get('filePreviewService').getNextContent().then(function () {
        _self.$('.preview-content').on('scroll', function () {
          if (Ember.$(this).scrollTop() + Ember.$(this).innerHeight() >= this.scrollHeight) {
            _self.get('filePreviewService').getNextContent();
          }
        });
      }, function () {
        // close modal here.
        _self.send('close');
        _self.sendAction('refreshAction');
      });
    },
    didCloseModal: function () {
      this.$('.preview-content').off('scroll');
      this.get('filePreviewService').reset();
    },
    download: function () {
      this.get('filePreviewService').download();
    }
  }

});
