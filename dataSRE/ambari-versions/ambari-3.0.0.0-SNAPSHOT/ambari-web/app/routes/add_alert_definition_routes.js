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

  route: '/alerts/add',

  enter: function (router) {
    if (App.isAuthorized('SERVICE.TOGGLE_ALERTS')) {
      Em.run.next(function () {
        var addAlertDefinitionController = router.get('addAlertDefinitionController');
        App.router.get('updateController').set('isWorking', false);
        var popup = App.ModalPopup.show({
          classNames: ['wizard-modal-wrapper'],
          modalDialogClasses: ['modal-xlg'],
          header: Em.I18n.t('alerts.add.header'),
          bodyClass: App.AddAlertDefinitionView.extend({
            controllerBinding: 'App.router.addAlertDefinitionController'
          }),
          primary: Em.I18n.t('form.cancel'),
          showFooter: false,
          secondary: null,

          onPrimary: function () {
            this.hide();
            App.router.transitionTo('main.alerts.index');
          },
          onClose: function () {
            this.set('showCloseButton', false); // prevent user to click "Close" many times
            App.router.get('updateController').set('isWorking', true);
            router.transitionTo('main.alerts.index');
            this.hide();
          },
          didInsertElement: function () {
            this._super();
            this.fitHeight();
          }
        });
        addAlertDefinitionController.set('popup', popup);
        router.transitionTo('step' + addAlertDefinitionController.get('currentStep'));
      });
    } else {
      Em.run.next(function () {
        App.router.transitionTo('main.alerts');
      });
    }
  },

  step1: Em.Route.extend({

    route: '/step1',

    connectOutlets: function (router) {
      var controller = router.get('addAlertDefinitionController');
      controller.setCurrentStep('1');
      controller.set('content', controller.getDBProperty('content'));
      controller.connectOutlet('addAlertDefinitionStep1', controller.get('content'));
    },

    next: function (router) {
      var controller = router.get('addAlertDefinitionController');
      controller.setDBProperty('content', controller.get('content'));
      router.transitionTo('step2');
    }

  }),

  step2: Em.Route.extend({

    route: '/step2',

    connectOutlets: function (router) {
      var controller = router.get('addAlertDefinitionController');
      controller.setCurrentStep('2');
      controller.set('content', controller.getDBProperty('content'));
      controller.connectOutlet('addAlertDefinitionStep2', controller.get('content'));
    },

    back: Em.Router.transitionTo('step1'),

    next: function (router) {
      var controller = router.get('addAlertDefinitionController');
      controller.set('content.configs', App.router.get('mainAlertDefinitionConfigsController.configs'));
      var newDefinitionData = App.router.get('mainAlertDefinitionConfigsController').getPropertiesToUpdate(false);
      newDefinitionData['AlertDefinition/source'].type = controller.get('content.selectedType');
      newDefinitionData['AlertDefinition/label'] = newDefinitionData['AlertDefinition/name'];
      newDefinitionData['AlertDefinition/name'] = newDefinitionData['AlertDefinition/name'].toLowerCase().replace(/\s+/g, '_');
      controller.set('content.formattedToRequestConfigs', newDefinitionData);
      controller.setDBProperty('content', controller.get('content'));
      router.transitionTo('step3');
    }

  }),

  step3: Em.Route.extend({

    route: '/step3',

    connectOutlets: function (router) {
      var controller = router.get('addAlertDefinitionController');
      controller.setCurrentStep('3');
      controller.set('content', controller.getDBProperty('content'));
      controller.connectOutlet('addAlertDefinitionStep3', controller.get('content'));
    },

    back: Em.Router.transitionTo('step2'),

    done: function (router) {
      var controller = router.get('addAlertDefinitionController');
      controller.createNewAlertDefinition(Em.get(controller.getDBProperty('content'), 'formattedToRequestConfigs')).done(function () {
        controller.get('popup').hide();
        controller.setDBProperty('content', {});
        controller.finish();
        App.clusterStatus.setClusterStatus({
            clusterName: controller.get('content.cluster.name'),
            clusterState: 'DEFAULT',
            localdb: App.db.data
          },
          {
            alwaysCallback: function () {
              controller.get('popup').hide();
              router.transitionTo('main.alerts');
              Em.run.next(function() {
                location.reload();
              });
            }
          });
      });
    }
  })

});
