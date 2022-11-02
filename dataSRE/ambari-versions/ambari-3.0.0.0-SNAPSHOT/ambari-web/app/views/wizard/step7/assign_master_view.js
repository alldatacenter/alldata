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

require('views/wizard/step5_view');

App.AssignMasterOnStep7View = App.AssignMasterComponentsView.extend({

  showTitle: false,

  alertMessage: '',

  isWizardStep: false,

  willInsertElement: function() {
    this._super();
    this.setAlertMessage();
  },

  /**
   * @method setAlertMessage
   */
  setAlertMessage: function() {
    var mastersToCreate = this.get('controller.mastersToCreate'),
        mastersToCreateDisplayName = mastersToCreate.map(function (item) {
          return App.format.role(item);
        }),
        stringText1 = mastersToCreate.length > 1 ? Em.I18n.t('common.hosts').toLowerCase() : Em.I18n.t('common.host').toLowerCase(),
        stringText2 = mastersToCreate.length > 1 ? Em.I18n.t('then') : Em.I18n.t('it'),
        alertMessage = Em.I18n.t('installer.step7.assign.master.body').format(mastersToCreateDisplayName.join(), stringText1, stringText2),
        dependentComponents = this.getDependentComponents(mastersToCreate),
        isManualKerberos = App.get('router.mainAdminKerberosController.isManualKerberos');

    if (dependentComponents.length) {
      alertMessage += '<br/>' + Em.I18n.t('installer.step7.assign.master.dependent.component.body')
                                .format(stringUtils.getFormattedStringFromArray(dependentComponents));
    }

    if (isManualKerberos) {
      var warnMessage = Em.I18n.t('common.important.strong') + ': ' + Em.I18n.t('installer.step8.kerberors.warning');
      alertMessage += '<br/>' + Em.I18n.t('common.warn.message').format(warnMessage);
    }
    this.set('alertMessage', alertMessage);
  },

  /**
   * @method getDependentComponents
   * @param {Array} mastersToCreate
   * @returns {Array}
   */
  getDependentComponents: function(mastersToCreate) {
    var dependentComponents = [];

    mastersToCreate.forEach(function(_component) {
      var dependencies = App.StackServiceComponent.find(_component).get('dependencies').filterProperty('scope', 'host').map(function (item) {
        return App.format.role(item.componentName);
      });
      dependentComponents = dependentComponents.concat(dependencies).uniq();
    }, this);
    return dependentComponents;
  }

});
