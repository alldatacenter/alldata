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
require('views/main/menu');

describe('App.MainMenuView', function () {
  var view;

  beforeEach(function () {
    view = App.MainSideMenuView.create();
  });

  describe("#itemViewClass", function () {
    var itemViewClass;

    beforeEach(function () {
      itemViewClass = view.get('itemViewClass').create({
        content: Em.Object.create({
          routing: ['firstroute']
        })
      });
    });

    describe("#isViewsItem", function () {

      it("should be false", function() {
        itemViewClass.set('content.routing', []);
        itemViewClass.propertyDidChange('isViewsItem');
        expect(itemViewClass.get('isViewsItem')).to.be.false;
      });

      it("should be true", function() {
        itemViewClass.set('content.routing', ['views']);
        itemViewClass.propertyDidChange('isViewsItem');
        expect(itemViewClass.get('isViewsItem')).to.be.true;
      });
    });

    describe("#goToSection()", function () {

      beforeEach(function() {
        sinon.stub(App.router, 'set');
        sinon.stub(App.router, 'route');
      });

      afterEach(function() {
        App.router.set.restore();
        App.router.route.restore();
      });

      it("should route to main", function() {
        itemViewClass.goToSection({context: ''});
        expect(App.router.route.calledWith('main/')).to.be.true;
      });

      it("should route to hosts", function() {
        itemViewClass.goToSection({context: 'hosts'});
        expect(App.router.set.calledWith('mainHostController.showFilterConditionsFirstLoad', false)).to.be.true;
      });

      it("should route to views", function() {
        itemViewClass.goToSection({context: 'views'});
        expect(App.router.route.calledWith('views')).to.be.true;
      });

      it("should route to alerts", function() {
        itemViewClass.goToSection({context: 'alerts'});
        expect(App.router.set.calledWith('mainAlertDefinitionsController.showFilterConditionsFirstLoad', false)).to.be.true;
      });
    });

    describe("#AdminDropdownItemView", function () {
      var adminDropdownItemView;

      beforeEach(function () {
        adminDropdownItemView = itemViewClass.get('AdminDropdownItemView').create({
          parentView: Em.Object.create({
            dropdownCategories: [
              {name: 'cat1', disabled: true},
              {name: 'cat2', disabled: false}
            ],
            content: Em.Object.create()
          })
        });
      });

      describe("#isDisabled", function () {

        it("should be true", function () {
          adminDropdownItemView.set('item', 'cat1');
          adminDropdownItemView.propertyDidChange('isDisabled');
          expect(adminDropdownItemView.get('isDisabled')).to.be.true;
        });

        it("should be false", function () {
          adminDropdownItemView.set('item', 'cat2');
          adminDropdownItemView.propertyDidChange('isDisabled');
          expect(adminDropdownItemView.get('isDisabled')).to.be.false;
        });
      });

      describe("#goToCategory()", function () {
        var testCases = [
          {
            routing: '',
            isActive: true,
            isDisabled: true,
            expected: false
          },
          {
            routing: 'admin',
            isActive: true,
            isDisabled: true,
            expected: false
          },
          {
            routing: 'admin',
            isActive: false,
            isDisabled: true,
            expected: false
          },
          {
            routing: 'admin',
            isActive: false,
            isDisabled: false,
            expected: true
          }
        ];

        beforeEach(function() {
          sinon.stub(App.router, 'route');
        });

        afterEach(function() {
          App.router.route.restore();
        });

        testCases.forEach(function(test) {
          it("routing=" + test.routing +
             " isActive=" + test.isActive +
             " isDisabled=" + test.isDisabled, function() {
            adminDropdownItemView.set('parentView.content.routing', test.routing);
            adminDropdownItemView.reopen({
              isActive: test.isActive,
              isDisabled: test.isDisabled
            });
            adminDropdownItemView.goToCategory({context: 'context'});
            expect(App.router.route.called).to.be.equal(test.expected);
          });
        });
      });
    });
  });
});
