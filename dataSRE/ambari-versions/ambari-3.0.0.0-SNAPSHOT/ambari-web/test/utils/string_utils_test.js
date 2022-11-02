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

var stringUtils = require('utils/string_utils');
require('utils/helper');

describe('stringUtils', function () {

  describe('#underScoreToCamelCase', function () {
    var tests = [
      {m:'a_b_c to aBC',i:'a_b_c',e:'aBC'},
      {m:'a_bc to aBc',i:'a_bc',e:'aBc'},
      {m:'ab_c to abC',i:'ab_c',e:'abC'},
      {m:'_b_c to BC',i:'_b_c',e:'BC'}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(stringUtils.underScoreToCamelCase(test.i)).to.equal(test.e);
      });
    });
  });

  describe('#pad', function () {
    var tests = [
      {m: '"name" to "    name"', i: 'name', l: 8, a: 1, f: ' ', e: '    name'},
      {m: '"name" to "name    "', i: 'name', l: 8, a: 2, f: ' ', e: 'name    '},
      {m: '"name" to "  name  "', i: 'name', l: 8, a: 3, f: ' ', e: '  name  '},
      {m: '"name" to "name    "', i: 'name', l: 8, a: 0, f: ' ', e: 'name    '},
      {m: '"name" to "name    "', i: 'name', l: 8, a:-1, f: ' ', e: 'name    '},
      {m: '"name" to "name"', i: 'name', l: 4, a: 1, f: ' ', e: 'name'},
      {m: '"name" to "||||||||name"', i: 'name', l: 8, a:1, f: '||', e: '||||||||name'},
      {m: '"name" to "||||name||||"', i: 'name', l: 8, a:3, f: '||', e: '||||name||||'},
      {m: '"name" to "name||||||||"', i: 'name', l: 8, a:2, f: '||', e: 'name||||||||'},
      {m: '"name" to "name" `str` param passed only', i: 'name', e: 'name'}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(stringUtils.pad(test.i, test.l, test.f, test.a)).to.equal(test.e);
      });
    });
  });

  describe('#compareVersions', function () {
    var tests = [
      {m: '1.2 equal to 1.2', v1:'1.2', v2:'1.2', e: 0},
      {m: '1.2 lower than 1.3', v1:'1.2', v2:'1.3', e: -1},
      {m: '1.3 higher than 1.2', v1:'1.3', v2:'1.2', e: 1},
      {m: '1.2.1 higher than 1.2', v1:'1.2.1', v2:'1.2', e: 1},
      {m: '11.2 higher than 2.2', v1:'11.2', v2:'2.2', e: 1},
      {m: '0.9 higher than 0.8', v1:'0.9', v2:'0.8', e: 1},
      {m: '1.1-2  equal to 1.1-2 ', v1:'1.1-2', v2:'1.1-2', e: 0},
      {m: '1.1-2 higher than 1.1-1', v1:'1.1-2', v2:'1.1-1', e: 1},
      {m: '1.1-4 lower than 1.1-46', v1:'1.1-4', v2:'1.1-46', e: -1},
      {m: 'return false if no string passed', v1: '0.9', e: -1}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(stringUtils.compareVersions(test.v1, test.v2)).to.equal(test.e);
      });
    });
  });

  describe('#isSingleLine', function () {
    var tests = [
      {m: 'is single line text', t: 'a b', e: true},
      {m: 'is single line text', t: 'a b\n', e: true},
      {m: 'is single line text', t: '\na b', e: true},
      {m: 'is not single line text', t: 'a\nb', e: false}
    ];
    tests.forEach(function(test) {
      it(test.t + ' ' + test.m + ' ', function () {
        expect(stringUtils.isSingleLine(test.t)).to.equal(test.e);
      });
    });
  });

  describe('#arrayToCSV', function() {
    var test = [{a: 1, b:2, c:3}, {a: 1, b:2, c:3}, {a: 1, b:2, c:3}];
    it('array of object to csv-string', function () {
      expect(stringUtils.arrayToCSV(test)).to.equal("1,2,3\n1,2,3\n1,2,3\n");
    });
  });

  describe('#getFileFromPath', function() {
    var tests = [
      {t: undefined, e: ''},
      {t: {}, e: ''},
      {t: [], e: ''},
      {t: '', e: ''},
      {t: function(){}, e: ''},
      {t: '/path/to/file.ext', e: 'file.ext'},
      {t: 'file.ext', e: 'file.ext'},
      {t: 'file', e: 'file'},
      {t: '/path/to/file', e: 'file'}
    ];
    tests.forEach(function(test) {
      it('Check ' + typeof test.t, function () {
        expect(stringUtils.getFileFromPath(test.t)).to.equal(test.e);
      });
    });
  });

  describe('#getPath', function() {
      var tests = [
        {t: undefined, e: ''},
        {t: {}, e: ''},
        {t: [], e: ''},
        {t: '', e: ''},
        {t: function(){}, e: ''},
        {t: '/path/to/filename', e: '/path/to'},
        {t: '/path/to/', e: '/path/to'},
        {t: '/filename', e: '/'},
        {t: 'filename', e: ''},
        {t: '/path/', e: '/path'},
        {t: 'filename/', e: ''}
      ];
      tests.forEach(function(test) {
          it('Check ' + typeof test.t, function () {
            expect(stringUtils.getPath(test.t)).to.equal(test.e);
          });
      });
  });

  describe('#getCamelCase', function () {
    var tests = [
      {i:'a',e:'A'},
      {i:'aB',e:'Ab'},
      {i:'a b',e:'A B'},
      {i:'a.b',e:'A.B'},
      {i:'a,b',e:'A,B'},
      {i:'a;b',e:'A;B'},
      {i:'a. b',e:'A. B'},
      {i:'a   b',e:'A   B'},
      {i:'aaa. bbb',e:'Aaa. Bbb'},
      {i:'aAA. bBB',e:'Aaa. Bbb'},
      {i:'STARTING',e:'Starting'},
      {i:'starting',e:'Starting'},
      {i:'starting,ending',e:'Starting,Ending'},
      {i: null, e: null},
      {i: undefined, e: undefined}
    ];
    tests.forEach(function(test) {
      it(test.i + ' to ' + test.e + ' ', function () {
        expect(stringUtils.getCamelCase(test.i)).to.equal(test.e);
      });
    });
  });

  describe('#findIn', function () {
    var tests = [
      {
        obj: {
          a: '1',
          b: '2'
        },
        key: 'a',
        index: 0,
        e: '1'
      }, {
        obj: {
          a: '1',
          b: '2'
        },
        key: 'a',
        index: 1,
        e: null
      }, {
        obj: {
          a: '1',
          b: '2',
          c: {
            a: '11',
            aa: '12'
          }
        },
        key: 'a',
        index: 1,
        e: '11'
      }, {
        obj: {
          a: '1',
          b: '2',
          c: {
            a: '11',
            aa: {
              a: '22'
            }
          }
        },
        key: 'a',
        index: 2,
        e: '22'
      }, {
        obj: {
          a: '1',
          b: '2',
          c: {
            a: '11',
            aa: {
              a: '22'
            }
          }
        },
        key: 'a',
        index: 0,
        e: '1'
      }, {
        obj: {
          a: '1',
          b: '2',
          c: {
            a: '11',
            aa: {
              a: '22'
            }
          }
        },
        key: 'g',
        index: 0,
        e: null
      }
    ];
    tests.forEach(function(test) {
      it(test.key + ' @ ' + test.index + ' = ' + test.e, function () {
        expect(test.key.findIn(test.obj, test.index)).to.equal(test.e);
      });
    });
  });

  describe("#htmlEntities()", function() {
    var tests = [
      {t: undefined, e: ''},
      {t: '', e: ''},
      {t: 'abc', e: 'abc'},
      {t: 'abc<script>abc', e: 'abc&lt;script&gt;abc'}
    ];
    tests.forEach(function(test) {
      it('Check ' + typeof test.t, function () {
        expect(stringUtils.htmlEntities(test.t)).to.equal(test.e);
      });
    });
  });

  describe('#upperUnderscoreToText', function() {
    var testCases = [
      {
        input: null,
        expected: ''
      },
      {
        input: '',
        expected: ''
      },
      {
        input: 'foo',
        expected: 'Foo'
      },
      {
        input: 'FOO',
        expected: 'Foo'
      },
      {
        input: 'FOO_BAR',
        expected: 'Foo Bar'
      }
    ];
    testCases.forEach(function(test) {
      it('should return ' + test.expected + ' when string is ' + test.input, function() {
        expect(stringUtils.upperUnderscoreToText(test.input)).to.be.equal(test.expected);
      });
    });
  });

  describe('#unicodeEscape', function() {

    it('a/b should be converted to "a\\u002fb"', function() {
      expect(stringUtils.unicodeEscape('a/b', /[\/]/g)).to.be.equal("a\\u002fb");
    });
    it('a/b should be converted to "\\u0061\\u002f\\u0062"',  function() {
      expect(stringUtils.unicodeEscape('a/b')).to.be.equal("\\u0061\\u002f\\u0062");
    });
    it('a/b should be converted to "a/b"',  function() {
      expect(stringUtils.unicodeEscape('a/b', /[0-9]/g)).to.be.equal("a/b");
    });
  });
});
