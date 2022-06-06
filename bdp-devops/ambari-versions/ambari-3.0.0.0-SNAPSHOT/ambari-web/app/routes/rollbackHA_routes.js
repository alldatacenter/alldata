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
  route: '/highAvailability/NameNode/rollbackHA',

  enter: function (router) {
    Em.run.next(function () {
      var rollbackHighAvailabilityWizardController = router.get('rollbackHighAvailabilityWizardController');
      App.router.get('updateController').set('isWorking', false);
      var popup = App.ModalPopup.show({
        classNames: ['wizard-modal-wrapper'],
        modalDialogClasses: ['modal-xlg'],
        header: Em.I18n.t('admin.highAvailability.wizard.rollback.header.title'),
        bodyClass: App.RollbackHighAvailabilityWizardView.extend({
          controller: rollbackHighAvailabilityWizardController
        }),
        primary: Em.I18n.t('form.cancel'),
        showFooter: false,
        secondary: null,
        hideCloseButton: function () {
          var currStep = App.router.get('rollbackHighAvailabilityWizardController.currentStep');
          if(currStep == 3){
            this.set('showCloseButton', false);
          }else{
            this.set('showCloseButton', true);
          }
        }.observes('App.router.rollbackHighAvailabilityWizardController.currentStep'),

        onClose: function () {
          this.hide();
          App.router.get('rollbackHighAvailabilityWizardController').setCurrentStep('1');
          App.router.get('updateController').set('isWorking', true);
          App.router.transitionTo('main.services');
        },
        didInsertElement: function () {
          this._super();
          this.fitHeight();
        }
      });
      rollbackHighAvailabilityWizardController.set('popup', popup);
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        switch (currentClusterStatus.clusterState) {
          case 'ROLLBACK_HIGH_AVAILABILITY' :
            rollbackHighAvailabilityWizardController.setCurrentStep(currentClusterStatus.localdb.RollbackHighAvailabilityWizard.currentStep);
            break;
          default:
            var currStep = App.router.get('rollbackHighAvailabilityWizardController.currentStep');
            rollbackHighAvailabilityWizardController.setCurrentStep(currStep);
            break;
        }
      }
      router.transitionTo('step' + rollbackHighAvailabilityWizardController.get('currentStep'));
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      var controller = router.get('rollbackHighAvailabilityWizardController');
      controller.setCurrentStep('1');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('rollbackHighAvailabilityWizardStep1', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('rollbackHighAvailabilityWizardController');
      controller.saveSelectedAddNN(controller.get('content.selectedAddNNHost'));
      controller.saveSelectedSNN(controller.get('content.selectedSNNHost'));
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      var controller = router.get('rollbackHighAvailabilityWizardController');
      controller.setCurrentStep('2');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.usersLoading().done(function () {
          controller.connectOutlet('rollbackHighAvailabilityWizardStep2', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      router.transitionTo('step3');
    },
    back: function (router) {
      router.transitionTo('step1');
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      var controller = router.get('rollbackHighAvailabilityWizardController');
      controller.setCurrentStep('3');
      controller.setLowerStepsDisable(3);
      App.clusterStatus.setClusterStatus({
        clusterName: router.get('content.cluster.name'),
        clusterState: 'ROLLBACK_HIGH_AVAILABILITY',
        wizardControllerName: 'rollbackHighAvailabilityWizardController',
        localdb: App.db.data
      });
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('rollbackHighAvailabilityWizardStep3',  controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('rollbackHighAvailabilityWizardController');
      controller.clearTasksData();
      controller.clearStorageData();
      controller.finish();
      controller.get('popup').hide();
      App.router.get('updateController').set('isWorking', true);
      App.clusterStatus.setClusterStatus({
        clusterName: router.get('content.cluster.name'),
        clusterState: 'DEFAULT',
        wizardControllerName: 'rollbackHighAvailabilityWizardController',
        localdb: App.db.data
      });
      router.transitionTo('main.index');
      Em.run.next(function() {
        location.reload();
      });
    }
  })

});
