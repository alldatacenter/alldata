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
require('views/common/chart/pie');
require('views/common/configs/services_config');

describe('App.ServiceConfigsByCategoryView', function () {

  var view = App.ServiceConfigsByCategoryView.create({
    serviceConfigs: []
  });

  var testData = [
    {
      title: 'four configs in correct order',
      configs: [
        Em.Object.create({index: 1, resultId: 1}),
        Em.Object.create({index: 2, resultId: 2}),
        Em.Object.create({index: 3, resultId: 3}),
        Em.Object.create({index: 4, resultId: 4})
      ]
    },
    {
      title: 'four configs in reverse order',
      configs: [
        Em.Object.create({index: 4, resultId: 4}),
        Em.Object.create({index: 3, resultId: 3}),
        Em.Object.create({index: 2, resultId: 2}),
        Em.Object.create({index: 1, resultId: 1})
      ]
    },
    {
      title: 'four configs in random order',
      configs: [
        Em.Object.create({index: 3, resultId: 3}),
        Em.Object.create({index: 4, resultId: 4}),
        Em.Object.create({index: 1, resultId: 1}),
        Em.Object.create({index: 2, resultId: 2})
      ]
    },
    {
      title: 'four configs with no index',
      configs: [
        Em.Object.create({resultId: 1}),
        Em.Object.create({resultId: 2}),
        Em.Object.create({resultId: 3}),
        Em.Object.create({resultId: 4})
      ]
    },
    {
      title: 'four configs but one with index',
      configs: [
        Em.Object.create({resultId: 2}),
        Em.Object.create({resultId: 3}),
        Em.Object.create({resultId: 4}),
        Em.Object.create({index: 1, resultId: 1})
      ]
    },
    {
      title: 'index is null or not number',
      configs: [
        Em.Object.create({index: null, resultId: 3}),
        Em.Object.create({index: 1, resultId: 1}),
        Em.Object.create({index: 2, resultId: 2}),
        Em.Object.create({index: 'a', resultId: 4})
      ]
    },
    {
      title: 'four configs when indexes skipped',
      configs: [
        Em.Object.create({index: 88, resultId: 3}),
        Em.Object.create({index: 67, resultId: 2}),
        Em.Object.create({index: 111, resultId: 4}),
        Em.Object.create({index: 3, resultId: 1})
      ]
    }
  ];

  App.TestAliases.testAsComputedIfThenElse(view, 'isCategoryBodyVisible', 'category.isCollapsed', 'display: none;', 'display: block;');

  describe('#sortByIndex', function () {
    var result = [1, 2, 3, 4];
    testData.forEach(function(_test){
      it(_test.title, function () {
        expect(view.sortByIndex(_test.configs).mapProperty('resultId')).to.deep.equal(result);
      })
    })
  });

  describe('#isShowBlock', function() {
    var tests = [
      {
        categoryConfigs: Em.A([
          Em.Object.create({ isHiddenByFilter: false })
        ]),
        category: {},
        m: 'no configs with widget, filtered properties are visible. Panel should be shown',
        e: true
      },
      {
        categoryConfigs: Em.A([]),
        category: Em.Object.create({ customCanAddProperty: true}),
        m: 'Category with custom properties. Panel shouldn\'t be shown',
        e: false
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ isHiddenByFilter: false })
        ]),
        category: Em.Object.create({ customCanAddProperty: true}),
        m: 'Category with custom properties. Filtered configs are hidden. Panel should be shown',
        e: true
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ isHiddenByFilter: true })
        ]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'Filtered configs are hidden. Category not for custom properties. Panel should be hidden',
        e: false
      },
      {
        categoryConfigs: Em.A([]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'Category without properties and not for custom configurations. Panel should be hidden',
        e: false
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ widget: {someProp: 'a'}}),
          Em.Object.create({ widget: {someProp: 'b'}})
        ]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'All properties have widgets and category is not custom. Panel should be hidden',
        e: false
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ widget: null }),
          Em.Object.create({ widget: null })
        ]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'All properties have widgets set to `null` and category is not custom. Panel should be hidden',
        e: false
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ widget: {someProp: 'a'} }),
          Em.Object.create({ isHiddenByFilter: true })
        ]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'Category contains mixed properties. Properties are hidden by filter. Panel should be hidden',
        e: false
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ widget: {someProp: 'a'} }),
          Em.Object.create({ isHiddenByFilter: false })
        ]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'Category contains mixed properties. Properties are visible. Panel should be shown',
        e: true
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ widget: {someProp: 'a'} })
        ]),
        isCompareMode: true,
        category: Em.Object.create({ customCanAddProperty: true }),
        m: 'Should hide block in compare mode',
        e: false
      }
    ];

    beforeEach(function () {
      this._view = App.ServiceConfigsByCategoryView.create({
        serviceConfigs: Em.A([])
      });
      sinon.stub(this._view, 'filteredCategoryConfigs', Em.K);
    });

    afterEach(function () {
      this._view.filteredCategoryConfigs.restore();
      this._view.destroy();
    });

    tests.forEach(function(test) {
      it(test.m, function() {
        this._view.reopen({
          category: test.category,
          categoryConfigs: test.categoryConfigs,
          mainView: Em.Object.create({
            columns: []
          }),
          controller: Em.Object.create({
            isCompareMode: test.isCompareMode
          })
        });
        expect(this._view.get('isShowBlock')).to.be.eql(test.e);
      });
    });
  });

  describe('#isSecureConfig', function () {

    var cases = [
      {
        name: 'n0',
        filename: 'f0',
        isSecureConfig: true,
        title: 'secure config'
      },
      {
        name: 'n1',
        filename: 'f1',
        isSecureConfig: false,
        title: 'secure config with the same name is present in another filename'
      },
      {
        name: 'n2',
        filename: 'f2',
        isSecureConfig: false,
        title: 'no configs of the specified filename are secure'
      }
    ];

    before(function () {
      App.config.reopen({
        secureConfigs: [
          {
            name: 'n0',
            filename: 'f0'
          },
          {
            name: 'n1',
            filename: 'f0'
          },
          {
            name: 'n2',
            filename: 'f1'
          }
        ]
      })
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(view.isSecureConfig(item.name, item.filename)).to.equal(item.isSecureConfig);
      });
    });

  });

  describe('#categoryConfigs', function () {
    var result = [1, 2, 3, 4, 5];
    var cases = [
        {
          categoryNname: 'TestCategory',
          serviceConfigs: [
            Em.Object.create({category: "TestCategory", index: 1, name: "a", isVisible: true, resultId: 1}),
            Em.Object.create({category: "TestCategory", index: 2, name: "b", isVisible: true, resultId: 2}),
            Em.Object.create({category: "TestCategory", index: 5, name: "c", isVisible: true, resultId: 5}),
            Em.Object.create({category: "TestCategory", index: 4, name: "d", isVisible: true, resultId: 4}),
            Em.Object.create({category: "TestCategory", index: 3, name: "e", isVisible: true, resultId: 3})
          ],
          title: 'Order by index with no content type'
        },
        {
          categoryNname: 'TestCategory',
          serviceConfigs: [
            Em.Object.create({category: "TestCategory", index: 1, name: "a", isVisible: true, resultId: 1, displayType: 'int'}),
            Em.Object.create({category: "TestCategory", index: 2, name: "b", isVisible: true, resultId: 4, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 3, name: "c", isVisible: true, resultId: 2}),
            Em.Object.create({category: "TestCategory", index: 4, name: "d", isVisible: true, resultId: 5, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 5, name: "e", isVisible: true, resultId: 3})
          ],
          title: 'Order configs by index and display type equal to content'
        },
        {
          categoryNname: 'TestCategory',
          serviceConfigs: [
            Em.Object.create({category: "TestCategory", name: "a", isVisible: true, resultId: 1, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", name: "b", isVisible: true, resultId: 2, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", name: "c", isVisible: true, resultId: 3, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", name: "d", isVisible: true, resultId: 4, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", name: "e", isVisible: true, resultId: 5, displayType: 'content'})
          ],
          title: 'Order configs by display type equal to content - so they will be sorted alphabetically'
        },
        {
          categoryNname: 'TestCategory',
          serviceConfigs: [
            Em.Object.create({category: "TestCategory", index: 5, name: "a", isVisible: true, resultId: 1, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 4, name: "b", isVisible: true, resultId: 2, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 3, name: "c", isVisible: true, resultId: 3, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 2, name: "d", isVisible: true, resultId: 4, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 1, name: "e", isVisible: true, resultId: 5, displayType: 'content'})
          ],
          title: 'Order configs by display type equal to content - so they will be sorted alphabetically not by index'
        }
      ];

    cases.forEach(function (item) {
      it(item.title, function () {
        view = App.ServiceConfigsByCategoryView.create({
          category: {
            name: item.categoryNname
          },
          serviceConfigs: item.serviceConfigs,
          filteredCategoryConfigs: Em.K,
          collapseCategory: Em.K
        });
        view.setCategoryConfigsAll();
        view.setVisibleCategoryConfigs();
        expect(view.get('categoryConfigs').mapProperty('resultId')).to.deep.equal(result);
        view.destroy();
      });
    });
  });

});
