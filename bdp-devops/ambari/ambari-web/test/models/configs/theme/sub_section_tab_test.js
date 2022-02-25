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
require('models/configs/theme/sub_section_tab');

describe('App.SubSectionTab', function () {

  var subSectionTab,
    getModel = function () {
      return App.SubSectionTab.createRecord();
    };

  beforeEach(function () {
    subSectionTab = getModel();
  });

  describe('#visibleProperties', function () {

    var configs = [
      Em.Object.create({
        id: 0,
        isVisible: false,
        hiddenBySection: false
      }),
      Em.Object.create({
        id: 1,
        isVisible: false,
        hiddenBySection: true
      }),
      Em.Object.create({
        id: 2,
        isVisible: true,
        hiddenBySection: false
      }),
      Em.Object.create({
        id: 3,
        isVisible: true,
        hiddenBySection: true
      }),
      Em.Object.create({
        id: 4,
        isVisible: false
      }),
      Em.Object.create({
        id: 5,
        isVisible: true
      })
    ];

    it('should include visible properties from visible sections only', function () {
      subSectionTab.set('configs', configs);
      expect(subSectionTab.get('visibleProperties').mapProperty('id')).to.eql([2, 5]);
    });

  });

  describe('#errorsCount', function () {

    var configs = [
      Em.Object.create({
        isVisible: true,
        isValid: true,
        isValidOverride: true
      }),
      Em.Object.create({
        isVisible: false,
        isValid: true,
        isValidOverride: true
      }),
      Em.Object.create({
        isVisible: true,
        isValid: true,
        isValidOverride: false
      }),
      Em.Object.create({
        isVisible: false,
        isValid: true,
        isValidOverride: false
      }),
      Em.Object.create({
        isVisible: true,
        isValid: false,
        isValidOverride: true
      }),
      Em.Object.create({
        isVisible: false,
        isValid: false,
        isValidOverride: true
      }),
      Em.Object.create({
        isVisible: true,
        isValid: false,
        isValidOverride: false
      }),
      Em.Object.create({
        isVisible: false,
        isValid: false,
        isValidOverride: false
      }),
      Em.Object.create({
        isVisible: true,
        isValid: true
      }),
      Em.Object.create({
        isVisible: false,
        isValid: true
      }),
      Em.Object.create({
        isVisible: true,
        isValid: false
      }),
      Em.Object.create({
        isVisible: false,
        isValid: false
      }),
      Em.Object.create({
        isVisible: true,
        isValidOverride: true
      }),
      Em.Object.create({
        isVisible: false,
        isValidOverride: true
      }),
      Em.Object.create({
        isVisible: true,
        isValidOverride: false
      }),
      Em.Object.create({
        isVisible: false,
        isValidOverride: false
      }),
      Em.Object.create({
        isVisible: true
      }),
      Em.Object.create({
        isVisible: false
      })
    ];

    it('should include visible properties with errors', function () {
      subSectionTab.set('configs', configs);
      expect(subSectionTab.get('errorsCount')).to.be.equal(8);
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
        subSectionTab.set('configs', test.configs);
        expect(subSectionTab.get('isHiddenByFilter')).to.equal(test.e);
      })
    });

  });});