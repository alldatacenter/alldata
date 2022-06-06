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
require('views/main/admin/stack_upgrade/upgrade_group_view');

describe('App.upgradeGroupView', function () {
  var view;

  beforeEach(function () {
    view = App.upgradeGroupView.create({
      content: Em.Object.create({}),
      failedStatuses: ['FAILED']
    });
  });

  afterEach(function () {
    clearTimeout(view.get('timer'));
    clearTimeout(view.get('upgradeItemTimer'));
    view.destroy();
  });

  describe("#toggleExpanded()", function () {
    var data;
    beforeEach(function () {
      sinon.stub(view, 'collapseLowerLevels', Em.K);
      data = {
        context: Em.Object.create({
          isExpanded: true
        }),
        contexts: [
          [],
          [
            Em.Object.create({
              isExpanded: true
            })
          ]
        ]
      };
      view.toggleExpanded(data);
    });
    afterEach(function () {
      view.collapseLowerLevels.restore();
    });
    it("collapseLowerLevels called twice", function () {
      expect(view.collapseLowerLevels.calledTwice).to.be.true;
    });
    it("context.isExpanded is false", function () {
      expect(data.context.get('isExpanded')).to.be.false;
    });
    it("contexts[1][0].isExpanded is false", function () {
      expect(data.contexts[1][0].get('isExpanded')).to.be.false;
    });
  });

  describe("#collapseLowerLevels()", function () {
    beforeEach(function () {
      sinon.spy(view, 'collapseLowerLevels');
    });
    afterEach(function () {
      view.collapseLowerLevels.restore();
    });
    it("isExpanded false", function () {
      var data = Em.Object.create({
        isExpanded: false
      });
      view.collapseLowerLevels(data);
      expect(view.collapseLowerLevels.calledOnce).to.be.true;
      expect(data.get('isExpanded')).to.be.false;
    });
    it("ITEM expanded", function () {
      var data = Em.Object.create({
        isExpanded: true,
        type: 'ITEM',
        tasks: [
          Em.Object.create({
            isExpanded: true
          })
        ]
      });
      view.collapseLowerLevels(data);
      expect(view.collapseLowerLevels.calledOnce).to.be.true;
      expect(data.get('tasks')[0].get('isExpanded')).to.be.false;
    });
    it("GROUP expanded", function () {
      var data = Em.Object.create({
        isExpanded: true,
        type: 'GROUP',
        upgradeItems: [
          Em.Object.create({
            isExpanded: true
          })
        ]
      });
      view.collapseLowerLevels(data);
      expect(view.collapseLowerLevels.calledTwice).to.be.true;
      expect(data.get('upgradeItems')[0].get('isExpanded')).to.be.false;
    });
  });

});