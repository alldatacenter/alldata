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
  route: '/host/add',

  leaveWizard: function (router, context) {
    var addHostController = router.get('addHostController');
    addHostController.resetOnClose(addHostController, 'hosts.index', router);
  },

  enter: function (router) {
    var self = this;
    router.get('mainController').dataLoading().done(function () {
      Ember.run.next(function () {
        var addHostController = router.get('addHostController');
        App.router.get('updateController').set('isWorking', false);
        var popup = App.ModalPopup.show({
          classNames: ['wizard-modal-wrapper'],
          modalDialogClasses: ['modal-xlg'],
          header: Em.I18n.t('hosts.add.header'),
          bodyClass: App.AddHostView.extend({
            controllerBinding: 'App.router.addHostController'
          }),
          primary: Em.I18n.t('form.cancel'),
          secondary: null,
          showFooter: false,

          onPrimary: function () {
            this.hide();
            App.router.get('updateController').set('isWorking', true);
            router.transitionTo('hosts.index');
          },
          onClose: function () {
            var popupContext = this;
            router.set('nextBtnClickInProgress', true);
            if (addHostController.get('currentStep') == '6') {
              App.ModalPopup.show({
                header: Em.I18n.t('hosts.add.exit.header'),
                body: Em.I18n.t('hosts.add.exit.body'),
                onPrimary: function () {
                  self.leaveWizard(router, popupContext);
                }
              });
            } else {
              self.leaveWizard(router, this);
            }
          },
          didInsertElement: function () {
            this._super();
            this.fitHeight();
          }
        });
        var currentClusterStatus = App.clusterStatus.get('value');
        if (currentClusterStatus) {
          switch (currentClusterStatus.clusterState) {
            case 'ADD_HOSTS_DEPLOY_PREP_2' :
              addHostController.setCurrentStep('4');
              break;
            case 'ADD_HOSTS_INSTALLING_3' :
            case 'SERVICE_STARTING_3' :
              addHostController.setCurrentStep('5');
              break;
            case 'ADD_HOSTS_INSTALLED_4' :
              addHostController.setCurrentStep('6');
              break;
            default:
              break;
          }
        }

        addHostController.set('popup', popup);
        App.router.get('wizardWatcherController').setUser(addHostController.get('name'));
        router.transitionTo('step' + addHostController.get('currentStep'));
      });
    });

  },

  step1: App.StepRoute.extend({
    route: '/step1',
    connectOutlets: function (router) {
      var controller = router.get('addHostController');
      controller.setCurrentStep('1');
      controller.set('hideBackButton', true);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        var wizardStep2Controller = router.get('wizardStep2Controller');
        wizardStep2Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep2', controller.get('content'));
      });
    },

    nextTransition: function (router) {
      var controller = router.get('addHostController');
      controller.save('installOptions');
      //hosts was saved to content.hosts inside wizardStep2Controller
      controller.save('hosts');
      router.transitionTo('step2');
      controller.setDBProperty('bootStatus', false);
    },
    evaluateStep: function (router) {
      var addHostController = router.get('addHostController');
      var wizardStep2Controller = router.get('wizardStep2Controller');

      wizardStep2Controller.set('hasSubmitted', true);

      if (!wizardStep2Controller.get('isSubmitDisabled')) {
        wizardStep2Controller.evaluateStep();
      }
    }
  }),

  step2: App.StepRoute.extend({
    route: '/step2',
    connectOutlets: function (router) {
      var controller = router.get('addHostController');
      controller.setCurrentStep('2');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        var wizardStep3Controller = router.get('wizardStep3Controller');
        wizardStep3Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep3', controller.get('content'));
      });
    },
    backTransition: function (router) {
      router.transitionTo('step1');
    },
    exit: function (router) {
      router.get('wizardStep3Controller').set('stopBootstrap', true);
    },
    nextTransition: function (router, context) {
      var addHostController = router.get('addHostController');
      var wizardStep3Controller = router.get('wizardStep3Controller');
      var wizardStep6Controller = router.get('wizardStep6Controller');
      addHostController.saveConfirmedHosts(wizardStep3Controller);
      addHostController.saveClients();

      addHostController.setDBProperties({
        bootStatus: true,
        slaveComponentHosts: undefined
      });
      wizardStep6Controller.set('isClientsSet', false);
      router.transitionTo('step3');
    },
    /**
     * Wrapper for remove host action.
     * Since saving data stored in addHostController, we should call this from router
     * @param router
     * @param context Array of hosts to delete
     */
    removeHosts: function (router, context) {
      var controller = router.get('addHostController');
      controller.removeHosts(context);
    }
  }),

  step3: App.StepRoute.extend({
    route: '/step3',
    connectOutlets: function (router, context) {
      var controller = router.get('addHostController');
      controller.setCurrentStep('3');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        var wizardStep6Controller = router.get('wizardStep6Controller');
        wizardStep6Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep6', controller.get('content'));
      });
    },
    backTransition: function (router) {
      router.transitionTo('step2');
    },
    next: function (router) {
      App.set('router.nextBtnClickInProgress', true);
      var addHostController = router.get('addHostController');
      var wizardStep6Controller = router.get('wizardStep6Controller');

      wizardStep6Controller.callValidation(function () {
        wizardStep6Controller.showValidationIssuesAcceptBox(function () {
          addHostController.saveSlaveComponentHosts(wizardStep6Controller);
          if (wizardStep6Controller.isAllCheckboxesEmpty()) {
            App.set('router.nextBtnClickInProgress', false);
            router.transitionTo('step5');
            addHostController.set('content.configGroups', []);
            addHostController.saveServiceConfigGroups();
          } else {
            App.set('router.nextBtnClickInProgress', false);
            router.transitionTo('step4');
          }
        });
      });
    }
  }),

  step4: App.StepRoute.extend({
    route: '/step4',
    connectOutlets: function (router, context) {
      var controller = router.get('addHostController');
      var addHostStep4Controller = router.get('addHostStep4Controller');
      controller.setCurrentStep('4');
      controller.dataLoading().done(function () {
        addHostStep4Controller.loadConfigGroups();
        addHostStep4Controller.set('isConfigGroupLoaded', false);
        addHostStep4Controller.configGroupsLoading().done(function () {
          controller.loadAllPriorSteps();
          controller.loadServiceConfigGroups();
          addHostStep4Controller.set('wizardController', controller);
          controller.connectOutlet('addHostStep4', controller.get('content'));
        });
      });
    },
    backTransition: function (router) {
      var goToPreviousStep = function() {
        router.transitionTo('step3');
      };
      var wizardStep7Controller = router.get('wizardStep7Controller');
      if (wizardStep7Controller.hasChanges()) {
        wizardStep7Controller.showChangesWarningPopup(goToPreviousStep);
      } else {
        goToPreviousStep();
      }
    },
    nextTransition: function (router) {
      var addHostController = router.get('addHostController');
      addHostController.saveServiceConfigGroups();
      router.transitionTo('step5');
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router, context) {
      var controller = router.get('addHostController');
      controller.setCurrentStep('5');
      controller.dataLoading().done(function () {
        router.get('mainController').isLoading.call(router.get('clusterController'), 'isServiceContentFullyLoaded').done(function () {
          controller.loadAllPriorSteps();
          controller.getServiceConfigGroups();
          var wizardStep8Controller = router.get('wizardStep8Controller');
          wizardStep8Controller.set('wizardController', controller);
          controller.connectOutlet('wizardStep8', controller.get('content'));
        });
      });
    },
    back: function(router){
      var addHostController = router.get('addHostController');

      if (addHostController.isConfigGroupsEmpty() && !router.get('wizardStep8Controller.isBackBtnDisabled')) {
        router.transitionTo('step3');
      } else if (!router.get('wizardStep8Controller.isBackBtnDisabled')) {
        router.transitionTo('step4');
      }
    },
    next: function (router) {
      router.get('mainAdminKerberosController').getKDCSessionState(function() {
        var addHostController = router.get('addHostController');
        var wizardStep8Controller = router.get('wizardStep8Controller');
        addHostController.applyConfigGroup();
        addHostController.installServices(false, function () {
          addHostController.setInfoForStep9();
          // We need to do recovery based on whether we are in Add Host or Installer wizard
          router.set('nextBtnClickInProgress', true);
          addHostController.saveClusterState('ADD_HOSTS_INSTALLING_3', {
            alwaysCallback: function () {
              wizardStep8Controller.set('servicesInstalled', true);
              router.set('nextBtnClickInProgress', false);
              router.transitionTo('step6');
            }
          });
        });
      });
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router, context) {
      var controller = router.get('addHostController');
      controller.setCurrentStep('6');
      controller.dataLoading().done(function () {
        var wizardStep9Controller = router.get('wizardStep9Controller');
        wizardStep9Controller.set('wizardController', controller);
        controller.loadAllPriorSteps();
        if (!App.get('testMode')) {              //if test mode is ON don't disable prior steps link.
          controller.setLowerStepsDisable(6);
        }
        controller.connectOutlet('wizardStep9', controller.get('content'));
      });
    },
    back: Em.Router.transitionTo('step5'),
    retry: function (router, context) {
      var addHostController = router.get('addHostController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      wizardStep9Controller.set('wizardController', addHostController);
      if (wizardStep9Controller.get('showRetry')) {
        if (wizardStep9Controller.get('content.cluster.status') === 'INSTALL FAILED') {
          var isRetry = true;
          addHostController.installServices(isRetry, function () {
            addHostController.setInfoForStep9();
            wizardStep9Controller.resetHostsForRetry();
            // We need to do recovery based on whether we are in Add Host or Installer wizard
            addHostController.saveClusterState('ADD_HOSTS_INSTALLING_3', {
              alwaysCallback: function () {
                wizardStep9Controller.navigateStep();
              }
            });
          });
        } else {
          wizardStep9Controller.navigateStep();
        }
      }
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var addHostController = router.get('addHostController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      wizardStep9Controller.set('wizardController', addHostController);
      addHostController.saveInstalledHosts(wizardStep9Controller);
      router.set('nextBtnClickInProgress', true);

      // We need to do recovery based on whether we are in Add Host or Installer wizard
      addHostController.saveClusterState('ADD_HOSTS_INSTALLED_4', {
        alwaysCallback: function () {
          router.set('nextBtnClickInProgress', false);
          router.transitionTo('step7');
        }
      });
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',
    connectOutlets: function (router, context) {
      var controller = router.get('addHostController');
      controller.setCurrentStep('7');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        var wizardStep10Controller = router.get('wizardStep10Controller');
        wizardStep10Controller.set('wizardController', controller);
        if (!App.get('testMode')) {              //if test mode is ON don't disable prior steps link.
          controller.setLowerStepsDisable(7);
        }
        controller.connectOutlet('wizardStep10', controller.get('content'));
        router.get('updateController').set('isWorking', true);
      });
    },
    back: Em.Router.transitionTo('step6'),
    complete: function (router, context) {
      $(context.currentTarget).parents("#modal").find(".close").trigger('click');
    }
  }),

  backToHostsList: function (router, event) {
    App.router.get('updateController').set('isWorking', true);
    router.transitionTo('hosts.index');
  }


});
