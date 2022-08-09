/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.ControlsView = Ember.View.extend({

	classNames: ['display-inline-block', 'config-controls'],

	templateName: require('templates/common/configs/controls'),

	serviceConfigProperty: null,

	showActions: function() {
		return App.isAuthorized('SERVICE.MODIFY_CONFIGS') && this.get('serviceConfigProperty.isRequiredByAgent') && !this.get('serviceConfigProperty.isComparison');
	}.property('serviceConfigProperty.isEditable', 'serviceConfigProperty.isRequiredByAgent', 'serviceConfigProperty.isComparison'),

	showSwitchToGroup: Em.computed.and('!serviceConfigProperty.isEditable', 'serviceConfigProperty.group'),

	showIsFinal: Em.computed.and('serviceConfigProperty.supportsFinal', '!serviceConfigProperty.isUndefinedLabel'),

	showRemove: Em.computed.and('showActions', 'serviceConfigProperty.isEditable', 'serviceConfigProperty.isRemovable'),

	showOverride: Em.computed.and('showActions', 'serviceConfigProperty.isPropertyOverridable', 'controller.canEdit'),

	showUndo: Em.computed.and('showActions', 'serviceConfigProperty.isEditable', '!serviceConfigProperty.cantBeUndone', 'serviceConfigProperty.isNotDefaultValue'),

	showSetRecommended: Em.computed.and('showActions', 'serviceConfigProperty.isEditable', 'serviceConfigProperty.recommendedValueExists')

});

