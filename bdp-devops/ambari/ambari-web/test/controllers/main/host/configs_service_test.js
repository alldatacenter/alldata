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
require('controllers/main/host/configs_service');


describe('App.MainHostServiceConfigsController', function () {

  var controller = App.MainHostServiceConfigsController.create({
    host: Em.Object.create()
  });


  describe('#filterServiceConfigs()', function () {
    var testCases = [
      {
        title: 'configCategories is empty',
        content: {
          configCategories: [],
          hostComponents: []
        },
        result: []
      },
      {
        title: 'Category hostComponentNames is null',
        content: {
          configCategories: [
            Em.Object.create({hostComponentNames: null})
          ],
          hostComponents: []
        },
        result: [
          Em.Object.create({hostComponentNames: null})
        ]
      },
      {
        title: 'Components of host are empty',
        content: {
          configCategories: [
            Em.Object.create({hostComponentNames: ['comp1']})
          ],
          hostComponents: []
        },
        result: []
      },
      {
        title: 'Host components do not match component of categories',
        content: {
          configCategories: [
            Em.Object.create({hostComponentNames: ['comp1']})
          ],
          hostComponents: [
            {
              componentName: 'comp2'
            }
          ]
        },
        result: []
      },
      {
        title: 'Host components match component of categories',
        content: {
          configCategories: [
            Em.Object.create({hostComponentNames: ['comp1']})
          ],
          hostComponents: [
            {
              componentName: 'comp1'
            }
          ]
        },
        result: [
          Em.Object.create({hostComponentNames: ['comp1']})
        ]
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('host.hostComponents', test.content.hostComponents);
        expect(controller.filterServiceConfigs(test.content.configCategories)).to.eql(test.result);
      });
    });
  });

	describe("#loadStep()", function () {
    beforeEach(function() {
      sinon.stub(App.themesMapper, 'generateAdvancedTabs', Em.K);
      sinon.stub(controller, 'loadCurrentVersions', Em.K);
      sinon.stub(controller, 'loadConfigTheme', function() {
        return { always: Em.K };
      });
      sinon.stub(controller, 'trackRequest');
    });
    afterEach(function() {
      controller.loadCurrentVersions.restore();
      controller.loadConfigTheme.restore();
      App.themesMapper.generateAdvancedTabs.restore();
      controller.trackRequest.restore();
    });
		it("should set host", function () {
			controller.set('content', {
				host: 'host1',
        dependentServiceNames: []
			});
			controller.loadStep();
			expect(controller.get('host')).to.be.equal('host1');
		});
	});

	describe("#renderServiceConfigs()", function () {

    beforeEach(function () {
      sinon.stub(controller, 'filterServiceConfigs', function () {
        this._super = Em.K;
      });
    });

    afterEach(function () {
      controller.filterServiceConfigs.restore();
    });

		it("should call filterServiceConfigs", function () {
			var serviceConfigs = {
				configCategories: 'val'
			};
			controller.renderServiceConfigs(serviceConfigs);
			expect(controller.filterServiceConfigs.calledWith('val')).to.be.true;
		});
	});

	describe("#switchHostGroup()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'launchSwitchConfigGroupOfHostDialog', Em.K);
      });

    afterEach(function () {
      controller.launchSwitchConfigGroupOfHostDialog.restore();
    });

		it("should call launchSwitchConfigGroupOfHostDialog", function () {
			controller.set('selectedConfigGroup', {});
			controller.set('configGroups', []);
			controller.set('host', {hostName: 'host1'});
			controller.switchHostGroup();

			expect(controller.launchSwitchConfigGroupOfHostDialog.calledWith({}, [], 'host1')).to.be.true;
		});
	});

});
