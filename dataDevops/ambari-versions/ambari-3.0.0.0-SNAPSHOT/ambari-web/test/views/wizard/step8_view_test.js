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
require('utils/helper');
require('utils/string_utils');
require('views/wizard/step8_view');
var view;

describe('App.WizardStep8View', function() {

  beforeEach(function() {
    view = App.WizardStep8View.create();
  });

  describe('#didInsertElement', function() {

    beforeEach(function () {
      view.set('controller', Em.Object.create({
        loadStep: Em.K
      }));
      sinon.spy(view.get('controller'), 'loadStep');
    });

    afterEach(function () {
      view.get('controller').loadStep.restore();
    });

    it('should call loadStep', function() {
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.equal(true);
    });
  });

  describe('#printReview', function() {

    beforeEach(function() {
      sinon.stub($.fn, 'jqprint', Em.K);
    });

    afterEach(function () {
      $.fn.jqprint.restore();
    });

    it('should call jqprint', function() {
      view.printReview();
      expect($.fn.jqprint.calledOnce).to.equal(true);
    });
  });

});