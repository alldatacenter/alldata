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

App.ServiceConfigLayoutTabCompareView = App.ServiceConfigLayoutTabView.extend({
  templateName: require('templates/common/configs/service_config_layout_tab_compare'),

  canEdit: false,
  primaryCompareVersion: null,
  secondaryCompareVersion: null,

  onToggleBlock: function(event) {
    event.context.toggleProperty('isCollapsed');
    $(event.currentTarget).siblings('.panel-body').slideToggle(500);
  },

  didInsertElement: function() {
    this._super();
    this.get('content.sectionRows').forEach(function(row) {
      row.setEach('isCollapsed', false);
      row.setEach('isCategoryBodyVisible', Em.computed.ifThenElse('isCollapsed', 'display: none;', 'display: block;'));
    });
  }

});
