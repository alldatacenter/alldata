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

module.exports = App.WizardRoute.extend({
  route: '/widget/create',
  enter: function (router, context) {
    router.get('mainController').dataLoading().done(function () {
      var widgetWizardController = router.get('widgetWizardController');
      App.router.get('updateController').set('isWorking', false);
      var popup = App.ModalPopup.show({
        classNames: ['wizard-modal-wrapper'],
        modalDialogClasses: ['modal-xlg'],
        header: Em.I18n.t('widget.create.wizard.header'),
        bodyClass: App.WidgetWizardView.extend({
          controller: widgetWizardController
        }),
        primary: Em.I18n.t('form.cancel'),
        showFooter: false,
        secondary: null,

        onClose: function () {
          widgetWizardController.cancel();
        },

        didInsertElement: function () {
          this._super();
          this.fitHeight();
        }

      });
      widgetWizardController.set('popup', popup);
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        if (App.get('testMode')) {
          widgetWizardController.setCurrentStep(App.db.data.WidgetWizard.currentStep);
        } else {
          var currStep = App.get('router.widgetWizardController.currentStep');
          widgetWizardController.setCurrentStep(currStep);
        }
      }
      Em.run.next(function () {
        router.transitionTo('step' + widgetWizardController.get('currentStep'));
      });
    });
  },

  step1: Em.Route.extend({
    route: '/step1',

    connectOutlets: function (router) {
      var controller = router.get('widgetWizardController');
      controller.dataLoading().done(function () {
        router.get('widgetWizardController').setCurrentStep('1');
        controller.loadAllPriorSteps();
        controller.connectOutlet('widgetWizardStep1', controller.get('content'));
      });
    },

    unroutePath: function () {
      return false;
    },

    next: function (router) {
      var widgetWizardController = router.get('widgetWizardController');
      var widgetStep1controller = router.get('widgetWizardStep1Controller');
      widgetWizardController.save('widgetType', widgetStep1controller.get('widgetType'));
      widgetWizardController.setDBProperties({
        widgetProperties: {},
        widgetMetrics: [],
        allMetrics: [],
        widgetValues: [],
        expressions: [],
        dataSets: [],
        templateValue: ''
      });
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',

    connectOutlets: function (router) {
      var controller = router.get('widgetWizardController');
      controller.dataLoading().done(function () {
        router.get('widgetWizardController').setCurrentStep('2');
        controller.loadAllPriorSteps();
        controller.connectOutlet('widgetWizardStep2', controller.get('content'));
      });
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step1'),

    next: function (router) {
      var widgetWizardController = router.get('widgetWizardController');
      var widgetStep2controller = router.get('widgetWizardStep2Controller');
      widgetWizardController.save('widgetProperties', widgetStep2controller.get('widgetProperties'));
      widgetWizardController.save('widgetMetrics', widgetStep2controller.get('widgetMetrics'));
      widgetWizardController.save('widgetValues', widgetStep2controller.get('widgetValues'));
      widgetWizardController.save('templateValue', widgetStep2controller.get('templateValue'));
      widgetWizardController.save('widgetName', "");
      widgetWizardController.save('widgetDescription', "");
      widgetWizardController.save('widgetScope', null);
      router.transitionTo('step3');
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',

    connectOutlets: function (router) {
      var controller = router.get('widgetWizardController');
      controller.dataLoading().done(function () {
        router.get('widgetWizardController').setCurrentStep('3');
        controller.loadAllPriorSteps();
        controller.connectOutlet('widgetWizardStep3', controller.get('content'));
      });
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step2'),
    complete: function (router, context) {
      router.get('widgetWizardController').postWidgetDefinition(context);
    }
  })
});
