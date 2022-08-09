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
  route: '/highAvailability/JournalNode/manage',

  breadcrumbs: {
    label: Em.I18n.t('admin.manageJournalNode.wizard.header')
  },

  enter: function (router) {
    var manageJournalNodeWizardController = router.get('manageJournalNodeWizardController');
    manageJournalNodeWizardController.dataLoading().done(function () {
      App.router.get('updateController').set('isWorking', false);
      var popup = App.ModalPopup.show({
        classNames: ['wizard-modal-wrapper'],
        modalDialogClasses: ['modal-xlg'],
        header: Em.I18n.t('admin.manageJournalNode.wizard.header'),
        bodyClass: App.ManageJournalNodeWizardView.extend({
          controller: manageJournalNodeWizardController
        }),
        primary: Em.I18n.t('form.cancel'),
        showFooter: false,
        secondary: null,
        hideCloseButton: function () {
          var currStep = App.router.get('manageJournalNodeWizardController.currentStep');
          switch (currStep) {
            default :
              this.set('showCloseButton', true);
          }
        }.observes('App.router.manageJournalNodeWizardController.currentStep'),

        onClose: function () {
          var controller = App.router.get('manageJournalNodeWizardController');
          controller.clearTasksData();
          controller.resetOnClose(controller, 'main.services.index');
        },
        didInsertElement: function () {
          this._super();
          this.fitHeight();
        }
      });
      manageJournalNodeWizardController.set('popup', popup);
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        switch (currentClusterStatus.clusterState) {
          case 'JOURNALNODE_MANAGEMENT' :
            manageJournalNodeWizardController.setCurrentStep(currentClusterStatus.localdb.ManageJournalNodeWizard.currentStep);
            break;
          default:
            var currStep = App.router.get('manageJournalNodeWizardController.currentStep');
            manageJournalNodeWizardController.setCurrentStep(currStep);
            break;
        }
      }
      Em.run.next(function () {
        App.router.get('wizardWatcherController').setUser(manageJournalNodeWizardController.get('name'));
        router.transitionTo('step' + manageJournalNodeWizardController.get('currentStep'));
      });
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('1');
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('manageJournalNodeWizardStep1', controller.get('content'));
        });
      });
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      var step1Controller = router.get('manageJournalNodeWizardStep1Controller');
      controller.saveMasterComponentHosts(step1Controller);
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('2');
        controller.setLowerStepsDisable(2);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('manageJournalNodeWizardStep2', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      App.set('router.nextBtnClickInProgress', true);
      var controller = router.get('manageJournalNodeWizardController');
      var stepController = router.get('manageJournalNodeWizardStep2Controller');
      controller.saveServiceConfigProperties(stepController);
      controller.saveConfigTag(stepController.get("hdfsSiteTag"));
      controller.saveNameServiceId(stepController.get('content.nameServiceId'));
      App.set('router.nextBtnClickInProgress', false);
      if (controller.get('isDeleteOnly')) {
        router.transitionTo('step4');
      } else {
        router.transitionTo('step3');
      }
    },
    back: Em.Router.transitionTo('step1')
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('3');
        controller.setLowerStepsDisable(3);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('manageJournalNodeWizardStep3', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      controller.clearTasksData();
      router.transitionTo('step4');
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('4');
        controller.setLowerStepsDisable(4);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('manageJournalNodeWizardStep4', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      controller.clearTasksData();
      if (controller.get('isDeleteOnly')) {
        router.transitionTo('step6');
      } else {
        router.transitionTo('step5');
      }
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('5');
        controller.setLowerStepsDisable(5);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('manageJournalNodeWizardStep5', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      router.transitionTo('step6');
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('6');
        controller.setLowerStepsDisable(6);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('manageJournalNodeWizardStep6', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      controller.clearTasksData();
      router.transitionTo('step7');
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',
    connectOutlets: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('7');
        controller.setLowerStepsDisable(7);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('manageJournalNodeWizardStep7', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('manageJournalNodeWizardController');
      controller.clearTasksData();
      controller.resetOnClose(controller, 'main.services.index');
    }
  })
});
