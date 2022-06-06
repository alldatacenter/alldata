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

App.DependentConfigsTableView = Em.View.extend({
  templateName: require('templates/common/modal_popups/dependent_configs_table'),
  recommendations: [],
  isClickable: false,
  showPopovers: true,
  elementsWithPopover: function () {
    return this.$('td.config-dependency-name');
  }.property(),
  hideMessage: Em.computed.alias('controller.isInstallWizard'),
  updateRecommendedDefault: function () {
    if (this.get('controller.isInstallWizard')) {
      var applyRecommendations = this.get('recommendations').filterProperty('saveRecommended');
      var dontApplyRecommendations = this.get('recommendations').filterProperty('saveRecommended', false);
      this.get('controller').undoRedoRecommended(applyRecommendations, true);
      this.get('controller').undoRedoRecommended(dontApplyRecommendations, false);
    }
  }.observes('recommendations.@each.saveRecommended'),
  isEditable: true,
  title: Em.computed.ifThenElse('isEditable', Em.I18n.t('popup.dependent.configs.table.recommended'), Em.I18n.t('popup.dependent.configs.table.required')),
  message: function () {
    var message = '';
    if (this.get('isEditable')) {
      if (this.get('parentView.isAfterRecommendation')) {
        message += Em.I18n.t('popup.dependent.configs.title.recommendation') + '<br>';
      }
      message += Em.I18n.t('popup.dependent.configs.title.values');
    } else {
      message += Em.I18n.t('popup.dependent.configs.title.required');
    }
    return message;
  }.property('isEditable'),
  didInsertElement: function () {
    if (this.get('showPopovers')) {
      App.popover(this.get('elementsWithPopover'), {
        placement: 'auto right',
        trigger: 'hover',
        html: true
      });
    }
  },
  willDestroyElement: function () {
    if (this.get('showPopovers')) {
      this.get('elementsWithPopover').popover('destroy');
    }
  }
});

App.DependentConfigsListView = Em.View.extend({
  templateName: require('templates/common/modal_popups/dependent_configs_list'),
  isAfterRecommendation: true,
  isRecommendationsClickable: false,
  showRecommendationsPopovers: true,
  recommendations: [],
  requiredChanges: [],
  allConfigsWithErrors: [],
  toggleAllId: '',
  toggleAll: App.CheckboxView.extend({
    didInsertElement: function () {
      this.set('parentView.toggleAllId', this.get('elementId'));
      this.updateCheckbox();
    },
    click: function () {
      Em.run.next(this, 'updateSaveRecommended');
    },
    updateCheckboxObserver: function () {
      Em.run.once(this, 'updateCheckbox');
    }.observes('parentView.recommendations.@each.saveRecommended'),

    updateCheckbox: function() {
      this.set('checked', !(this.get('parentView.recommendations') || []).someProperty('saveRecommended', false));
    },
    updateSaveRecommended: function() {
      this.get('parentView.recommendations').setEach('saveRecommended', this.get('checked'));
    }
  }),
  setAllConfigsWithErrors: function () {
    if (this.get('state') === 'inBuffer' || Em.isNone(this.get('controller.stepConfigs'))) {
      return false;
    }
    this.set('allConfigsWithErrors', this.get('controller.stepConfigs').reduce(function (result, stepConfig) {
      if (stepConfig.get('configsWithErrors.length')) {
        result = result.concat(stepConfig.get('configsWithErrors'));
      }
      return result;
    }, []));
  }.observes('controller.stepConfigs.@each.configsWithErrors'),
  didInsertElement: function () {
    $('span.dropdown-toggle').dropdown();
    this.setAllConfigsWithErrors();
    this._super();
  }
});

/**
 * Show confirmation popup
 * @param {[Object]} recommendations
 * @param {[Object]} requiredChanges
 * @param {function} [primary=null]
 * @param {function} [secondary=null]
 * @param {boolean} [isRecommendationsClickable=false]
 * @param {boolean} [isRecommendationsClickable=true]
 * we use this parameter to defer saving configs before we make some decisions.
 * @return {App.ModalPopup}
 */
App.showDependentConfigsPopup = function (recommendations, requiredChanges, primary, secondary, controller, isRecommendationsClickable = false, showRecommendationsPopovers = true) {
  return App.ModalPopup.show({
    encodeBody: false,
    header: Em.I18n.t('popup.dependent.configs.header'),
    classNames: ['common-modal-wrapper'],
    modalDialogClasses: ['modal-xlg'],
    secondaryClass: 'cancel-button',
    bodyClass: App.DependentConfigsListView.extend({
      recommendations,
      requiredChanges,
      controller,
      isRecommendationsClickable,
      showRecommendationsPopovers
    }),
    saveChanges: function() {
      recommendations.forEach(function (c) {
        Em.set(c, 'saveRecommendedDefault', Em.get(c, 'saveRecommended'));
      });
    },
    discardChanges: function() {
      recommendations.forEach(function(c) {
        Em.set(c, 'saveRecommended', Em.get(c, 'saveRecommendedDefault'));
      });
    },
    onPrimary: function () {
      this._super();
      if (primary) primary();
      this.saveChanges();
    },
    onSecondary: function() {
      this._super();
      if (secondary) secondary();
      this.discardChanges();
    },
    onClose: function () {
      this.onSecondary();
    }
  });
};
