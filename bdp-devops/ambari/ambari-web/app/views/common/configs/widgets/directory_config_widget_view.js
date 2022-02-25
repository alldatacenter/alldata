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

require('views/common/controls_view');

var App = require('app');

App.DirectoryConfigWidgetView = App.ConfigWidgetView.extend({
  templateName: require('templates/common/configs/widgets/directory_config_widget'),
  classNames: ['widget-config', 'directory-widget'],

  disabled: Em.computed.not('config.isEditable'),

  /**
   * Control to edit value.
   *
   * @type {App.ServiceConfigTextArea}
   * @property configView
   */
  configView: App.ServiceConfigTextArea.extend({
    isPopoverEnabled: 'false',
    widthClass: 'col-md-12',
    serviceConfigBinding: 'parentView.config',
    popoverPlacement: 'top'
  }),

  didInsertElement: function() {
    this.initPopover();
    this._super();
    this.set('config.displayType', this.get('config.stackConfigProperty.widget.type'));
  }

});
