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
var mapper;

require('mappers/server_data_mapper');

describe('App.QuickDataMapper', function () {

  var testJson = {
    a1: {
      b1: {
        c1: 'val1'
      },
      b2: 'val2',
      b3: [
        {
          c2: 'val4'
        },
        {
          c2: 'val5'
        },
        {
          c2: 'val1'
        }
      ]
    },
    a2: 'val3',
    item: {
      'key.dotted': 'val6'
    }
  };

  beforeEach(function () {
    mapper = App.QuickDataMapper.create();
  });

  describe('#getJsonProperty', function() {
    var tests = [
      {i:'a1.b1.c1',e:'val1'},
      {i:'a1.b2',e:'val2'},
      {i:'a2',e:'val3'},
      {i:'a1.b3[0].c2',e:'val4'},
      {i:'a1.b3[1].c2',e:'val5'}
    ];
    tests.forEach(function(test) {
      it(test.i, function() {
        expect(mapper.getJsonProperty(testJson, test.i)).to.equal(test.e);
      });
    });
  });

  describe('#parseIt', function() {
    var config = {
      $a2: 'a2',
      f1: 'a1.b1.c1',
      f2: 'a1.b3[0].c2',
      f3: 'a1.b3',
      f4_key: 'a1.b3',
      f4_type: 'array',
      f4: {
        item: 'c2'
      }
    };
    var result;

    beforeEach(function () {
      result = mapper.parseIt(testJson, config);
    });

    it('Property starts with $', function() {
      expect(result.a2).to.equal('a2');
    });
    it('Multi-components path', function() {
      expect(result.f1).to.equal('val1');
    });
    it('Path with array index', function() {
      expect(result.f2).to.equal('val4');
    });
    it('Path returns array', function() {
      expect(result.f3.length).to.equal(3);
    });
    it('Generate array of json fields', function() {
      expect(result.f4).to.eql(['val1','val4','val5']);
    });
  });

  describe('#binaryIndexOf', function () {

    var array1 = [1,2,3,4,5,6,7,8,9];
    var array2 = ['b','c','d','e','f','g'];

    array1.forEach(function(item, index) {
      it('numeric array. test ' + (index + 1), function () {
        expect(mapper.binaryIndexOf(array1, item)).to.equal(index);
      });
    });

    it('numeric array. element doesn\'t exists', function () {
      expect(mapper.binaryIndexOf(array1, 0)).to.be.below(0);
    });

    it('numeric array. element doesn\'t exists 2', function () {
      expect(mapper.binaryIndexOf(array1, 10)).to.be.below(0);
    });

    array2.forEach(function(item, index) {
      it('string array. test ' + (index + 1), function () {
        expect(mapper.binaryIndexOf(array2, item)).to.equal(index);
      });
    });

    it('string array. element doesn\'t exists', function () {
      expect(mapper.binaryIndexOf(array2, 'a')).to.be.below(0);
    });

    it('string array. element doesn\'t exists 2', function () {
      expect(mapper.binaryIndexOf(array2, 'q')).to.be.below(0);
    });

  });

  describe('#updatePropertiesByConfig', function() {

    it('should update properties of record', function() {
      const record = Em.Object.create({
        isLoaded: true,
        prop1: 'v1',
        prop2: 'v2'
      });
      const config = {
        prop1: 'ext1',
        prop2: 'ext2',
        prop3: 'ext3'
      };
      mapper.updatePropertiesByConfig(record, {ext1: 'v11', ext3: 'v31'}, config);
      expect(record.get('prop1')).to.be.equal('v11');
      expect(record.get('prop2')).to.be.equal('v2');
      expect(record.get('prop3')).to.be.equal('v31');
    });
  });

});
