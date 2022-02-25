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

var router, installerController, currentClusterStatus;

describe('App.RouterRedirections', function () {

  beforeEach(function () {

    installerController = Em.Object.create({
      currentStep: '',
      totalSteps: 11,
      setCurrentStep: function (k) {
        this.set('currentStep', k);
      }
    });
    App.router.get('installerController').setIsStepDisabled.call(installerController);
    installerController.setLowerStepsDisable = App.router.get('installerController').setLowerStepsDisable.bind(installerController);
    installerController.get('isStepDisabled').pushObject(Ember.Object.create({
      step: 0,
      value: true
    }));

    router = Em.Object.create(App.RouterRedirections, {
      transitionTo: Em.K,
      setAuthenticated: Em.K,
      installerController: installerController
    });

    currentClusterStatus = {};

    sinon.spy(router, 'transitionTo');
    sinon.spy(router, 'setAuthenticated');
    sinon.stub(App, 'get').withArgs('router').returns(router);

  });

  afterEach(function () {
    router.transitionTo.restore();
    router.setAuthenticated.restore();
    App.get.restore();
    currentClusterStatus = {};
  });

  describe('#redirectToInstaller', function () {

    it('CLUSTER_NOT_CREATED_1. user is on installer', function () {
      currentClusterStatus.clusterState = 'CLUSTER_NOT_CREATED_1';
      installerController.set('currentStep', '4');
      router.redirectToInstaller(router, currentClusterStatus, true);
      expect(router.transitionTo.calledWith('step4')).to.be.true;
    });

    it('CLUSTER_NOT_CREATED_1. user is not on installer', function () {
      currentClusterStatus.clusterState = 'CLUSTER_NOT_CREATED_1';
      installerController.set('currentStep', '4');
      router.redirectToInstaller(router, currentClusterStatus, false);
      expect(router.transitionTo.calledWith('installer.step4')).to.be.true;
    });

    it('CLUSTER_DEPLOY_PREP_2. user is on installer', function () {
      currentClusterStatus.clusterState = 'CLUSTER_DEPLOY_PREP_2';
      router.redirectToInstaller(router, currentClusterStatus, true);
      expect(router.transitionTo.calledWith('step8')).to.be.true;
      expect(router.setAuthenticated.calledWith(true)).to.be.true;
    });

    it('CLUSTER_DEPLOY_PREP_2. user is not on installer', function () {
      currentClusterStatus.clusterState = 'CLUSTER_DEPLOY_PREP_2';
      router.redirectToInstaller(router, currentClusterStatus, false);
      expect(router.transitionTo.calledWith('installer.step8')).to.be.true;
      expect(router.setAuthenticated.calledWith(true)).to.be.true;
    });

    it('CLUSTER_INSTALLING_3. user is on installer', function () {
      currentClusterStatus.clusterState = 'CLUSTER_INSTALLING_3';
      router.redirectToInstaller(router, currentClusterStatus, true);
      expect(router.transitionTo.calledWith('step9')).to.be.true;
    });

    it('CLUSTER_INSTALLING_3. user is not on installer', function () {
      currentClusterStatus.clusterState = 'CLUSTER_INSTALLING_3';
      router.redirectToInstaller(router, currentClusterStatus, false);
      expect(router.transitionTo.calledWith('installer.step9')).to.be.true;
    });

    it('SERVICE_STARTING_3. user is on installer', function () {
      currentClusterStatus.clusterState = 'SERVICE_STARTING_3';
      router.redirectToInstaller(router, currentClusterStatus, true);
      expect(router.transitionTo.calledWith('step9')).to.be.true;
    });

    it('SERVICE_STARTING_3. user is not on installer', function () {
      currentClusterStatus.clusterState = 'SERVICE_STARTING_3';
      router.redirectToInstaller(router, currentClusterStatus, false);
      expect(router.transitionTo.calledWith('installer.step9')).to.be.true;
    });

    it('CLUSTER_INSTALLED_4. user is on installer', function () {
      currentClusterStatus.clusterState = 'CLUSTER_INSTALLED_4';
      router.redirectToInstaller(router, currentClusterStatus, true);
      expect(router.transitionTo.calledWith('step10')).to.be.true;
      expect(router.setAuthenticated.calledWith(true)).to.be.true;
    });

    it('CLUSTER_INSTALLED_4. user is not on installer', function () {
      currentClusterStatus.clusterState = 'CLUSTER_INSTALLED_4';
      router.redirectToInstaller(router, currentClusterStatus, false);
      expect(router.transitionTo.calledWith('installer.step10')).to.be.true;
      expect(router.setAuthenticated.calledWith(true)).to.be.true;
    });

  });

});