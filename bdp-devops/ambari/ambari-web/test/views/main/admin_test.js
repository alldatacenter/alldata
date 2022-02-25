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
require('views/main/admin');

describe('App.MainAdminView', function () {

  var view;

  beforeEach(function () {
    view = App.MainAdminView.create({
      controller: Em.Object.create()
    });
  });

  describe.skip('#categories', function () {

    var cases = [
      {
        isHadoopWindowsStack: true,
        categories: [
          {
            name: 'stackAndUpgrade',
            url: 'stackAndUpgrade.index',
            label: Em.I18n.t('admin.stackUpgrade.title')
          },
          {
            name: 'adminServiceAccounts',
            url: 'adminServiceAccounts',
            label: Em.I18n.t('common.serviceAccounts')
          }
        ],
        title: 'HDPWIN'
      },
      {
        isHadoopWindowsStack: false,
        categories: [
          {
            name: 'stackAndUpgrade',
            url: 'stackAndUpgrade.index',
            label: Em.I18n.t('admin.stackUpgrade.title')
          },
          {
            name: 'adminServiceAccounts',
            url: 'adminServiceAccounts',
            label: Em.I18n.t('common.serviceAccounts')
          },
          {
            name: 'kerberos',
            url: 'adminKerberos.index',
            label: Em.I18n.t('common.kerberos')
          }
        ],
        title: 'not HDPWIN'
      }
    ];

    beforeEach(function () {
      this.stub = sinon.stub(App, 'get');
    });

    afterEach(function () {
      App.get.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        this.stub.withArgs('isHadoopWindowsStack').returns(item.isHadoopWindowsStack);
        view.propertyDidChange('categories');
        expect(view.get('categories')).to.eql(item.categories);
      });
    });
  });

  describe("#willDestroyElement()", function() {
    it("controller.category is set to null", function() {
      view.willDestroyElement();
      expect(view.get('controller.category')).to.be.null;
    });
  });

  describe("#NavItemView", function () {
    var navItemView;

    beforeEach(function() {
      navItemView = view.get('NavItemView').create({
        parentView: Em.Object.create()
      });
    });

    describe("#isDisabled", function () {

      it("view should be disabled", function() {
        navItemView.set('parentView.categories', [
          {name: 'cat1', disabled: true},
          {name: 'cat2', disabled: false}
        ]);
        navItemView.set('item', 'cat1');
        expect(navItemView.get('isDisabled')).to.be.true;
      });

      it("view should not be disabled", function() {
        navItemView.set('parentView.categories', [
          {name: 'cat1', disabled: true},
          {name: 'cat2', disabled: false}
        ]);
        navItemView.set('item', 'cat2');
        expect(navItemView.get('isDisabled')).to.be.false;
      });
    });
  });

});