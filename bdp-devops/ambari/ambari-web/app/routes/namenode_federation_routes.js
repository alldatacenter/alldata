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
  route: '/NameNode/federation',

  breadcrumbs: {
    label: Em.I18n.t('admin.nameNodeFederation.wizard.header')
  },

  enter: function (router, transition) {
    var nameNodeFederationWizardController = router.get('nameNodeFederationWizardController');
    nameNodeFederationWizardController.dataLoading().done(function () {
      //Set HDFS as current service
      App.router.set('mainServiceItemController.content', App.Service.find().findProperty('serviceName', 'HDFS'));
      App.router.get('updateController').set('isWorking', false);
      var popup = App.ModalPopup.show({
        classNames: ['wizard-modal-wrapper'],
        modalDialogClasses: ['modal-xlg'],
        header: Em.I18n.t('admin.nameNodeFederation.wizard.header'),
        bodyClass: App.NameNodeFederationWizardView.extend({
          controller: nameNodeFederationWizardController
        }),
        primary: Em.I18n.t('form.cancel'),
        showFooter: false,
        secondary: null,

        onClose: function () {
          var nameNodeFederationWizardController = router.get('nameNodeFederationWizardController'),
            currStep = nameNodeFederationWizardController.get('currentStep');
          App.showConfirmationPopup(function () {
            nameNodeFederationWizardController.resetOnClose(nameNodeFederationWizardController, 'main.services.index');
          }, Em.I18n.t(parseInt(currStep) === 4 ? 'admin.nameNodeFederation.closePopup2' : 'admin.nameNodeFederation.closePopup'));
        },
        didInsertElement: function () {
          this._super();
          this.fitHeight();
        }
      });
      nameNodeFederationWizardController.set('popup', popup);
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        switch (currentClusterStatus.clusterState) {
          case 'NN_FEDERATION_DEPLOY' :
            nameNodeFederationWizardController.setCurrentStep(currentClusterStatus.localdb.NameNodeFederationWizard.currentStep);
            break;
          default:
            var currStep = App.router.get('nameNodeFederationWizardController.currentStep');
            nameNodeFederationWizardController.setCurrentStep(currStep);
            break;
        }
      }
      Em.run.next(function () {
        App.router.get('wizardWatcherController').setUser(nameNodeFederationWizardController.get('name'));
        router.transitionTo('step' + nameNodeFederationWizardController.get('currentStep'));
      });
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      var controller = router.get('nameNodeFederationWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('1');
        controller.connectOutlet('nameNodeFederationWizardStep1', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('nameNodeFederationWizardController');
      controller.saveNameServiceId(router.get('nameNodeFederationWizardStep1Controller.content.nameServiceId'));
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      var controller = router.get('nameNodeFederationWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('2');
        controller.loadAllPriorSteps();
        controller.connectOutlet('nameNodeFederationWizardStep2', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var wizardController = router.get('nameNodeFederationWizardController');
      var stepController = router.get('nameNodeFederationWizardStep2Controller');
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
      var controller = router.get('nameNodeFederationWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('3');
        controller.loadAllPriorSteps();
        controller.connectOutlet('nameNodeFederationWizardStep3', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('nameNodeFederationWizardController');
      var stepController = router.get('nameNodeFederationWizardStep3Controller');
      controller.saveServiceConfigProperties(stepController);
      router.transitionTo('step4');
    },
    back: Em.Router.transitionTo('step2')
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router) {
      var controller = router.get('nameNodeFederationWizardController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('4');
        controller.setLowerStepsDisable(4);
        controller.loadAllPriorSteps();
        controller.connectOutlet('nameNodeFederationWizardStep4', controller.get('content'));
      })
    },
    unroutePath: function (router, path) {
      // allow user to leave route if wizard has finished
      if (router.get('nameNodeFederationWizardController').get('isFinished')) {
        this._super(router, path);
      } else {
        return false;
      }
    },
    next: function (router) {
      var controller = router.get('nameNodeFederationWizardController');
      controller.resetOnClose(controller, 'main.services.index');
    }
  })

});
