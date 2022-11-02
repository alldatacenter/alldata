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
require('models/cluster_states');
require('controllers/wizard/step0_controller');
var wizardStep0Controller;

describe('App.WizardStep0Controller', function () {

  beforeEach(function() {
    wizardStep0Controller = App.WizardStep0Controller.create({content: {cluster: {}}});
    sinon.stub(App.clusterStatus, 'set', Em.K);
    sinon.stub(App.router, 'send', Em.K);
    App.set('router.nextBtnClickInProgress', false);
  });

  afterEach(function() {
    App.clusterStatus.set.restore();
    App.router.send.restore();
    App.set('router.nextBtnClickInProgress', false);
  });

  describe('#invalidClusterName', function () {
    it('should return true if no cluster name is present', function () {
      wizardStep0Controller.set('hasSubmitted', true);
      wizardStep0Controller.set('content', {'cluster':{'name':''}});
      expect(wizardStep0Controller.get('invalidClusterName')).to.equal(true);
    });
    it('should return true if cluster name contains white spaces', function () {
      wizardStep0Controller.set('hasSubmitted', true);
      wizardStep0Controller.set('content', {'cluster':{'name':'the cluster'}});
      expect(wizardStep0Controller.get('invalidClusterName')).to.equal(true);
    });
    it('should return true if cluster name contains special chars', function () {
      wizardStep0Controller.set('hasSubmitted', true);
      wizardStep0Controller.set('content', {'cluster':{'name':'$cluster'}});
      expect(wizardStep0Controller.get('invalidClusterName')).to.equal(true);
      wizardStep0Controller.set('content', {'cluster':{'name':']^cluster\\'}});
      expect(wizardStep0Controller.get('invalidClusterName')).to.equal(true);
      wizardStep0Controller.set('content', {'cluster':{'name':'[cluster]'}});
      expect(wizardStep0Controller.get('invalidClusterName')).to.equal(true);
    });
    it('should return true if cluster name contains more than 100 symbols', function () {
      wizardStep0Controller.set('hasSubmitted', true);
      wizardStep0Controller.set('content', {'cluster':{'name':'0000-0000-0000-0000-0000-0000-0000-0000-0000-0000-0000-0000-0000-0000-0000-0000-0000-0000-0000-0000-0'}});
      expect(wizardStep0Controller.get('invalidClusterName')).to.equal(true);
    });
    it('should return true if cluster name contains special chars', function () {
      wizardStep0Controller.set('hasSubmitted', true);
      wizardStep0Controller.set('content', {'cluster':{'name':'$cluster'}});
      expect(wizardStep0Controller.get('invalidClusterName')).to.equal(true);
    });
  });

  describe('#loadStep', function() {
    it('should clear step data', function() {
      wizardStep0Controller.loadStep();
      expect(wizardStep0Controller.get('hasSubmitted')).to.equal(false);
      expect(wizardStep0Controller.get('clusterNameError')).to.equal('');
    });
  });

  describe('#submit', function() {
    it('if cluster name is valid should proceed', function() {
      wizardStep0Controller.set('content.cluster.name', 'tdk');
      wizardStep0Controller.submit();
      expect(wizardStep0Controller.get('content.cluster.status')).to.equal('PENDING');
      expect(wizardStep0Controller.get('content.cluster.isCompleted')).to.equal(false);
      expect(App.router.send.calledWith('next')).to.equal(true);
      expect(App.clusterStatus.set.calledWith('clusterName', 'tdk')).to.equal(true);
    });

    it('if cluster name isn\'t valid shouldn\'t proceed', function() {
      wizardStep0Controller.set('content.cluster.name', '@@@@');
      wizardStep0Controller.submit();
      expect(App.router.send.called).to.equal(false);
      expect(App.clusterStatus.set.called).to.equal(false);
    });
    it('if Next button is clicked multiple times before the next step renders, it must not be processed', function() {
      wizardStep0Controller.set('content.cluster.name', 'tdk');
      wizardStep0Controller.submit();
      expect(App.router.send.calledWith('next')).to.equal(true);

      App.router.send.reset();
      wizardStep0Controller.submit();
      expect(App.router.send.called).to.equal(false);
    });
  });

});
