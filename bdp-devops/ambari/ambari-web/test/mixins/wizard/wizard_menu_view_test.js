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

var view;

describe('App.WizardMenuMixin', function () {

  beforeEach(function () {
    view = Em.View.create(App.WizardMenuMixin, {
      controller: Em.Object.create({
        isStepDisabled: []
      })
    });
  });

  describe("#isStepDisabled()", function () {
    it("step 1 disabled", function () {
      view.set('controller.isStepDisabled', [Em.Object.create({
        step: 1,
        value: true
      })]);
      expect(view.isStepDisabled(1)).to.be.true;
    });
  });

  describe("#isStep#Disabled", function () {
    var testCases = [
      {
        property: 'isStep0Disabled',
        step: 0
      },
      {
        property: 'isStep1Disabled',
        step: 1
      },
      {
        property: 'isStep2Disabled',
        step: 2
      },
      {
        property: 'isStep3Disabled',
        step: 3
      },
      {
        property: 'isStep4Disabled',
        step: 4
      },
      {
        property: 'isStep5Disabled',
        step: 5
      },
      {
        property: 'isStep6Disabled',
        step: 6
      },
      {
        property: 'isStep7Disabled',
        step: 7
      },
      {
        property: 'isStep8Disabled',
        step: 8
      },
      {
        property: 'isStep9Disabled',
        step: 9
      },
      {
        property: 'isStep10Disabled',
        step: 10
      }
    ];

    testCases.forEach(function (test) {
      it("step" + test.step + " disabled", function () {
        view.set('controller.isStepDisabled', [Em.Object.create({
          step: test.step,
          value: true
        })]);
        view.propertyDidChange(test.property);
        expect(view.get(test.property)).to.be.true;
      });
    }, this);
  });

});