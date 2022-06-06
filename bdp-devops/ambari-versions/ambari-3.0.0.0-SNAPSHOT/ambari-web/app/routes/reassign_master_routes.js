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
  route: '/service/reassign',

  leaveWizard: function (router, context) {
    var reassignMasterController = router.get('reassignMasterController');
    reassignMasterController.resetOnClose(reassignMasterController, 'main.index');
  },

  breadcrumbs: {
    labelBindingPath: 'App.router.reassignMasterController.content.reassign.display_name',
    labelPostFormat(label) {
      let msg = Em.I18n.t('services.reassign.header');
      return label ? `${msg} (${label})` : msg;
    }
  },

  enter: function (router) {
    var context = this;
    var reassignMasterController = router.get('reassignMasterController');

    reassignMasterController.dataLoading().done(function () {
      if (App.get('allHostNames.length') > 1) {
        Em.run.next(function () {
          App.router.get('updateController').set('isWorking', false);
          var popup = App.ModalPopup.show({
            classNames: ['wizard-modal-wrapper'],
            modalDialogClasses: ['modal-xlg'],
            header: Em.I18n.t('services.reassign.header'),
            bodyClass: App.ReassignMasterView.extend({
              controller: reassignMasterController
            }),
            primary: Em.I18n.t('form.cancel'),
            showFooter: false,
            secondary: null,

            onPrimary: function () {
              this.hide();
              App.router.get('updateController').set('isWorking', true);
              App.router.transitionTo('main.services.index');
            },
            onClose: function () {
              var currStep = reassignMasterController.get('currentStep');
              if (parseInt(currStep) > 3) {
                var self = this;

                var step4Controller = router.get('reassignMasterWizardStep4Controller');
                var testDBTaskId = step4Controller.get('tasks').filterProperty('command', 'testDBConnection').get('firstObject.id');

                if(currStep !== "7"
                   && testDBTaskId
                   && reassignMasterController.get('content.tasksStatuses').get(testDBTaskId) === "FAILED")
                {
                  App.showConfirmationPopup(function () {
                    App.router.transitionTo('step7');
                  }, Em.I18n.t('services.reassign.rollback.confirm'));
                } else {
                  App.showConfirmationPopup(function () {
                    router.get('reassignMasterWizardStep' + currStep + 'Controller').removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
                    context.leaveWizard(router, self);
                  }, Em.I18n.t('services.reassign.closePopup').format(reassignMasterController.get('content.reassign.display_name')));
                }
              } else {
                context.leaveWizard(router, this);
              }
            },
            didInsertElement: function () {
              this._super();
              this.fitHeight();
            }
          });
          reassignMasterController.set('popup', popup);
          reassignMasterController.loadComponentToReassign();
          var currStep = reassignMasterController.get('currentStep');
          var currentClusterStatus = App.clusterStatus.get('value');
          if (currentClusterStatus) {
            switch (currentClusterStatus.clusterState) {
              case 'REASSIGN_MASTER_INSTALLING' :
                if (currentClusterStatus.localdb.ReassignMaster.currentStep !== currStep) {
                  reassignMasterController.setCurrentStep(currentClusterStatus.localdb.ReassignMaster.currentStep);
                }
                break;
            }
          }
          App.router.get('wizardWatcherController').setUser(reassignMasterController.get('name'));
          router.transitionTo('step' + currStep);
        });
      } else {
        App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('services.reassign.error.fewHosts'), function () {
          router.transitionTo('main.services.index');
        })
      }
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      var controller = router.get('reassignMasterController');
      var step1Controller = router.get('reassignMasterWizardStep1Controller');
      controller.setCurrentStep('1');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('reassignMasterWizardStep1', controller.get('content'));
        step1Controller.loadConfigsTags();
      })
    },
    next: function (router) {
      var controller = router.get('reassignMasterController');
      App.db.setMasterComponentHosts(undefined);
      controller.clearMasterComponentHosts();
      router.transitionTo('step2');
    },

    unroutePath: function () {
      return false;
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      var controller = router.get('reassignMasterController');
      controller.setCurrentStep('2');
      router.get('mainController').isLoading.call(router.get('clusterController'), 'isServiceContentFullyLoaded').done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('reassignMasterWizardStep2', controller.get('content'));
      })

    },
    back: Em.Router.transitionTo('step1'),
    next: function (router) {
      var controller = router.get('reassignMasterController');
      var reassignMasterWizardStep2 = router.get('reassignMasterWizardStep2Controller');
      controller.saveMasterComponentHosts(reassignMasterWizardStep2);
      var reassignHosts = {};
      var componentName = reassignMasterWizardStep2.get('content.reassign.component_name');
      var masterAssignmentsHosts = reassignMasterWizardStep2.get('selectedServicesMasters').filterProperty('component_name', componentName).mapProperty('selectedHost');
      var currentMasterHosts = App.HostComponent.find().filterProperty('componentName', componentName).mapProperty('hostName');
      masterAssignmentsHosts.forEach(function (host) {
        if (!currentMasterHosts.contains(host)) {
          reassignHosts.target = host;
        }
      }, this);
      currentMasterHosts.forEach(function (host) {
        if (!masterAssignmentsHosts.contains(host)) {
          reassignHosts.source = host;
        }
      }, this);
      controller.saveReassignHosts(reassignHosts);
      router.transitionTo('step3');
    },

    unroutePath: function () {
      return false;
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      var controller = router.get('reassignMasterController'),
        stepController = router.get('reassignMasterWizardStep3Controller');
      controller.setCurrentStep('3');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        stepController.set('wizardController', controller);
        controller.connectOutlet('reassignMasterWizardStep3', controller.get('content'));
      })
    },
    back: Em.Router.transitionTo('step2'),
    next: function (router) {
      var controller = router.get('reassignMasterController'),
        stepController = router.get('reassignMasterWizardStep3Controller'),
        configs = stepController.get('configs'),
        attributes = stepController.get('configsAttributes'),
        secureConfigs = stepController.get('secureConfigs');
      App.db.setReassignTasksStatuses(undefined);
      App.db.setReassignTasksRequestIds(undefined);
      App.clusterStatus.setClusterStatus({
        clusterName: router.get('reassignMasterController.content.cluster.name'),
        clusterState: 'REASSIGN_MASTER_INSTALLING',
        wizardControllerName: 'reassignMasterController',
        localdb: App.db.data
      });
      controller.saveReassignComponentsInMM(controller.getReassignComponentsInMM());
      stepController.updateServiceConfigs();
      controller.saveConfigs(configs, attributes);
      controller.saveSecureConfigs(secureConfigs);
      router.transitionTo('step4');
    },

    exit: function (router) {
      router.get('reassignMasterWizardStep3Controller').clearStep();
    },

    unroutePath: function () {
      return false;
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router) {
      var controller = router.get('reassignMasterController'),
        stepController = router.get('reassignMasterWizardStep4Controller');
      controller.setCurrentStep('4');
      controller.setLowerStepsDisable(4);
      router.get('mainController').isLoading.call(router.get('clusterController'), 'isServiceContentFullyLoaded').done(function () {
        controller.loadAllPriorSteps();
        stepController.set('wizardController', controller);
        controller.connectOutlet('reassignMasterWizardStep4', controller.get('content'));
      });
    },
    next: function (router) {
      router.get('reassignMasterController').setCurrentStep('5');

      App.clusterStatus.setClusterStatus({
        clusterName: router.get('reassignMasterController.content.cluster.name'),
        clusterState: 'REASSIGN_MASTER_INSTALLING',
        wizardControllerName: 'reassignMasterController',
        localdb: App.db.data
      });

      router.transitionTo('step5');
    },

    complete: function (router) {
      var controller = router.get('reassignMasterController');
      var reassignMasterWizardStep4 = router.get('reassignMasterWizardStep4Controller');
      if (!reassignMasterWizardStep4.get('isSubmitDisabled')) {
        controller.resetOnClose(controller, 'main.index');
      }
    },

    unroutePath: function () {
      return false;
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router) {
      var controller = router.get('reassignMasterController');
      controller.setCurrentStep('5');
      router.get('mainController').isLoading.call(router.get('clusterController'), 'isServiceContentFullyLoaded').done(function () {
        controller.loadAllPriorSteps();
        controller.setLowerStepsDisable(5);
        if ((controller.get('content.reassign.component_name') === 'NAMENODE') || controller.get('content.reassign.component_name') === 'SECONDARY_NAMENODE') {
          controller.usersLoading().done(function () {
            controller.connectOutlet('reassignMasterWizardStep5', controller.get('content'));
          })
        } else {
          controller.connectOutlet('reassignMasterWizardStep5', controller.get('content'));
        }
      })
    },
    next: function (router) {
      App.showConfirmationPopup(function () {
        var controller = router.get('reassignMasterController');
        controller.saveReassignComponentsInMM(controller.getReassignComponentsInMM());
        router.transitionTo('step6');
      }, Em.I18n.t('services.reassign.step5.confirmPopup.body'));
    },

    unroutePath: function () {
      return false;
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router) {
      var controller = router.get('reassignMasterController');
      controller.setCurrentStep('6');
      controller.setLowerStepsDisable(6);
      router.get('mainController').isLoading.call(router.get('clusterController'), 'isServiceContentFullyLoaded').done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('reassignMasterWizardStep6', controller.get('content'));
      })
    },

    next: function (router) {
      var controller = router.get('reassignMasterController');
      controller.finish();
      App.clusterStatus.setClusterStatus({
        clusterName: router.get('reassignMasterController.content.cluster.name'),
        clusterState: 'DEFAULT',
        localdb: App.db.data
      }, {
        alwaysCallback: function () {
          controller.get('popup').hide();
          router.transitionTo('main.index');
          Em.run.next(function () {
            location.reload();
          });
        }
      });
    },

    unroutePath: function () {
      return false;
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',
    connectOutlets: function (router) {
      var controller = router.get('reassignMasterController'),
        stepController = router.get('reassignMasterWizardStep7Controller');
      controller.setCurrentStep('7');
      controller.setLowerStepsDisable(7);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        stepController.set('wizardController', controller);
        controller.connectOutlet('reassignMasterWizardStep7', controller.get('content'));
      });
    },

    next: function (router) {
      var controller = router.get('reassignMasterController');
      controller.resetOnClose(controller, 'main.index');
    },

    complete: function (router) {
      var controller = router.get('reassignMasterController');
      controller.resetOnClose(controller, 'main.index');
    },

    unroutePath: function () {
      return false;
    }
  }),

  gotoStep7: Em.Router.transitionTo('step7'),

  backToServices: function (router) {
    App.router.get('updateController').set('isWorking', true);
    router.transitionTo('services');
  }

});
