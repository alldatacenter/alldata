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
  route: '/highAvailability/NameNode/enable',

  breadcrumbs: {
    label: Em.I18n.t('admin.highAvailability.wizard.header')
  },

  enter: function (router) {
    var highAvailabilityWizardController = router.get('highAvailabilityWizardController');
    highAvailabilityWizardController.dataLoading().done(function () {
        App.router.get('updateController').set('isWorking', false);
        var popup = App.ModalPopup.show({
          classNames: ['wizard-modal-wrapper'],
          modalDialogClasses: ['modal-xlg'],
          header: Em.I18n.t('admin.highAvailability.wizard.header'),
          bodyClass: App.HighAvailabilityWizardView.extend({
            controller: highAvailabilityWizardController
          }),
          primary: Em.I18n.t('form.cancel'),
          showFooter: false,
          secondary: null,
          hideCloseButton: function () {
            var currStep = App.router.get('highAvailabilityWizardController.currentStep');
            switch (currStep) {
              case "5" :
              case "7" :
              case "9" :
                if (App.supports.autoRollbackHA) {
                  this.set('showCloseButton', false);
                } else {
                  this.set('showCloseButton', true);
                }
                break;
              default :
                this.set('showCloseButton', true);
            }
          }.observes('App.router.highAvailabilityWizardController.currentStep'),

          onClose: function () {
            var currStep = App.router.get('highAvailabilityWizardController.currentStep');
            var highAvailabilityProgressPageController = App.router.get('highAvailabilityProgressPageController');
            if (parseInt(currStep, 10) > 4) {
              if (!App.supports.autoRollbackHA) {
                highAvailabilityProgressPageController.manualRollback();
              } else {
                this.hide();
                App.router.get('highAvailabilityWizardController').setCurrentStep('1');
                App.router.transitionTo('rollbackHighAvailability');
              }
            } else {
              var controller = App.router.get('highAvailabilityWizardController');
              controller.clearTasksData();
              controller.resetOnClose(controller, 'main.services.index');
            }
          },
          didInsertElement: function () {
            this._super();
            this.fitHeight();
          }
        });
        highAvailabilityWizardController.set('popup', popup);
        var currentClusterStatus = App.clusterStatus.get('value');
        if (currentClusterStatus) {
          switch (currentClusterStatus.clusterState) {
            case 'HIGH_AVAILABILITY_DEPLOY' :
              highAvailabilityWizardController.setCurrentStep(currentClusterStatus.localdb.HighAvailabilityWizard.currentStep);
              break;
            default:
              var currStep = App.router.get('highAvailabilityWizardController.currentStep');
              highAvailabilityWizardController.setCurrentStep(currStep);
              break;
          }
        }
      Em.run.next(function () {
        App.router.get('wizardWatcherController').setUser(highAvailabilityWizardController.get('name'));
        router.transitionTo('step' + highAvailabilityWizardController.get('currentStep'));
      });
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController'),
        highAvailabilityWizardStep1Controller = router.get('highAvailabilityWizardStep1Controller');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('1');
        highAvailabilityWizardStep1Controller.setHawqInstalled();
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('highAvailabilityWizardStep1', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.saveNameServiceId(router.get('highAvailabilityWizardStep1Controller.content.nameServiceId'));
      controller.clearMasterComponentHosts();
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('2');
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('highAvailabilityWizardStep2', controller.get('content'));
        });
      });
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      var highAvailabilityWizardStep2Controller = router.get('highAvailabilityWizardStep2Controller');
      var addNN = highAvailabilityWizardStep2Controller.get('selectedServicesMasters').filterProperty('component_name', 'NAMENODE').findProperty('isInstalled', false).get('selectedHost');
      var sNN = highAvailabilityWizardStep2Controller.get('selectedServicesMasters').findProperty('component_name','SECONDARY_NAMENODE').get('selectedHost');
      if(addNN){
        App.db.setRollBackHighAvailabilityWizardAddNNHost(addNN);
      }
      if(sNN){
        App.db.setRollBackHighAvailabilityWizardSNNHost(sNN);
      }

      controller.saveMasterComponentHosts(highAvailabilityWizardStep2Controller);
      controller.get('content').set('serviceConfigProperties', null);
      controller.setDBProperty('serviceConfigProperties', null);
      router.transitionTo('step3');
    },
    back: function (router) {
      router.transitionTo('step1');
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('3');
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('highAvailabilityWizardStep3', controller.get('content'));
        });
      });
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      App.set('router.nextBtnClickInProgress', true);
      var controller = router.get('highAvailabilityWizardController');
      var stepController = router.get('highAvailabilityWizardStep3Controller');
      controller.saveServiceConfigProperties(stepController);
      controller.saveConfigTag(stepController.get("hdfsSiteTag"));
      controller.saveConfigTag(stepController.get("coreSiteTag"));
      if (App.Service.find().someProperty('serviceName', 'HBASE')) {
        controller.saveConfigTag(stepController.get("hbaseSiteTag"));
      }
      if (App.Service.find().someProperty('serviceName', 'RANGER')) {
        controller.saveConfigTag(stepController.get("rangerEnvTag"));
      }
      App.set('router.nextBtnClickInProgress', false);
      router.transitionTo('step4');
    },
    back: Em.Router.transitionTo('step2')
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('4');
        controller.setLowerStepsDisable(4);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('highAvailabilityWizardStep4', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.clearTasksData();
      router.transitionTo('step5');
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('5');
        controller.setLowerStepsDisable(5);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('highAvailabilityWizardStep5', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.clearTasksData();
      router.transitionTo('step6');
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('6');
        controller.setLowerStepsDisable(6);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('highAvailabilityWizardStep6', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      router.transitionTo('step7');
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('7');
        controller.setLowerStepsDisable(7);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('highAvailabilityWizardStep7', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.clearTasksData();
      router.transitionTo('step8');
    }
  }),

  step8: Em.Route.extend({
    route: '/step8',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('8');
        controller.setLowerStepsDisable(8);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('highAvailabilityWizardStep8', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      App.showConfirmationPopup(function() {
        router.transitionTo('step9');
      }, Em.I18n.t('admin.highAvailability.wizard.step8.confirmPopup.body'));
    }
  }),

  step9: Em.Route.extend({
    route: '/step9',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('9');
        controller.setLowerStepsDisable(9);
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('highAvailabilityWizardStep9', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var proceed = function() {
        var controller = router.get('highAvailabilityWizardController');
        controller.clearTasksData();
        controller.resetOnClose(controller, 'main.services.index');
      };
      if (App.Service.find().someProperty('serviceName', 'HAWQ')) {
        App.showAlertPopup(
            Em.I18n.t('admin.highAvailability.wizard.step9.hawq.confirmPopup.header'),
            Em.I18n.t('admin.highAvailability.wizard.step9.hawq.confirmPopup.body'),
            proceed);
      }
      else {
        proceed();
      }
    }
  })

});
