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
var stringUtils = require('utils/string_utils');

App.WizardStep5View = App.AssignMasterComponentsView.extend({

  isReassignWizard: Em.computed.equal('controller.content.controllerName', 'reassignMasterController'),

  title: Em.computed.ifThenElse('isReassignWizard', Em.I18n.t('installer.step5.reassign.header'), Em.I18n.t('installer.step5.header')),

  alertMessage: function () {
    var result = Em.I18n.t('installer.step5.body');
    result += this.get('coHostedComponentText') ? '\n' + this.get('coHostedComponentText') : '';
    return result;
  }.property('coHostedComponentText'),

  coHostedComponentText: '',

  didInsertElement: function () {
    this._super();
    this.setCoHostedComponentText();
  },

  setCoHostedComponentText: function () {
    var coHostedComponents = App.StackServiceComponent.find().filterProperty('isOtherComponentCoHosted').filterProperty('stackService.isSelected');
    var coHostedComponentsText = '';
    if (!this.get('controller').get('isReassignWizard')) {
      coHostedComponents.forEach(function (serviceComponent, index) {
        var coHostedComponentsDisplayNames = serviceComponent.get('coHostedComponents').map(function (item) {
          return App.StackServiceComponent.find().findProperty('componentName', item).get('displayName');
        });
        var componentTextArr = [serviceComponent.get('displayName')].concat(coHostedComponentsDisplayNames);
        coHostedComponents[index] = stringUtils.getFormattedStringFromArray(componentTextArr);
        coHostedComponentsText += '<br/>' + Em.I18n.t('installer.step5.body.coHostedComponents').format(coHostedComponents[index]);
      }, this);
    }

    this.set('coHostedComponentText', coHostedComponentsText);
  }

});
