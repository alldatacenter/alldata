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
  route: '/highAvailability/Hawq/add',

  breadcrumbs: {
    label: Em.I18n.t('admin.addHawqStandby.wizard.header')
  },

  enter: function (router, transition) {
    var addHawqStandbyWizardController = router.get('addHawqStandbyWizardController');
    addHawqStandbyWizardController.dataLoading().done(function () {
      //Set HAWQ as current service
      App.router.set('mainServiceItemController.content', App.Service.find().findProperty('serviceName', 'HAWQ'));
      App.router.get('updateController').set('isWorking', false);
      var popup = App.ModalPopup.show({
        classNames: ['wizard-modal-wrapper'],
        modalDialogClasses: ['modal-xlg'],
        header: Em.I18n.t('admin.addHawqStandby.wizard.header'),
        bodyClass: App.AddHawqStandbyWizardView.extend({
          controller: addHawqStandbyWizardController
        }),
        primary: Em.I18n.t('form.cancel'),
        showFooter: false,
        secondary: null,
        updateClusterStatus: function() {
          var controller = router.get('addHawqStandbyWizardController');
          router.get('updateController').set('isWorking', true);
          addHawqStandbyWizardController.finish();
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
        },

        onClose: function () {
          var addHawqStandbyWizardController = router.get('addHawqStandbyWizardController'),
              currStep = addHawqStandbyWizardController.get('currentStep'),
              self = this;

          if (parseInt(currStep) === 4) {
            App.showConfirmationPopup(function () {
              popup.updateClusterStatus();
            }, Em.I18n.t('admin.addHawqStandby.closePopup'));
          } else {
            popup.updateClusterStatus();
          }
        },
        didInsertElement: function () {
          this._super();
          this.fitHeight();
        }
      });
      addHawqStandbyWizardController.set('popup', popup);
      var currentClusterStatus = App.clusterStatus.get('value');
      switch (currentClusterStatus.clusterState) {
        case 'ADD_HAWQ_STANDBY' :
          addHawqStandbyWizardController.setCurrentStep(currentClusterStatus.localdb.AddHawqStandbyWizard.currentStep);
          break;
        default:
          var currStep = App.router.get('addHawqStandbyWizardController.currentStep');
          addHawqStandbyWizardController.setCurrentStep(currStep);
          break;
      }
      Em.run.next(function () {
        App.router.get('wizardWatcherController').setUser(addHawqStandbyWizardController.get('name'));
        router.transitionTo('step' + addHawqStandbyWizardController.get('currentStep'));
      });
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      var controller = router.get('addHawqStandbyWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('1');
        controller.connectOutlet('addHawqStandbyWizardStep1', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('addHawqStandbyWizardController');
      controller.setDBProperty('hawqHosts', undefined);
      controller.clearMasterComponentHosts();
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      var controller = router.get('addHawqStandbyWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('2');
        controller.loadAllPriorSteps();
        controller.connectOutlet('addHawqStandbyWizardStep2', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('addHawqStandbyWizardController');
      var stepController = router.get('addHawqStandbyWizardStep2Controller');
      var hawqHost = {
          hawqMaster : stepController.get('servicesMasters').filterProperty('component_name', 'HAWQMASTER').findProperty('isInstalled', true).get('selectedHost'),
          newHawqStandby : stepController.get('servicesMasters').filterProperty('component_name', 'HAWQSTANDBY').findProperty('isInstalled', false).get('selectedHost')
      };
      controller.saveHawqHosts(hawqHost);
      controller.saveMasterComponentHosts(stepController);
      router.transitionTo('step3');
    },
    back: function (router) {
      router.transitionTo('step1');
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      var controller = router.get('addHawqStandbyWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('3');
        controller.loadAllPriorSteps();
        controller.connectOutlet('addHawqStandbyWizardStep3', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('addHawqStandbyWizardController');
      var stepController = router.get('addHawqStandbyWizardStep3Controller');
      var configs = stepController.get('selectedService.configs');
      controller.saveConfigs(configs);
      router.transitionTo('step4');
    },
    back: Em.Router.transitionTo('step2')
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router) {
      var controller = router.get('addHawqStandbyWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('4');
        controller.setLowerStepsDisable(4);
        controller.loadAllPriorSteps();
        controller.connectOutlet('addHawqStandbyWizardStep4', controller.get('content'));
      })
    },
    unroutePath: function (router, path) {
      // allow user to leave route if wizard has finished
      if (router.get('addHawqStandbyWizardController').get('isFinished')) {
        this._super(router, path);
      } else {
        return false;
      }
    },
    next: function (router) {
      var controller = router.get('addHawqStandbyWizardController');
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
