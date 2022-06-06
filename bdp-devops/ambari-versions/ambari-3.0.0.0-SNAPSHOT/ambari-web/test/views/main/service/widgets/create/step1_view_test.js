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
require('/views/main/service/widgets/create/step1_view');


describe('App.WidgetWizardStep1View', function () {
  var view;

  beforeEach(function() {
    view = App.WidgetWizardStep1View.create({
      controller: Em.Object.create({
        content: Em.Object.create(),
        loadStep: Em.K
      })
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function () {
      sinon.stub(Em.run, 'later', function (ctx, callback) {
        callback();
      });
      sinon.stub(App, 'tooltip');
      sinon.stub(view.get('controller'), 'loadStep');
      view.didInsertElement();
    });
    afterEach(function () {
      Em.run.later.restore();
      App.tooltip.restore();
      view.get('controller').loadStep.restore();
    });

    it("loadStep should be called", function() {
      expect(view.get('controller').loadStep.calledOnce).to.be.true;
    });

    it("Em.run.later should be called", function() {
      expect(Em.run.later.calledOnce).to.be.true;
    });

    it("App.tooltip should be called", function() {
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });
});
