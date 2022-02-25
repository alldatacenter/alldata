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
require('models/finished_upgrade_entity');

function getModel() {
  return App.finishedUpgradeEntity.create();
}

describe('App.finishedUpgradeEntity', function () {
  var model;

  beforeEach(function () {
    model = getModel();
  });

  App.TestAliases.testAsComputedNotEqual(getModel(), 'isVisible', 'status', 'PENDING');

  describe("#progress", function() {
    it("progress_percent = 1.9", function() {
      model.set('progress_percent', 1.9);
      model.propertyDidChange('progress');
      expect(model.get('progress')).to.equal(1);
    });
    it("progress_percent = 1", function() {
      model.set('progress_percent', 1);
      model.propertyDidChange('progress');
      expect(model.get('progress')).to.equal(1);
    });
  });

  describe("#isActive", function() {
    it("status IN_PROGRESS", function() {
      model.set('status', 'IN_PROGRESS');
      model.propertyDidChange('isActive');
      expect(model.get('isActive')).to.be.true;
    });
    it("status PENDING", function() {
      model.set('status', 'PENDING');
      model.propertyDidChange('isActive');
      expect(model.get('isActive')).to.be.false;
    });
  });

  describe('#isExpandableGroup', function () {

    var cases = [
      {
        input: {
          type: 'ITEM'
        },
        isExpandableGroup: false,
        title: 'not upgrade group'
      },
      {
        input: {
          type: 'GROUP',
          status: 'PENDING',
          hasExpandableItems: false
        },
        isExpandableGroup: false,
        title: 'pending upgrade group without expandable items'
      },
      {
        input: {
          type: 'GROUP',
          status: 'ABORTED',
          hasExpandableItems: false
        },
        isExpandableGroup: true,
        title: 'aborted upgrade group without expandable items'
      },
      {
        input: {
          type: 'GROUP',
          status: 'ABORTED',
          hasExpandableItems: true
        },
        isExpandableGroup: true,
        title: 'aborted upgrade group with expandable items'
      },
      {
        input: {
          type: 'GROUP',
          status: 'IN_PROGRESS',
          hasExpandableItems: false
        },
        isExpandableGroup: true,
        title: 'active upgrade group'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        model.setProperties(item.input);
        expect(model.get('isExpandableGroup')).to.equal(item.isExpandableGroup);
      });
    });

  });

  describe('#upgradeGroupStatus', function () {

    var cases = [
      {
        input: {
          type: 'ITEM',
          upgradeSuspended: false
        },
        upgradeGroupStatus: undefined,
        title: 'not upgrade group'
      },
      {
        input: {
          type: 'GROUP',
          status: 'PENDING',
          hasExpandableItems: false,
          upgradeSuspended: false
        },
        upgradeGroupStatus: 'PENDING',
        title: 'pending upgrade group'
      },
      {
        input: {
          type: 'GROUP',
          status: 'PENDING',
          hasExpandableItems: true,
          upgradeSuspended: false
        },
        upgradeGroupStatus: 'SUBITEM_FAILED',
        title: 'pending upgrade group with expandable items'
      },
      {
        input: {
          type: 'GROUP',
          status: 'ABORTED',
          hasExpandableItems: false,
          upgradeSuspended: false
        },
        upgradeGroupStatus: 'ABORTED',
        title: 'aborted upgrade group with expandable items'
      },
      {
        input: {
          type: 'GROUP',
          status: 'ABORTED',
          hasExpandableItems: true,
          upgradeSuspended: true
        },
        upgradeGroupStatus: 'ABORTED',
        title: 'aborted upgrade group with expandable items'
      },
      {
        input: {
          type: 'GROUP',
          status: 'IN_PROGRESS',
          hasExpandableItems: false,
          upgradeSuspended: false
        },
        upgradeGroupStatus: 'IN_PROGRESS',
        title: 'active upgrade'
      }
    ];

    beforeEach(function() {
      this.mock = sinon.stub(App, 'get');
    });
    afterEach(function() {
      this.mock.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        this.mock.returns(item.input.upgradeSuspended);
        model.setProperties(item.input);
        expect(model.get('upgradeGroupStatus')).to.equal(item.upgradeGroupStatus);
      });
    });

  });
});