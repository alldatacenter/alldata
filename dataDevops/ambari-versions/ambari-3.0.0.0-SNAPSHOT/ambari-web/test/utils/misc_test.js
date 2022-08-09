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

var misc = require('utils/misc');

describe('misc', function () {

  describe('#formatBandwidth', function () {
    var tests = Em.A([
      {m:'undefined to undefined',i:undefined,e:undefined},
      {m:'0 to <1KB',i:'0',e:'<1KB'},
      {m:'1000 to <1KB',i:'1000',e:'<1KB'},
      {m:'1024 to 1.0KB',i:'1024',e:'1.0KB'},
      {m:'2048 to 2.0KB',i:'2048',e:'2.0KB'},
      {m:'1048576 to 1.0MB',i:'1048576',e:'1.0MB'},
      {m:'1782579 to 1.7MB',i:'1782579',e:'1.7MB'},
      {m:'1546188226 to 1.44GB',i:'1546188226',e:'1.44GB'}
    ]);
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(misc.formatBandwidth(test.i)).to.equal(test.e);
      });
    });
    it('NaN to NaN', function () {
      expect(isNaN(misc.formatBandwidth(NaN))).to.equal(true);
    });
  });

  describe('#ipToInt', function () {
    var tests = Em.A([
      {m:'0.0.0.0 to 0',i:'0.0.0.0',e:0},
      {m:'255.255.255.255 to 4294967295',i:'255.255.255.255',e:4294967295},
      {m:'"" to false',i:'',e:false},
      {m:'255.255.255.256 to false',i:'255.255.255.256',e:false},
      {m:'255.255.255 to false',i:'255.255.255',e:false}
    ]);
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(misc.ipToInt(test.i)).to.equal(test.e);
      });
    });
  });

  describe('#sortByOrder', function() {
    var tests = Em.A([
      {
        sortOrder: ['b', 'c', 'a'],
        array: [{id:'a'}, {id:'b'}, Em.Object.create({id:'c'})],
        e: [{id:'b'}, Em.Object.create({id:'c'}), {id:'a'}],
        m: 'Array with Ember and native objects'
      },
      {
        sortOrder: ['b', 'c', 'a'],
        array: [{id:'a'}, {id:'b'}, {id:'c'}],
        e: [{id:'b'}, {id:'c'}, {id:'a'}],
        m: 'Array with native objects'
      },
      {
        sortOrder: ['b', 'c', 'a'],
        array: [Em.Object.create({id:'a'}), Em.Object.create({id:'b'}), Em.Object.create({id:'c'})],
        e: [Em.Object.create({id:'b'}), Em.Object.create({id:'c'}), Em.Object.create({id:'a'})],
        m: 'Array with Ember objects'
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        expect(misc.sortByOrder(test.sortOrder, test.array)).to.eql(test.e);
      });
    });
  });
});
