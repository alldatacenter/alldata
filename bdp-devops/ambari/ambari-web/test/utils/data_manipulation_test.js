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

var dataManipulation = require('utils/data_manipulation');

describe('dataManipulation', function () {

  describe('#rejectPropertyValues', function () {

    it('Basic test', function () {
      var collection = [
          {n: 'v1'},
          {n: 'v2'},
          {n: 'v3'},
          {n: 'v4'}
        ],
        key = 'n',
        valuesToReject = ['v2', 'v3'];
      var result = dataManipulation.rejectPropertyValues(collection, key, valuesToReject);
      expect(result).to.eql([
        {n: 'v1'},
        {n: 'v4'}
      ]);
    });

  });

  describe('#filterPropertyValues', function () {

    it('Basic test', function () {
      var collection = [
          {n: 'v1'},
          {n: 'v2'},
          {n: 'v3'},
          {n: 'v4'}
        ],
        key = 'n',
        valuesToFilter = ['v2', 'v3'];
      var result = dataManipulation.filterPropertyValues(collection, key, valuesToFilter);
      expect(result).to.eql([
        {n: 'v2'},
        {n: 'v3'}
      ]);
    });

  });

  describe('#groupPropertyValues', function () {

    it('Basic test', function () {
      var collection = [
          {n: 'v1'},
          {n: 'v2'},
          {n: 'v2'},
          {n: 'v4'}
        ],
        key = 'n';
      var result = dataManipulation.groupPropertyValues(collection, key);
      expect(JSON.stringify(result)).to.equal(JSON.stringify({
        v1: [
          {n: 'v1'}
        ],
        v2: [
          {n: 'v2'},
          {n: 'v2'}
        ],
        v4: [
          {n: 'v4'}
        ]}));
    });

  });

});