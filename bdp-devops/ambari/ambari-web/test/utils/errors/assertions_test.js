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
require('utils/errors/assertions');

describe('Error assertions', function () {
  describe('#App.assert', function () {
    var errorCases = [
      {
        args: [null, false],
        errorType: Error,
        desc: '',
        title: 'default error object, no description'
      },
      {
        args: ['desc0', false],
        errorType: Error,
        desc: ' Info:  desc0',
        title: 'default error object, custom description'
      },
      {
        args: [null, false, TypeError],
        errorType: TypeError,
        desc: '',
        title: 'custom error object, no description'
      },
      {
        args: ['desc1', false, TypeError],
        errorType: TypeError,
        desc: ' Info:  desc1',
        title: 'custom error object, custom description'
      }
    ];
    errorCases.forEach(function (test) {
      it(test.title, function () {
        var testFunction = function () {
          App.assert.apply(null, test.args);
        };
        expect(testFunction).to.throw(test.errorType, test.desc);
      });
    });
    it('no error', function () {
      expect(App.assert.bind(null, 'desc2', true)).to.not.throw();
    });
  });

  describe('#App.assertExists', function () {
    var errorCases = [
        {
          value: null,
          desc: 'desc0',
          title: 'null'
        },
        {
          value: undefined,
          desc: 'desc1',
          title: 'undefined'
        }
      ],
      noErrorCases = [
        {
          value: [],
          title: 'empty array'
        },
        {
          value: [0],
          title: 'non-empty array'
        },
        {
          value: {},
          title: 'empty object'
        },
        {
          value: {
            a: ''
          },
          title: 'non-empty object'
        },
        {
          value: Em.Object.create(),
          title: 'empty Ember object'
        },
        {
          value: Em.Object.create({
            a: ''
          }),
          title: 'non-empty Ember object'
        },
        {
          value: 0,
          title: '0'
        },
        {
          value: 1,
          title: 'non-zero number'
        },
        {
          value: NaN,
          title: 'NaN'
        },
        {
          value: Infinity,
          title: 'Infinity'
        },
        {
          value: false,
          title: 'false'
        },
        {
          value: true,
          title: 'true'
        },
        {
          value: '',
          title: 'empty string'
        },
        {
          value: '0',
          title: 'non-empty string'
        },
        {
          value: ' ',
          title: 'spaces-only string'
        },
        {
          value: new Number(0),
          title: 'number object'
        },
        {
          value: new String(''),
          title: 'string object'
        },
        {
          value: new Boolean(false),
          title: 'boolean object'
        },
        {
          value: new Number(1),
          title: 'object of non-zero number'
        },
        {
          value: new String('a'),
          title: 'object of non-empty string'
        },
        {
          value: new Boolean(true),
          title: 'object of true boolean'
        }
      ];
    errorCases.forEach(function (test) {
      it(test.title, function () {
        expect(App.assertExists.bind(null, test.value, test.desc)).to.throw(App.NotNullTypeError, test.desc);
      });
    });
    noErrorCases.forEach(function (test) {
      it(test.title, function () {
        expect(App.assertExists.bind(null, test.value, 'desc')).to.not.throw();
      });
    });
  });

  describe('#App.assertObject', function () {
    var errorCases = [
        {
          value: null,
          desc: 'desc0',
          title: 'null'
        },
        {
          value: undefined,
          desc: 'desc1',
          title: 'undefined'
        },
        {
          value: 1,
          desc: 'desc2',
          title: 'number'
        },
        {
          value: NaN,
          desc: 'desc3',
          title: 'NaN'
        },
        {
          value: Infinity,
          desc: 'desc4',
          title: 'Infinity'
        },
        {
          value: true,
          desc: 'desc5',
          title: 'boolean'
        },
        {
          value: 'a',
          desc: 'desc6',
          title: 'string'
        },
        {
          value: function () {
          },
          desc: 'desc7',
          title: 'function'
        }
      ],
      noErrorCases = [
        {
          value: [],
          title: 'array'
        },
        {
          value: {},
          title: 'object'
        },
        {
          value: new Date(),
          title: 'date'
        },
        {
          value: new RegExp(),
          title: 'regexp'
        },
        {
          value: Em.Object.create(),
          title: 'Ember object'
        },
        {
          value: Em.View.create(),
          title: 'extended Ember object'
        },
        {
          value: new Error(),
          title: 'error object'
        },
        {
          value: new Number(1),
          title: 'number object'
        },
        {
          value: new String('a'),
          title: 'string object'
        },
        {
          value: new Boolean(true),
          title: 'boolean object'
        }
      ];
    errorCases.forEach(function (test) {
      it(test.title, function () {
        expect(App.assertObject.bind(null, test.value, test.desc)).to.throw(App.ObjectTypeError, test.desc);
      });
    });
    noErrorCases.forEach(function (test) {
      it(test.title, function () {
        expect(App.assertObject.bind(null, test.value, 'desc')).to.not.throw();
      });
    });
  });

  describe('#App.assertEmberObject', function () {
    var errorCases = [
        {
          value: null,
          desc: 'desc0',
          title: 'null'
        },
        {
          value: undefined,
          desc: 'desc1',
          title: 'undefined'
        },
        {
          value: 1,
          desc: 'desc2',
          title: 'number'
        },
        {
          value: NaN,
          desc: 'desc3',
          title: 'NaN'
        },
        {
          value: Infinity,
          desc: 'desc4',
          title: 'Infinity'
        },
        {
          value: true,
          desc: 'desc5',
          title: 'boolean'
        },
        {
          value: 'a',
          desc: 'desc6',
          title: 'string'
        },
        {
          value: function () {
          },
          desc: 'desc7',
          title: 'function'
        },
        {
          value: [],
          desc: 'desc8',
          title: 'array'
        },
        {
          value: {},
          desc: 'desc9',
          title: 'object'
        },
        {
          value: new Date(),
          desc: 'desc10',
          title: 'date'
        },
        {
          value: new RegExp(),
          desc: 'desc11',
          title: 'regexp'
        },
        {
          value: new Error(),
          desc: 'desc12',
          title: 'error object'
        },
        {
          value: Em.Object,
          desc: 'desc13',
          title: 'Ember object constructor'
        },
        {
          value: Em.View,
          desc: 'desc14',
          title: 'extended Ember object constructor'
        },
        {
          value: new Number(1),
          desc: 'desc15',
          title: 'number object'
        },
        {
          value: new String('a'),
          desc: 'desc16',
          title: 'string object'
        },
        {
          value: new Boolean(true),
          desc: 'desc17',
          title: 'boolean object'
        }
      ],
      noErrorCases = [
        {
          value: Em.Object.create(),
          title: 'Ember object'
        },
        {
          value: Em.View.create(),
          title: 'extended Ember object'
        }
      ];
    errorCases.forEach(function (test) {
      it(test.title, function () {
        expect(App.assertEmberObject.bind(null, test.value, test.desc)).to.throw(App.EmberObjectTypeError, test.desc);
      });
    });
    noErrorCases.forEach(function (test) {
      it(test.title, function () {
        expect(App.assertEmberObject.bind(null, test.value, 'desc')).to.not.throw();
      });
    });
  });

  describe('#App.assertArray', function () {
    var errorCases = [
        {
          value: null,
          desc: 'desc0',
          title: 'null'
        },
        {
          value: undefined,
          desc: 'desc1',
          title: 'undefined'
        },
        {
          value: 1,
          desc: 'desc2',
          title: 'number'
        },
        {
          value: NaN,
          desc: 'desc3',
          title: 'NaN'
        },
        {
          value: Infinity,
          desc: 'desc4',
          title: 'Infinity'
        },
        {
          value: true,
          desc: 'desc5',
          title: 'boolean'
        },
        {
          value: 'a',
          desc: 'desc6',
          title: 'string'
        },
        {
          value: function () {
          },
          desc: 'desc7',
          title: 'function'
        },
        {
          value: {},
          desc: 'desc8',
          title: 'object'
        },
        {
          value: new Date(),
          desc: 'desc9',
          title: 'date'
        },
        {
          value: new RegExp(),
          desc: 'desc10',
          title: 'regexp'
        },
        {
          value: new Error(),
          desc: 'desc11',
          title: 'error object'
        },
        {
          value: Em.Object,
          desc: 'desc12',
          title: 'Ember object constructor'
        },
        {
          value: Em.View,
          desc: 'desc13',
          title: 'extended Ember object constructor'
        },
        {
          value: Em.Object.create(),
          desc: 'desc14',
          title: 'Ember object'
        },
        {
          value: Em.View.create(),
          desc: 'desc15',
          title: 'extended Ember object'
        },
        {
          value: new Number(1),
          desc: 'desc16',
          title: 'number object'
        },
        {
          value: new Boolean(true),
          desc: 'desc17',
          title: 'boolean object'
        },
        {
          value: {
            setInterval: Em.K,
            0: 'a',
            length: 1
          },
          desc: 'desc18',
          title: 'object with setInterval property'
        },
        {
          value: (function () {
            var value = 0;
            value.length = 0;
            return value;
          })(),
          desc: 'desc19',
          title: 'non-object with length property'
        }
      ],
      noErrorCases = [
        {
          value: [],
          title: 'array'
        },
        {
          value: Em.A(),
          title: 'Ember array'
        },
        {
          value: {
            0: 'a',
            length: 1
          },
          title: 'array-like object'
        },
        {
          value: Em.Object.create({
            0: 'a',
            length: 1
          }),
          title: 'array-like Ember object'
        },
        {
          value: new String('a'),
          title: 'string object'
        }
      ];
    errorCases.forEach(function (test) {
      it(test.title, function () {
        expect(App.assertArray.bind(null, test.value, test.desc)).to.throw(App.ArrayTypeError, test.desc);
      });
    });
    noErrorCases.forEach(function (test) {
      it(test.title, function () {
        expect(App.assertArray.bind(null, test.value, 'desc')).to.not.throw();
      });
    });
  });

  describe('#App.assertFunction', function () {
    var errorCases = [
        {
          value: null,
          desc: 'desc0',
          title: 'null'
        },
        {
          value: undefined,
          desc: 'desc1',
          title: 'undefined'
        },
        {
          value: 1,
          desc: 'desc2',
          title: 'number'
        },
        {
          value: NaN,
          desc: 'desc3',
          title: 'NaN'
        },
        {
          value: Infinity,
          desc: 'desc4',
          title: 'Infinity'
        },
        {
          value: true,
          desc: 'desc5',
          title: 'boolean'
        },
        {
          value: 'a',
          desc: 'desc6',
          title: 'string'
        },
        {
          value: [],
          desc: 'desc7',
          title: 'array'
        },
        {
          value: {},
          desc: 'desc8',
          title: 'object'
        },
        {
          value: new Date(),
          desc: 'desc9',
          title: 'date'
        },
        {
          value: new RegExp(),
          desc: 'desc10',
          title: 'regexp'
        },
        {
          value: Em.Object.create(),
          desc: 'desc11',
          title: 'Ember object'
        },
        {
          value: Em.View.create(),
          desc: 'desc12',
          title: 'extended Ember object'
        },
        {
          value: new Error(),
          desc: 'desc13',
          title: 'error object'
        },
        {
          value: new Number(1),
          desc: 'desc14',
          title: 'number object'
        },
        {
          value: new String('a'),
          desc: 'desc15',
          title: 'string object'
        },
        {
          value: new Boolean(true),
          desc: 'desc16',
          title: 'boolean object'
        }
      ],
      noErrorCases = [
        {
          value: function () {
          },
          title: 'function'
        },
        {
          value: new Function(),
          title: 'function from constructor'
        }
      ];
    errorCases.forEach(function (test) {
      it(test.title, function () {
        expect(App.assertFunction.bind(null, test.value, test.desc)).to.throw(App.FunctionTypeError, test.desc);
      });
    });
    noErrorCases.forEach(function (test) {
      it(test.title, function () {
        expect(App.assertFunction.bind(null, test.value, 'desc')).to.not.throw();
      });
    });
  });
});
