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
  route: '/widget/edit',
  enter: function (router, context) {
    router.get('mainController').dataLoading().done(function () {
      var widgetEditController = router.get('widgetEditController');
      var popup = App.ModalPopup.show({
        classNames: ['wizard-modal-wrapper'],
        modalDialogClasses: ['modal-xlg'],
        header: Em.I18n.t('widget.edit.wizard.header'),
        bodyClass: App.WidgetEditView.extend({
          controller: widgetEditController
        }),
        primary: Em.I18n.t('form.cancel'),
        showFooter: false,
        secondary: null,

        onClose: function () {
          widgetEditController.cancel();
        },

        didInsertElement: function () {
          this._super();
          this.fitHeight();
        }

      });
      widgetEditController.set('popup', popup);
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        if (App.get('testMode')) {
          widgetEditController.setCurrentStep(App.db.data.WidgetWizard.currentStep);
        } else {
          var currStep = App.get('router.widgetEditController.currentStep');
          widgetEditController.setCurrentStep(currStep);
        }
      }
      Em.run.next(function () {
        router.transitionTo('step' + widgetEditController.get('currentStep'));
      });
    });
  },

  step1: Em.Route.extend({
    route: '/step1',

    connectOutlets: function (router) {
      var controller = router.get('widgetEditController');
      controller.dataLoading().done(function () {
        router.get('widgetEditController').setCurrentStep('1');
        controller.loadAllPriorSteps();
        controller.connectOutlet('widgetWizardStep2', controller.get('content'));
      });
    },
    unroutePath: function () {
      return false;
    },

    next: function (router) {
      var widgetEditController = router.get('widgetEditController');
      var widgetStep2controller = router.get('widgetWizardStep2Controller');
      widgetEditController.save('widgetProperties', widgetStep2controller.get('widgetProperties'));
      widgetEditController.save('widgetMetrics', widgetStep2controller.get('widgetMetrics'));
      widgetEditController.save('widgetValues', widgetStep2controller.get('widgetValues'));
      widgetEditController.save('expressions', widgetStep2controller.get('expressions'));
      widgetEditController.save('dataSets', widgetStep2controller.get('dataSets'));
      widgetEditController.save('templateValue', widgetStep2controller.get('templateValue'));
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',

    connectOutlets: function (router) {
      var controller = router.get('widgetEditController');
      controller.dataLoading().done(function () {
        router.get('widgetEditController').setCurrentStep('2');
        controller.loadAllPriorSteps();
        controller.connectOutlet('widgetWizardStep3', controller.get('content'));
      });
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step1'),
    complete: function (router, context) {
      router.get('widgetEditController').putWidgetDefinition(context);
    }
  })
});
