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

var App = require('app');

App.DropdownButtonsComponent = Em.Component.extend(App.ClickElsewhereMixin,{
  tagName:'li',
  restartConfirming:false,
  onClickElsewhere:function () {
    this.set('restartConfirming',false);
    this.$().parents('.dropdown-menu').parent().removeClass('open');
  },
  dropdownHideControl:function () {
    this.$().parents('.dropdown-menu').parent().on(
      "hide.bs.dropdown", function() {
        return !this.get('restartConfirming');
      }.bind(this));
  }.on('didInsertElement'),
  button:Em.Component.extend({
    tagName:'a',
    click:function (event) {
      event.stopPropagation();
      this.triggerAction({
        action: 'showRestartConfirmation',
        target: this.get('parentView'),
        actionContext: this.get('context')
      });
    }
  }),
  actions:{
    showRestartConfirmation: function() {
      this.toggleProperty('restartConfirming');
    },
    confirm: function (arg) {
      this.set('restartConfirming',false);
      this.sendAction('action',arg);
    }
  }
})
