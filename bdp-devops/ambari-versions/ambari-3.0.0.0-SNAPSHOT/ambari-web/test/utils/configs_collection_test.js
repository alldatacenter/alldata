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
require('utils/configs_collection');

describe('App.configsCollection', function () {

  var configsCollection;

  beforeEach(function () {
    configsCollection = Em.Object.create(App.configsCollection);
    sinon.spy(Em, 'assert');
  });

  afterEach(function () {
    Em.assert.restore();
  });

  describe('#add', function () {

    var throwCases = [
      {
        obj: undefined,
        collection: [],
        isError: true,
        title: 'null passed'
      },
      {
        obj: {},
        collection: [],
        isError: true,
        title: 'no id passed'
      },
      {
        obj: undefined,
        collection: [],
        isError: true,
        title: 'no item passed'
      }
    ];

    var cases = [
      {
        collection: [],
        isError: false,
        title: 'initial state'
      },
      {
        obj: {
          id: 1,
          name: 'n10'
        },
        collection: [
          {
            id: 1,
            name: 'n10'
          }
        ],
        mapItem: {
          id: 1,
          name: 'n10'
        },
        isError: false,
        title: 'new item'
      },
      {
        obj: {
          id: 1,
          name: 'n11'
        },
        collection: [
          {
            id: 1,
            name: 'n10'
          }
        ],
        mapItem: {
          id: 1,
          name: 'n11'
        },
        isError: false,
        title: 'duplicate id'
      },
      {
        obj: {
          id: '1',
          name: 'n12'
        },
        collection: [
          {
            id: 1,
            name: 'n10'
          }
        ],
        mapItem: {
          id: '1',
          name: 'n12'
        },
        isError: false,
        title: 'duplicate id, key name conversion'
      }
    ];

    throwCases.forEach(function (item) {

      it(item.title, function () {
        expect(function () {configsCollection.add(item.obj);}).to.throw(Error);
      });

    });

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          if (item.hasOwnProperty('obj')) {
            configsCollection.add(item.obj);
          }
        });

        it('configs array', function () {
          expect(configsCollection.getAll()).to.eql(item.collection);
        });

        if (item.obj && item.obj.id) {
          it('configs map', function () {
            expect(configsCollection.getConfig(item.obj.id)).to.eql(item.mapItem);
          });
        }

      });

    });

  });

  describe('#getConfig', function () {

    var throwCases = [
      {
        result: undefined,
        isError: true,
        title: 'no id passed'
      },
      {
        id: null,
        result: undefined,
        isError: true,
        title: 'invalid id passed'
      }
    ];

    var cases = [
      {
        id: 1,
        result: {
          id: 1
        },
        isError: false,
        title: 'existing item'
      },
      {
        id: 1,
        result: {
          id: 1
        },
        isError: false,
        title: 'existing item, key name conversion'
      },
      {
        id: 2,
        result: undefined,
        isError: false,
        title: 'item doesn\'t exist'
      }
    ];

    throwCases.forEach(function (item) {

      it(item.title, function () {
        configsCollection.add({
          id: 1
        });
        expect(function () {configsCollection.getConfig(item.id);}).to.throw(Error);
      });

    });

    cases.forEach(function (item) {

      describe(item.title, function () {

        var result;

        beforeEach(function () {
          configsCollection.add({
            id: 1
          });
          result = configsCollection.getConfig(item.id);
        });

        it('returned value', function () {
          expect(result).to.eql(item.result);
        });

      });

    });

  });

  describe('#getConfigByName', function () {

    var configIds = ['n0_f0', 'n1_f1'];
    var throwCases = [
      {
        fileName: 'f0',
        result: undefined,
        isError: true,
        title: 'no name passed'
      },
      {
        name: 'n0',
        result: undefined,
        isError: true,
        title: 'no filename passed'
      }
    ];
    var cases = [
        {
          name: 'n0',
          fileName: 'f0',
          result: {
            id: 'n0_f0'
          },
          isError: false,
          title: 'existing item'
        },
        {
          name: 'n0',
          fileName: 'f1',
          result: undefined,
          isError: false,
          title: 'not existing item'
        }
      ];

    beforeEach(function () {
      sinon.stub(App.config, 'configId', function (name, fileName) {
        return name + '_' + fileName;
      });
    });

    afterEach(function () {
      configsCollection.clearAll();
      App.config.configId.restore();
    });

    throwCases.forEach(function (item) {

      it(item.title, function () {
        expect(function () {configsCollection.getConfigByName(item.name, item.fileName);}).to.throw(Error);
      });

    });

    cases.forEach(function (item) {

      describe(item.title, function () {

        var result;

        beforeEach(function () {

          configIds.forEach(function (id) {
            configsCollection.add({
              id: id
            });
          });
          result = configsCollection.getConfigByName(item.name, item.fileName);
        });

        it('returned value', function () {
          expect(result).to.eql(item.result);
        });

      });

    });

  });

  describe('#getAll', function () {

    var configs = [
      {
        id: 'c0'
      },
      {
        id: 'c1'
      }
    ];

    beforeEach(function () {
      configsCollection.clearAll();
    });

    it('should return all configs', function () {
      configs.forEach(function (item) {
        configsCollection.add(item);
      });
      expect(configsCollection.getAll()).to.eql(configs);
    });

  });


  describe('#clearAll', function () {

    beforeEach(function () {
      configsCollection.add({
        id: 'c0'
      });
      configsCollection.clearAll();
    });

    it('should clear configs array', function () {
      expect(configsCollection.getAll()).to.have.length(0);
    });

    it('should clear configs map', function () {
      expect(configsCollection.getConfig('c0')).to.be.undefined;
    });

  });

});
