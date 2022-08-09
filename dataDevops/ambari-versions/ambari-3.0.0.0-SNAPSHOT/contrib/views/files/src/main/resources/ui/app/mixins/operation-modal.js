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
import { EKMixin, keyUp, EKFirstResponderOnFocusMixin } from 'ember-keyboard';

export default Ember.Mixin.create(EKMixin, EKFirstResponderOnFocusMixin, {
  modalEventBus: Ember.inject.service('modal-event-bus'),
  name: '',
  closeOnEscape: false,
  isModalOpen: false,

  setupKey: Ember.on('init', function() {
    this.set('keyboardActivated', true);
  }),

  //disableEscape:
  closeModalOnEscape: Ember.on(keyUp('Escape'), function() {
    if (this.get('closeOnEscape')) {
      this.$('.modal').modal('hide');
    }
  }),

  initModal: function() {
    Ember.defineProperty(this, 'modalGuard', Ember.computed.alias('modalEventBus.' + this.get('name')));
    this.addObserver('modalGuard', () => {
      if(this.get('modalGuard')) {
        this.set('isModalOpen', true);
        Ember.run.later(this, () => {
          this.$('.modal').modal({backdrop: 'static', keyboard: false});
          this.$('.modal').on('hide.bs.modal', () => {
            this.send('closeModal');
          });
          this.send('modalOpened');
        });

      }
    });
  }.on('didInitAttrs'),

  hideModal: Ember.on('willDestroyElement', function() {
    if (this.get('isModalOpen')) {
      this.$('.modal').modal('hide');
    }
  }),

  actions: {
    /** close by action in the UI **/
    close: function() {
      this.$('.modal').modal('hide');
    },

    closeModal: function() {
      this.$('.modal').off('hide.bs.modal');
      this.set('isModalOpen', false);
      this.get('modalEventBus').resetModal(this.get('name'));
      this.send('didCloseModal');
    },

    modalOpened: function() {
      this.send('didOpenModal');
    },

    didCloseModal: function() {},
    didOpenModal: function() {}
  }
});
