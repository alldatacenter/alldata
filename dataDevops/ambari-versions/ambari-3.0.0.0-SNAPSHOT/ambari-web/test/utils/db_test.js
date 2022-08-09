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

describe('App.db', function () {

  describe('#App.db.set', function () {

    afterEach(function () {
      App.db.cleanUp();
    });

    it('should create one object', function () {
      App.db.set('a', 'b', 1);
      expect(App.db.data.a.b).to.equal(1);
    });

    it('should create nested objects', function () {
      App.db.set('b.c', 'd', 1);
      expect(App.db.data.b.c.d).to.equal(1);
    });

  });

  describe('#App.db.get', function () {

    after(function () {
      App.db.cleanUp();
    });

    it('should return undefined', function () {
      var ret = App.db.get('a', 'b');
      expect(ret).to.be.undefined;
    });

    it('should return set value', function () {
      App.db.set('a', 'b', 10);
      var ret = App.db.get('a', 'b');
      expect(ret).to.equal(10);
    });

  });

  describe('#App.db.setProperties', function () {

    afterEach(function () {
      App.db.cleanUp();
    });

    it('should create one object', function () {
      App.db.setProperties('a', {b: 1, c: 2});
      expect(App.db.data.a).to.eql({b: 1, c: 2});
    });

    it('should create nested objects', function () {
      App.db.setProperties('b.c', {b: 1, c: 2});
      expect(App.db.data.b.c).to.eql({b: 1, c: 2});
    });

  });

  describe('#App.db.getProperties', function () {

    after(function () {
      App.db.cleanUp();
    });

    it('should return undefined', function () {
      var ret = App.db.getProperties('a', ['b', 'c']);
      expect(ret).to.eql({b: undefined, c: undefined});
    });

    it('should return set value', function () {
      App.db.setProperties('a', {b: 1, c: 2});
      var ret = App.db.getProperties('a', ['b', 'c']);
      expect(ret).to.eql({b: 1, c: 2});
    });

  });

});