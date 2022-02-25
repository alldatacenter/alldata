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
require('views/wizard/step0_view');

var view, controller = Em.Object.create({
  clusterNameError: '',
  loadStep: Em.K
});

describe('App.WizardStep0View', function () {

  beforeEach(function () {
    view = App.WizardStep0View.create({'controller': controller});
  });

  describe('#onError', function () {
    it('should be true if clusterNameError appears', function () {
      controller.set('clusterNameError', 'ERROR');
      expect(view.get('onError')).to.equal(true);
    });
    it('should be false if clusterNameError doesn\'t appears', function () {
      controller.set('clusterNameError', '');
      expect(view.get('onError')).to.equal(false);
    });
  });

  describe('#didInsertElement', function () {
    beforeEach(function () {
      sinon.stub(App, 'popover', Em.K);
      sinon.spy(view.get('controller'), 'loadStep');
    });
    afterEach(function () {
      App.popover.restore();
      view.get('controller').loadStep.restore();
    });
    it('should call loadStep', function () {
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.equal(true);
    });
    it('should create popover', function () {
      view.didInsertElement();
      expect(App.popover.calledOnce).to.equal(true);
    });
  });

});

describe('App.WizardStep0ViewClusterNameInput', function () {

  beforeEach(function() {
    view = App.WizardStep0ViewClusterNameInput.create({
      parentView: Em.Object.create({
        controller: Em.Object.create({
          submit: Em.K
        })
      })
    });
  });

  describe('#keyPress', function() {

    beforeEach(function () {
      sinon.spy(view.get('parentView.controller'), 'submit');
    });

    afterEach(function () {
      view.get('parentView.controller').submit.restore();
    });

    it('should return true if pressed not Enter', function() {
      expect(view.keyPress({keyCode: 1})).to.equal(true);
      expect(view.get('parentView.controller').submit.called).to.equal(false);
    });
    it('should submit form if Enter pressed', function() {
      expect(view.keyPress({keyCode: 13})).to.equal(false);
      expect(view.get('parentView.controller').submit.calledOnce).to.equal(true);
    });
  });

});