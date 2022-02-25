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

var validator = require('utils/validator');

App.WidgetWizardStep3Controller = Em.Controller.extend({
  name: "widgetWizardStep3Controller",

  isEditController: Em.computed.equal('content.controllerName', 'widgetEditController'),

  /**
   * @type {string}
   */
  widgetName: '',

  /**
   * @type {boolean}
   */
  isNameInvalid: false,

  /**
   * @type {string}
   */
  widgetAuthor: '',

  /**
   * @type {string}
   */
  widgetNameErrorMessage: '',

  /**
   * @type {string}
   */
  descriptionErrorMessage: '',

  /**
   * @type {boolean}
   */
  isSharedChecked: false,

  /**
   * @type {boolean}
   */
  isSharedCheckboxDisabled: false,

  /**
   * @type {string}
   */
  widgetScope: Em.computed.ifThenElse('isSharedChecked', 'Cluster', 'User'),

  /**
   * @type {string}
   */
  widgetDescription: '',

  /**
   * @type {boolean}
   */
  isDescriptionInvalid: false,

  /**
   * actual values of properties in API format
   * @type {object}
   */
  widgetProperties: {},

  /**
   * @type {Array}
   */
  widgetValues: [],

  /**
   * @type {Array}
   */
  widgetMetrics: [],

  /**
   * @type {bool}
   */
  widgetNameEmpty: function () {
    return this.get('widgetName') ? !Boolean(this.get('widgetName').trim()) : true;
  }.property('widgetName'),

  /**
   * @type {boolean}
   */
  isSubmitDisabled: Em.computed.or('widgetNameEmpty', 'isNameInvalid', 'isDescriptionInvalid'),

  /**
   * validates the name on 3rd step
   */
  validateName: function(){
    var errorMessage='';
    var widgetName = this.get('widgetName');
    this.set("isNameInvalid",false);
    if(widgetName && widgetName.length > 128){
      errorMessage = Em.I18n.t("widget.create.wizard.step3.name.invalid.msg");
      this.set("isNameInvalid",true);
    }

    if(widgetName && !validator.isValidWidgetName(widgetName)){
      errorMessage = Em.I18n.t("widget.create.wizard.step3.name.invalidCharacter.msg");
      this.set("isNameInvalid",true);
    }
    this.set('widgetNameErrorMessage',errorMessage);
  }.observes('widgetName'),

  /**
   * validates the description on 3rd step
   */
  validateDescription: function(){
    var errorMessage='';
    var widgetDescription = this.get('widgetDescription');
    this.set("isDescriptionInvalid",false);
    if(widgetDescription && widgetDescription.length > 2048){
      errorMessage = Em.I18n.t("widget.create.wizard.step3.description.invalid.msg");
      this.set("isDescriptionInvalid",true);
    }

    if(widgetDescription && !validator.isValidWidgetDescription(widgetDescription)){
      errorMessage = Em.I18n.t("widget.create.wizard.step3.description.invalidCharacter.msg");
      this.set("isDescriptionInvalid",true);
    }
    this.set('descriptionErrorMessage',errorMessage);
  }.observes('widgetDescription'),

  /**
   * restore widget data set on 2nd step
   */
  initPreviewData: function () {
    this.set('widgetProperties', this.get('content.widgetProperties'));
    this.set('widgetValues', this.get('content.widgetValues'));
    this.set('widgetMetrics', this.get('content.widgetMetrics'));
    this.set('widgetAuthor', this.get('content.widgetAuthor'));
    this.set('widgetName', this.get('content.widgetName'));
    this.set('widgetDescription', this.get('content.widgetDescription'));
    this.set('isSharedChecked', this.get('content.widgetScope') == 'CLUSTER');
    // on editing, don't allow changing from shared scope to unshare
    var isSharedCheckboxDisabled = ((this.get('content.widgetScope') == 'CLUSTER') && this.get('isEditController'));
    this.set('isSharedCheckboxDisabled', isSharedCheckboxDisabled);
    if (!isSharedCheckboxDisabled) {
      this.addObserver('isSharedChecked', this, this.showConfirmationOnSharing);
    }
  },

  /**
   * confirmation popup
   * @returns {App.ModalPopup|undefined}
   */
  showConfirmationOnSharing: function () {
    var self = this;
    if (this.get('isSharedChecked')) {
      var bodyMessage = Em.Object.create({
        confirmMsg: Em.I18n.t('dashboard.widgets.browser.action.share.confirmation'),
        confirmButton: Em.I18n.t('dashboard.widgets.browser.action.share')
      });
      return App.showConfirmationFeedBackPopup(function (query) {
        self.set('isSharedChecked', true);
      }, bodyMessage, function (query) {
        self.set('isSharedChecked', false);
      });
    }
  },

  /**
   * collect all needed data to create new widget
   * @returns {{WidgetInfo: {cluster_name: *, widget_name: *, widget_type: *, description: *, scope: string, metrics: *, values: *, properties: *}}}
   */
  collectWidgetData: function () {
    var widgetData = {
      WidgetInfo: {
        widget_name: this.get('widgetName'),
        widget_type: this.get('content.widgetType'),
        description: this.get('widgetDescription') || "",
        scope: this.get('widgetScope').toUpperCase(),
        author: this.get('widgetAuthor'),
        values: this.get('widgetValues').map(function (value) {
          delete value.computedValue;
          return value;
        }),
        properties: this.get('widgetProperties')
      }
    };

    this.get('widgetMetrics').forEach(function (metric) {
      if (metric.tag) widgetData.WidgetInfo.tag = metric.tag;
    });

    widgetData.WidgetInfo.metrics = this.get('widgetMetrics').map(function (metric) {
      delete metric.data;
      delete metric.tag;
      return metric;
    });

    return widgetData;
  },

  cancel: function () {
    App.router.get(this.get('content.controllerName')).cancel();
  },

  complete: function () {
    App.router.send('complete', this.collectWidgetData());
    App.router.get(this.get('content.controllerName')).finishWizard();
  }
});
