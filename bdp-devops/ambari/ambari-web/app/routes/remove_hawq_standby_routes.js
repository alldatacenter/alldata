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
  route: '/highAvailability/Hawq/remove',

  breadcrumbs: {
    label: Em.I18n.t('admin.removeHawqStandby.wizard.header')
  },

  enter: function (router, transition) {
    var removeHawqStandbyWizardController = router.get('removeHawqStandbyWizardController');
    removeHawqStandbyWizardController.dataLoading().done(function () {
      App.router.set('mainServiceItemController.content', App.Service.find().findProperty('serviceName', 'HAWQ'));
    });
    Em.run.next(function() {
      App.router.get('updateController').set('isWorking', false);
      var popup = App.ModalPopup.show({
        classNames: ['wizard-modal-wrapper'],
        modalDialogClasses: ['modal-xlg'],
        header: Em.I18n.t('admin.removeHawqStandby.wizard.header'),
        bodyClass: App.RemoveHawqStandbyWizardView.extend({
          controller: removeHawqStandbyWizardController
        }),
        primary: Em.I18n.t('form.cancel'),
        showFooter: false,
        secondary: null,

        onClose: function () {
          var removeHawqStandbyWizardController = router.get('removeHawqStandbyWizardController'),
              currStep = removeHawqStandbyWizardController.get('currentStep')
          if (parseInt(currStep) === 3) {
            App.showConfirmationPopup(function () {
              var controller = router.get('removeHawqStandbyWizardController');
              router.get('updateController').set('isWorking', true);
              removeHawqStandbyWizardController.finish();
              App.clusterStatus.setClusterStatus({
                clusterName: App.router.getClusterName(),
                clusterState: 'DEFAULT',
                localdb: App.db.data
              }, {
                alwaysCallback: function () {
                  controller.get('popup').hide();
                  router.transitionTo('main.services.index');
                  Em.run.next(function() {
                    location.reload();
                  });
                }
              });
            }, Em.I18n.t('admin.removeHawqStandby.closePopup'));
          } else {
            var controller = router.get('removeHawqStandbyWizardController');
            router.get('updateController').set('isWorking', true);
            removeHawqStandbyWizardController.finish();
            App.clusterStatus.setClusterStatus({
              clusterName: App.router.getClusterName(),
              clusterState: 'DEFAULT',
              localdb: App.db.data
            }, {
              alwaysCallback: function () {
                controller.get('popup').hide();
                router.transitionTo('main.services.index');
                Em.run.next(function() {
                  location.reload();
                });
              }
            });
          }
        },
        didInsertElement: function () {
          this._super();
          this.fitHeight();
        }
      });
      removeHawqStandbyWizardController.set('popup', popup);
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        switch (currentClusterStatus.clusterState) {
          case 'REMOVE_HAWQ_STANDBY' :
            removeHawqStandbyWizardController.setCurrentStep(currentClusterStatus.localdb.RemoveHawqStandbyWizard.currentStep);
            break;
          default:
            var currStep = App.router.get('removeHawqStandbyWizardController.currentStep');
            removeHawqStandbyWizardController.setCurrentStep(currStep);
            break;
        }
      }
      router.transitionTo('step' + removeHawqStandbyWizardController.get('currentStep'));
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      var controller = router.get('removeHawqStandbyWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('1');
        controller.connectOutlet('removeHawqStandbyWizardStep1', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('removeHawqStandbyWizardController');
      var hawqMaster = App.HostComponent.find().findProperty('componentName','HAWQMASTER').get('hostName');
      var hawqStandby = App.HostComponent.find().findProperty('componentName','HAWQSTANDBY').get('hostName');
      var hawqHosts = {
        hawqMaster: hawqMaster,
        hawqStandby: hawqStandby
      };
      controller.saveHawqHosts(hawqHosts);
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      var controller = router.get('removeHawqStandbyWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('2');
        controller.loadAllPriorSteps();
        controller.connectOutlet('removeHawqStandbyWizardStep2', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      App.showConfirmationPopup(function() {
        var wizardController = router.get('removeHawqStandbyWizardController');
        var stepController = router.get('removeHawqStandbyWizardStep2Controller');
        var configs = stepController.get('selectedService.configs');
        wizardController.saveConfigs(configs);
        router.transitionTo('step3');
      }, Em.I18n.t('admin.removeHawqStandby.wizard.step2.confirmPopup.body'));
    },
    back: Em.Router.transitionTo('step1')
  }),
  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      var controller = router.get('removeHawqStandbyWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('3');
        controller.setLowerStepsDisable(3);
        controller.loadAllPriorSteps();
        controller.connectOutlet('removeHawqStandbyWizardStep3', controller.get('content'));
      })
    },
    unroutePath: function (router, path) {
      // allow user to leave route if wizard has finished
      if (router.get('removeHawqStandbyWizardController').get('isFinished')) {
        this._super(router, path);
      } else {
        return false;
      }
    },
    next: function (router) {
      var controller = router.get('removeHawqStandbyWizardController');
      controller.finish();
      App.clusterStatus.setClusterStatus({
        clusterName: controller.get('content.cluster.name'),
        clusterState: 'DEFAULT',
        localdb: App.db.data
      }, {
        alwaysCallback: function () {
          controller.get('popup').hide();
          router.transitionTo('main.services.index');
          Em.run.next(function () {
            location.reload();
          });
        }
      });
    }
  })

});
