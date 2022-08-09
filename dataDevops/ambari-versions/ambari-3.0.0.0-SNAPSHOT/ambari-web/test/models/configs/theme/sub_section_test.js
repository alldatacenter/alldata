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
var model;

function getModel() {
  return App.SubSection.createRecord();
}

describe('App.SubSection', function () {

  beforeEach(function () {
    model = getModel();
  });

  App.TestAliases.testAsComputedAnd(getModel(), 'showTabs', ['hasTabs', 'someSubSectionTabIsVisible']);

  App.TestAliases.testAsComputedAnd(getModel(), 'addLeftVerticalSplitter', ['!isFirstColumn', 'leftVerticalSplitter']);

  App.TestAliases.testAsComputedAnd(getModel(), 'showTopSplitter', ['!isFirstRow', '!border']);

  App.TestAliases.testAsComputedAnd(getModel(), 'isSectionVisible', ['!isHiddenByFilter', '!isHiddenByConfig', 'someConfigIsVisible']);

  App.TestAliases.testAsComputedFilterBy(getModel(), 'visibleTabs', 'subSectionTabs', 'isVisible', true);

  describe('#errorsCount', function () {

    beforeEach(function () {
      model.set('configs', [
        App.ServiceConfigProperty.create({isValid: true}),
        App.ServiceConfigProperty.create({isValid: false}),
        App.ServiceConfigProperty.create({isValid: false}),
        App.ServiceConfigProperty.create({isValid: false}),
      ]);
    });

    it('should use configs.@each.isValid', function () {
      expect(model.get('errorsCount')).to.equal(3);
    });

    it('should use configs.@each.isValidOverride', function() {
      // original value is valid
      var validOriginalSCP = model.get('configs').objectAt(0);
      // add override with not valid value
      validOriginalSCP.set('isValidOverride', false);
      validOriginalSCP.set('isValid', true);
      expect(model.get('errorsCount')).to.equal(3);
    });

  });

  describe('#isHiddenByFilter', function () {

    Em.A([
        {
          configs: [],
          e: false,
          m: 'Can\'t be hidden if there is no configs'
        },
        {
          configs: [Em.Object.create({isHiddenByFilter: true, isVisible: true}), Em.Object.create({isHiddenByFilter: true, isVisible: true})],
          e: true,
          m: 'All configs are hidden'
        },
        {
          configs: [Em.Object.create({isHiddenByFilter: false, isVisible: true}), Em.Object.create({isHiddenByFilter: true, isVisible: true})],
          e: false,
          m: 'Some configs are hidden'
        },
        {
          configs: [Em.Object.create({isHiddenByFilter: false, isVisible: true}), Em.Object.create({isHiddenByFilter: true, isVisible: true})],
          e: false,
          m: 'Some configs are hidden'
        },
        {
          configs: [Em.Object.create({isHiddenByFilter: false, isVisible: true}), Em.Object.create({isHiddenByFilter: false, isVisible: true})],
          e: false,
          m: 'No configs are hidden'
        }
    ]).forEach(function (test) {
        it(test.m, function () {
          model.set('configs', test.configs);
          expect(model.get('isHiddenByFilter')).to.equal(test.e);
        })
      });

  });

});
