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
  route: '/highAvailability/RangerAdmin/enable',

  breadcrumbs: {
    label: Em.I18n.t('admin.ra_highAvailability.wizard.header')
  },

  enter: function (router) {
    var rAHighAvailabilityWizardController = router.get('rAHighAvailabilityWizardController');
    rAHighAvailabilityWizardController.dataLoading().done(function () {
      //Set RANGER as current service
      App.router.set('mainServiceItemController.content', App.Service.find().findProperty('serviceName', 'RANGER'));
    });
    Em.run.next(function () {
      App.router.get('updateController').set('isWorking', false);
      var popup = App.ModalPopup.show({
        classNames: ['wizard-modal-wrapper'],
        modalDialogClasses: ['modal-xlg'],
        header: Em.I18n.t('admin.ra_highAvailability.wizard.header'),
        bodyClass: App.RAHighAvailabilityWizardView.extend({
          controller: rAHighAvailabilityWizardController
        }),
        primary: Em.I18n.t('form.cancel'),
        showFooter: false,
        secondary: null,

        onClose: function () {
          var rAHighAvailabilityWizardController = router.get('rAHighAvailabilityWizardController'),
              currStep = rAHighAvailabilityWizardController.get('currentStep');
          if (parseInt(currStep) === 4) {
            App.showConfirmationPopup(function () {
              rAHighAvailabilityWizardController.resetOnClose(rAHighAvailabilityWizardController, 'main.services.index');
            }, Em.I18n.t('admin.ra_highAvailability.closePopup'));
          } else {
            rAHighAvailabilityWizardController.resetOnClose(rAHighAvailabilityWizardController, 'main.services.index');
          }
        },
        didInsertElement: function () {
          this._super();
          this.fitHeight();
        }
      });
      rAHighAvailabilityWizardController.set('popup', popup);
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        switch (currentClusterStatus.clusterState) {
          case 'RA_HIGH_AVAILABILITY_DEPLOY' :
            rAHighAvailabilityWizardController.setCurrentStep(currentClusterStatus.localdb.RAHighAvailabilityWizard.currentStep);
            break;
          default:
            var currStep = App.router.get('rAHighAvailabilityWizardController.currentStep');
            rAHighAvailabilityWizardController.setCurrentStep(currStep);
            break;
        }
      }
      App.router.get('wizardWatcherController').setUser(rAHighAvailabilityWizardController.get('name'));
      router.transitionTo('step' + rAHighAvailabilityWizardController.get('currentStep'));
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      var controller = router.get('rAHighAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('1');
        controller.loadAllPriorSteps().done(function(){
          controller.connectOutlet('rAHighAvailabilityWizardStep1', controller.get('content'));
        });
      });
    },
    next: function (router) {
      var controller = router.get('rAHighAvailabilityWizardController');
      controller.save('loadBalancerURL');
      controller.clearMasterComponentHosts();
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      var controller = router.get('rAHighAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('2');
        controller.loadAllPriorSteps().done(function(){
          controller.connectOutlet('rAHighAvailabilityWizardStep2', controller.get('content'));
        });
      });
    },
    next: function (router) {
      var wizardController = router.get('rAHighAvailabilityWizardController');
      var stepController = router.get('rAHighAvailabilityWizardStep2Controller');
      var currentRA = stepController.get('servicesMasters').filterProperty('component_name', 'RANGER_ADMIN').findProperty('isInstalled', true);
      var additionalRAs = stepController.get('servicesMasters').filterProperty('component_name', 'RANGER_ADMIN').filterProperty('isInstalled', false);
      var raHosts = {
        currentRA: currentRA.get('selectedHost'),
        additionalRA: additionalRAs.mapProperty('selectedHost')
      };
      wizardController.set('content.raHosts', raHosts);
      wizardController.save('raHosts');
      wizardController.saveMasterComponentHosts(stepController);
      router.transitionTo('step3');
    },
    back: function (router) {
      router.transitionTo('step1');
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      var controller = router.get('rAHighAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('3');
        controller.loadAllPriorSteps().done(function () {
          var stepController = router.get('rAHighAvailabilityWizardStep3Controller');
          stepController.set('wizardController', controller);
          controller.connectOutlet('rAHighAvailabilityWizardStep3', controller.get('content'));
        });
      });
    },
    next: function (router) {
      router.transitionTo('step4');
    },
    back: function (router) {
      router.transitionTo('step2');
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router) {
      var controller = router.get('rAHighAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('4');
        controller.setLowerStepsDisable(4);
        controller.loadAllPriorSteps().done(function () {
          var stepController = router.get('rAHighAvailabilityWizardStep4Controller');
          stepController.set('wizardController', controller);
          controller.connectOutlet('rAHighAvailabilityWizardStep4', controller.get('content'));
        });
      });
    },
    next: function (router) {
      router.get('updateController').set('isWorking', true);
      var rAHighAvailabilityWizardController = router.get('rAHighAvailabilityWizardController');
      rAHighAvailabilityWizardController.finish();
      App.clusterStatus.setClusterStatus({
        clusterName: App.router.getClusterName(),
        clusterState: 'DEFAULT',
        localdb: App.db.data
      }, {
        alwaysCallback: function () {
          rAHighAvailabilityWizardController.get('popup').hide();
          router.transitionTo('main.services.index');
          Em.run.next(function() {
            location.reload();
          });
        }
      });
    }
  })
});
