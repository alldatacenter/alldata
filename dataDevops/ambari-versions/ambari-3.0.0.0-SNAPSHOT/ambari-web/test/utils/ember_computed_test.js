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

require('utils/ember_computed');

describe('Ember.computed macros', function () {

  beforeEach(function () {
    App.reopen({
      someRandomTestingKey: function () {
        return this.get('someAnotherKey');
      }.property('someAnotherKey'),
      someAnotherKey: '',
      appProp1: 1,
      appProp2: 2
    });
  });

  afterEach(function () {
    delete App.someAnotherKey;
    delete App.someRandomTestingKey;
  });

  describe('#equal', function () {

    beforeEach(function () {
      App.setProperties({
        someAnotherKey: '123'
      });
      this.obj = Em.Object.create({
        prop1: '123',
        prop2: Em.computed.equal('prop1', '123'),
        prop3: Em.computed.equal('App.someRandomTestingKey', '123')
      });
    });

    it('`true` if values are equal', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if values are not equal', function () {
      this.obj.set('prop1', '321');
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`prop3` depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.be.true;
      App.set('someAnotherKey', '');
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['App.someRandomTestingKey']);
    });

  });

  describe('#notEqual', function () {

    beforeEach(function () {
      App.setProperties({
        someAnotherKey: '123'
      });
      this.obj = Em.Object.create({
        prop1: '123',
        prop2: Em.computed.notEqual('prop1', '123'),
        prop3: Em.computed.notEqual('App.someRandomTestingKey', '123')
      });
    });

    it('`false` if values are equal', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if values are not equal', function () {
      this.obj.set('prop1', '321');
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`prop3` depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.be.false;
      App.set('someAnotherKey', '');
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['App.someRandomTestingKey']);
    });

  });

  describe('#equalProperties', function () {

    beforeEach(function () {
      App.set('someAnotherKey', '123');
      this.obj = Em.Object.create({
        prop1: '123',
        prop2: '123',
        prop3: Em.computed.equalProperties('prop1', 'prop2'),
        prop4: Em.computed.equalProperties('App.someRandomTestingKey', 'prop2'),
        prop5: Em.computed.equalProperties('prop1', 'App.someRandomTestingKey')
      });
    });

    it('`true` if values are equal', function () {
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('`false` if values are not equal', function () {
      this.obj.set('prop1', '321');
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('prop4 depends on App.* key', function () {
      expect(this.obj.get('prop4')).to.be.true;
      App.set('someAnotherKey', '');
      expect(this.obj.get('prop4')).to.be.false;
    });

    it('prop5 depends on App.* key', function () {
      expect(this.obj.get('prop5')).to.be.true;
      App.set('someAnotherKey', '');
      expect(this.obj.get('prop5')).to.be.false;
    });

    it('prop4 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop4._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop2']);
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['prop1', 'App.someRandomTestingKey']);
    });

  });

  describe('#notEqualProperties', function () {

    beforeEach(function () {
      App.set('someAnotherKey', '123');
      this.obj = Em.Object.create({
        prop1: '123',
        prop2: '123',
        prop3: Em.computed.notEqualProperties('prop1', 'prop2'),
        prop4: Em.computed.notEqualProperties('App.someRandomTestingKey', 'prop2'),
        prop5: Em.computed.notEqualProperties('prop1', 'App.someRandomTestingKey')
      });
    });

    it('`false` if values are equal', function () {
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`true` if values are not equal', function () {
      this.obj.set('prop1', '321');
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('prop4 depends on App.* key', function () {
      expect(this.obj.get('prop4')).to.be.false;
      App.set('someAnotherKey', '');
      expect(this.obj.get('prop4')).to.be.true;
    });

    it('prop5 depends on App.* key', function () {
      expect(this.obj.get('prop5')).to.be.false;
      App.set('someAnotherKey', '');
      expect(this.obj.get('prop5')).to.be.true;
    });

    it('prop4 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop4._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop2']);
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['prop1', 'App.someRandomTestingKey']);
    });

  });

  describe('#ifThenElse', function () {

    beforeEach(function () {
      App.set('someAnotherKey', true);
      this.obj = Em.Object.create({
        prop1: true,
        prop2: Em.computed.ifThenElse('prop1', '1', '0'),
        prop3: Em.computed.ifThenElse('App.someRandomTestingKey', '1', '0')
      });
    });

    it('`1` if `prop1` is true', function () {
      expect(this.obj.get('prop2')).to.equal('1');
    });

    it('`0` if `prop1` is false', function () {
      this.obj.set('prop1', false);
      expect(this.obj.get('prop2')).to.equal('0');
    });

    it('prop3 depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.equal('1');
      App.set('someAnotherKey', false);
      expect(this.obj.get('prop3')).to.equal('0');
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['App.someRandomTestingKey']);
    });

  });

  describe('#ifThenElseByKeys', function () {

    beforeEach(function () {
      App.set('someAnotherKey', true);
      this.obj = Em.Object.create({
        prop1: true,
        prop2: Em.computed.ifThenElseByKeys('prop1', 'prop4', 'prop5'),
        prop3: Em.computed.ifThenElseByKeys('App.someRandomTestingKey', 'App.appProp1', 'App.appProp2'),
        prop4: 1,
        prop5: 2
      });
    });

    it('`1` if `prop1` is true', function () {
      expect(this.obj.get('prop2')).to.equal(1);
    });

    it('`0` if `prop1` is false', function () {
      this.obj.set('prop1', false);
      expect(this.obj.get('prop2')).to.equal(2);
    });

    it('prop2 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1', 'prop4', 'prop5']);
    });

    it('prop3 depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.equal(1);
      App.set('someAnotherKey', false);
      expect(this.obj.get('prop3')).to.equal(2);
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['App.someRandomTestingKey', 'App.appProp1', 'App.appProp2']);
    });

  });

  describe('#and', function () {

    beforeEach(function () {
      App.setProperties({
        someAnotherKey: true
      });
      this.obj = Em.Object.create({
        prop1: true,
        prop2: true,
        prop3: true,
        prop4: Em.computed.and('prop1', 'prop2', 'prop3'),
        prop5: Em.computed.and('prop1', '!prop2', '!prop3'),
        prop6: Em.computed.and('App.someRandomTestingKey', 'prop1'),
        prop7: Em.computed.and('!App.someRandomTestingKey', 'prop1')
      });
    });

    it('prop4 `true` if all dependent properties are true', function () {
      expect(this.obj.get('prop4')).to.be.true;
    });

    it('prop4 `false` if at elast one dependent property is false', function () {
      this.obj.set('prop2', false);
      expect(this.obj.get('prop4')).to.be.false;
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['prop1', 'prop2', 'prop3']);
    });

    it('prop5 `false` if some inverted dependent properties is true', function () {
      expect(this.obj.get('prop5')).to.be.false;
    });

    it('prop5 `false` if some inverted dependent properties is true (2)', function () {
      this.obj.set('prop1', true);
      expect(this.obj.get('prop5')).to.be.false;
    });

    it('prop5 `true` ', function () {
      this.obj.set('prop2', false);
      this.obj.set('prop3', false);
      expect(this.obj.get('prop5')).to.be.true;
    });

    it('`prop6` depends on App.* key', function () {
      expect(this.obj.get('prop6')).to.be.true;
      App.set('someAnotherKey', false);
      expect(this.obj.get('prop6')).to.be.false;
      App.set('someAnotherKey', true);
      expect(this.obj.get('prop6')).to.be.true;
    });

    it('prop6 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop6._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop1']);
    });

    it('`prop7` depends on inverted App.* key', function () {
      expect(this.obj.get('prop7')).to.be.false;
      App.set('someAnotherKey', false);
      expect(this.obj.get('prop7')).to.be.true;
      App.set('someAnotherKey', true);
      expect(this.obj.get('prop7')).to.be.false;
    });

    it('prop7 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop7._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop1']);
    });

  });

  describe('#or', function () {
    beforeEach(function () {
      App.setProperties({
        someAnotherKey: true
      });
      this.obj = Em.Object.create({
        prop1: false,
        prop2: false,
        prop3: false,
        prop4: Em.computed.or('prop1', 'prop2', 'prop3'),
        prop5: Em.computed.or('!prop1', '!prop2', '!prop3'),
        prop6: Em.computed.or('App.someRandomTestingKey', 'prop1'),
        prop7: Em.computed.or('!App.someRandomTestingKey', 'prop1')
      });
    });

    it('`false` if all dependent properties are false', function () {
      expect(this.obj.get('prop4')).to.be.false;
    });

    it('`true` if at elast one dependent property is true', function () {
      this.obj.set('prop2', true);
      expect(this.obj.get('prop4')).to.be.true;
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['prop1', 'prop2', 'prop3']);
    });

    it('prop5 `true` if some inverted dependent properties is true', function () {
      expect(this.obj.get('prop5')).to.be.true;
    });

    it('prop5 `true` if some inverted dependent properties is true (2)', function () {
      this.obj.set('prop1', true);
      expect(this.obj.get('prop5')).to.be.true;
    });

    it('prop5 `false` ', function () {
      this.obj.set('prop1', true);
      this.obj.set('prop2', true);
      this.obj.set('prop3', true);
      expect(this.obj.get('prop5')).to.be.false;
    });

    it('`prop6` depends on App.* key', function () {
      expect(this.obj.get('prop6')).to.be.true;
      App.set('someAnotherKey', false);
      expect(this.obj.get('prop6')).to.be.false;
      App.set('someAnotherKey', true);
      expect(this.obj.get('prop6')).to.be.true;
    });

    it('prop6 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop6._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop1']);
    });

    it('`prop7` depends on inverted App.* key', function () {
      expect(this.obj.get('prop7')).to.be.false;
      App.set('someAnotherKey', false);
      expect(this.obj.get('prop7')).to.be.true;
      App.set('someAnotherKey', true);
      expect(this.obj.get('prop7')).to.be.false;
    });

    it('prop7 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop7._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop1']);
    });

  });

  describe('#sumProperties', function () {

    beforeEach(function () {
      App.setProperties({
        someAnotherKey: 5
      });
      this.obj = Em.Object.create({
        prop1: 1,
        prop2: 2,
        prop3: 3,
        prop4: Em.computed.sumProperties('prop1', 'prop2', 'prop3'),
        prop5: Em.computed.sumProperties('prop1', 'prop2', 'App.someRandomTestingKey')
      });
    });

    it('should be sum of dependent values', function () {
      expect(this.obj.get('prop4')).to.equal(6);
    });

    it('should be updated if some dependent value is changed', function () {
      this.obj.set('prop1', 4);
      expect(this.obj.get('prop4')).to.equal(9);
    });

    it('should be updated if some dependent value is string', function () {
      this.obj.set('prop1', '4');
      expect(this.obj.get('prop4')).to.equal(9);
    });

    it('should be updated if some dependent value is string (2)', function () {
      this.obj.set('prop1', '4.5');
      expect(this.obj.get('prop4')).to.equal(9.5);
    });

    it('should be updated if some dependent value is null', function () {
      this.obj.set('prop1', null);
      expect(this.obj.get('prop4')).to.equal(5);
    });

    it('`prop5` depends on App.* key', function () {
      expect(this.obj.get('prop5')).to.equal(8);
      App.set('someAnotherKey', 6);
      expect(this.obj.get('prop5')).to.equal(9);
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['prop1', 'prop2', 'App.someRandomTestingKey']);
    });

  });

  describe('#gte', function () {

    beforeEach(function () {
      App.set('someAnotherKey', 4);
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: Em.computed.gte('prop1', 3),
        prop3: Em.computed.gte('App.someRandomTestingKey', 3)
      });
    });

    it('`false` if value is less than needed', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if value is equal to the needed', function () {
      this.obj.set('prop1', 3);
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`true` if value is greater than needed', function () {
      this.obj.set('prop1', 4);
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('prop3 depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.be.true;
      App.set('someAnotherKey', 3);
      expect(this.obj.get('prop3')).to.be.true;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['App.someRandomTestingKey']);
    });

  });

  describe('#gteProperties', function () {

    beforeEach(function () {
      App.set('someAnotherKey', 4);
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: 3,
        prop3: Em.computed.gteProperties('prop1', 'prop2'),
        prop4: Em.computed.gteProperties('App.someRandomTestingKey', 'prop2'),
        prop5: Em.computed.gteProperties('prop1', 'App.someRandomTestingKey')
      });
    });

    it('`false` if value is less than needed', function () {
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`true` if value is equal to the needed', function () {
      this.obj.set('prop1', 3);
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('`true` if value is greater than needed', function () {
      this.obj.set('prop1', 4);
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('prop4 depends on App.* key', function () {
      expect(this.obj.get('prop4')).to.be.true;
      App.set('someAnotherKey', 3);
      expect(this.obj.get('prop4')).to.be.true;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop4')).to.be.false;
    });

    it('prop5 depends on App.* key', function () {
      expect(this.obj.get('prop5')).to.be.false;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop5')).to.be.true;
      App.set('someAnotherKey', 1);
      expect(this.obj.get('prop5')).to.be.true;
    });

    it('prop4 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop4._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop2']);
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['prop1', 'App.someRandomTestingKey']);
    });

  });

  describe('#lte', function () {

    beforeEach(function () {
      App.set('someAnotherKey', 0);
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: Em.computed.lte('prop1', 1),
        prop3: Em.computed.lte('App.someRandomTestingKey', 1)
      });
    });

    it('`false` if value is greater  than needed', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if value is equal to the needed', function () {
      this.obj.set('prop1', 1);
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`true` if value is less than needed', function () {
      this.obj.set('prop1', 0);
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('prop3 depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.be.true;
      App.set('someAnotherKey', 1);
      expect(this.obj.get('prop3')).to.be.true;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['App.someRandomTestingKey']);
    });

  });

  describe('#lteProperties', function () {

    beforeEach(function () {
      App.set('someAnotherKey', 1);
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: 1,
        prop3: Em.computed.lteProperties('prop1', 'prop2'),
        prop4: Em.computed.lteProperties('App.someRandomTestingKey', 'prop2'),
        prop5: Em.computed.lteProperties('prop1', 'App.someRandomTestingKey')
      });
    });

    it('`false` if d1 is greater than d2', function () {
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`true` if d1 is equal to the d2', function () {
      this.obj.set('prop1', 1);
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('`true` if d1 is less than d2', function () {
      this.obj.set('prop1', 0);
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('prop4 depends on App.* key', function () {
      expect(this.obj.get('prop4')).to.be.true;
      App.set('someAnotherKey', 0);
      expect(this.obj.get('prop4')).to.be.true;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop4')).to.be.false;
    });

    it('prop5 depends on App.* key', function () {
      expect(this.obj.get('prop5')).to.be.false;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop5')).to.be.true;
      App.set('someAnotherKey', 3);
      expect(this.obj.get('prop5')).to.be.true;
    });

    it('prop4 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop4._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop2']);
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['prop1', 'App.someRandomTestingKey']);
    });

  });

  describe('#gt', function () {

    beforeEach(function () {
      App.set('someAnotherKey', 4);
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: Em.computed.gt('prop1', 3),
        prop3: Em.computed.gt('App.someRandomTestingKey', 3)
      });
    });

    it('`false` if value is less than needed', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`false` if value is equal to the needed', function () {
      this.obj.set('prop1', 3);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if value is greater than needed', function () {
      this.obj.set('prop1', 4);
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('prop3 depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.be.true;
      App.set('someAnotherKey', 3);
      expect(this.obj.get('prop3')).to.be.false;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['App.someRandomTestingKey']);
    });

  });

  describe('#gtProperties', function () {

    beforeEach(function () {
      App.set('someAnotherKey', 4);
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: 3,
        prop3: Em.computed.gtProperties('prop1', 'prop2'),
        prop4: Em.computed.gtProperties('App.someRandomTestingKey', 'prop2'),
        prop5: Em.computed.gtProperties('prop1', 'App.someRandomTestingKey')
      });
    });

    it('`false` if value is less than needed', function () {
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`false` if value is equal to the needed', function () {
      this.obj.set('prop1', 3);
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`true` if value is greater than needed', function () {
      this.obj.set('prop1', 4);
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('prop4 depends on App.* key', function () {
      expect(this.obj.get('prop4')).to.be.true;
      App.set('someAnotherKey', 3);
      expect(this.obj.get('prop4')).to.be.false;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop4')).to.be.false;
    });

    it('prop5 depends on App.* key', function () {
      expect(this.obj.get('prop5')).to.be.false;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop5')).to.be.false;
      App.set('someAnotherKey', 1);
      expect(this.obj.get('prop5')).to.be.true;
    });

    it('prop4 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop4._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop2']);
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['prop1', 'App.someRandomTestingKey']);
    });

  });

  describe('#lt', function () {

    beforeEach(function () {
      App.set('someAnotherKey', 0);
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: Em.computed.lt('prop1', 1),
        prop3: Em.computed.lt('App.someRandomTestingKey', 1)
      });
    });

    it('`false` if value is greater  than needed', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`false` if value is equal to the needed', function () {
      this.obj.set('prop1', 1);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if value is less than needed', function () {
      this.obj.set('prop1', 0);
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('prop3 depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.be.true;
      App.set('someAnotherKey', 1);
      expect(this.obj.get('prop3')).to.be.false;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['App.someRandomTestingKey']);
    });

  });

  describe('#ltProperties', function () {

    beforeEach(function () {
      App.set('someAnotherKey', 1);
      this.obj = Em.Object.create({
        prop1: 2,
        prop2: 1,
        prop3: Em.computed.ltProperties('prop1', 'prop2'),
        prop4: Em.computed.ltProperties('App.someRandomTestingKey', 'prop2'),
        prop5: Em.computed.ltProperties('prop1', 'App.someRandomTestingKey')
      });
    });

    it('`false` if d1 is greater than d2', function () {
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`false` if d1 is equal to the d2', function () {
      this.obj.set('prop1', 1);
      expect(this.obj.get('prop3')).to.be.false;
    });

    it('`true` if d1 is less than d2', function () {
      this.obj.set('prop1', 0);
      expect(this.obj.get('prop3')).to.be.true;
    });

    it('prop4 depends on App.* key', function () {
      expect(this.obj.get('prop4')).to.be.false;
      App.set('someAnotherKey', 0);
      expect(this.obj.get('prop4')).to.be.true;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop4')).to.be.false;
    });

    it('prop5 depends on App.* key', function () {
      expect(this.obj.get('prop5')).to.be.false;
      App.set('someAnotherKey', 2);
      expect(this.obj.get('prop5')).to.be.false;
      App.set('someAnotherKey', 3);
      expect(this.obj.get('prop5')).to.be.true;
    });

    it('prop4 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop4._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop2']);
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['prop1', 'App.someRandomTestingKey']);
    });

  });

  describe('#match', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 'abc',
        prop2: Em.computed.match('prop1', /^ab/)
      })
    });

    it('`true` if value match regexp', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`true` if value match regexp (2)', function () {
      this.obj.set('prop1', 'abaaa');
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if value doesn\'t match regexp', function () {
      this.obj.set('prop1', '!!!!');
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`prop2` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1']);
    });

  });

  describe('#someBy', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: [{a: 1}, {a: 2}, {a: 3}],
        prop2: Em.computed.someBy('prop1', 'a', 2)
      });
    });

    it('`true` if some collection item has needed property value', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if on one collection item doesn\'t have needed property value', function () {
      this.obj.set('prop1.1.a', 3);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`false` for null/undefined collection', function () {
      this.obj.set('prop1', null);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`prop2` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1.@each.a']);
    });

  });

  describe('#someByKey', function () {

    beforeEach(function () {
      App.setProperties({
        someAnotherKey: 2
      });
      this.obj = Em.Object.create({
        prop1: [{a: 1}, {a: 2}, {a: 3}],
        prop2: Em.computed.someByKey('prop1', 'a', 'value1'),
        prop3: Em.computed.someByKey('prop1', 'a', 'App.someRandomTestingKey'),
        value1: 2
      });
    });

    it('`true` if some collection item has needed property value', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if on one collection item doesn\'t have needed property value', function () {
      this.obj.set('prop1.1.a', 3);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`false` for null/undefined collection', function () {
      this.obj.set('prop1', null);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`prop2` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1.@each.a', 'value1']);
    });

    it('`prop3` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['prop1.@each.a', 'App.someRandomTestingKey']);
    });

    it('`prop3` depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.be.true;
      this.obj.set('prop1.1.a', 3);
      expect(this.obj.get('prop3')).to.be.false;
    });

  });

  describe('#everyBy', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: [{a: 2}, {a: 2}, {a: 2}],
        prop2: Em.computed.everyBy('prop1', 'a', 2)
      });
    });

    it('`true` if all collection items have needed property value', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if at least one collection item doesn\'t have needed property value', function () {
      this.obj.set('prop1.1.a', 3);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`false` for null/undefined collection', function () {
      this.obj.set('prop1', null);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`prop2` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1.@each.a']);
    });

  });

  describe('#everyByKey', function () {
    beforeEach(function () {
      App.setProperties({
        someAnotherKey: 2
      });
      this.obj = Em.Object.create({
        prop1: [{a: 2}, {a: 2}, {a: 2}],
        prop2: Em.computed.everyByKey('prop1', 'a', 'value1'),
        prop3: Em.computed.everyByKey('prop1', 'a', 'App.someRandomTestingKey'),
        value1: 2
      });
    });

    it('`true` if all collection items have needed property value', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if at least one collection item doesn\'t have needed property value', function () {
      this.obj.set('prop1.1.a', 3);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`false` for null/undefined collection', function () {
      this.obj.set('prop1', null);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`prop2` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1.@each.a', 'value1']);
    });

    it('`prop3` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['prop1.@each.a', 'App.someRandomTestingKey']);
    });

    it('`prop3` depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.be.true;
      this.obj.set('prop1.1.a', 3);
      expect(this.obj.get('prop3')).to.be.false;
    });

  });

  describe('#mapBy', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: [{a: 1}, {a: 2}, {a: 3}],
        prop2: Em.computed.mapBy('prop1', 'a')
      });
    });

    it('should map dependent property', function () {
      expect(this.obj.get('prop2')).to.eql([1, 2, 3]);
    });

    it('should map dependent property (2)', function () {
      this.obj.get('prop1').push({a: 4});
      expect(this.obj.get('prop2')).to.eql([1, 2, 3, 4]);
    });

    it('`[]` for null/undefined collection', function () {
      this.obj.set('prop1', null);
      expect(this.obj.get('prop2')).to.eql([]);
    });

    it('`prop2` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1.@each.a']);
    });

  });

  describe('#filterBy', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: [{a: 2}, {a: 2}, {a: 3}],
        prop2: Em.computed.filterBy('prop1', 'a', 2)
      });
    });

    it('should filter dependent property', function () {
      expect(this.obj.get('prop2')).to.eql([{a: 2}, {a: 2}]);
    });

    it('should filter dependent property (2)', function () {
      this.obj.get('prop1').pushObject({a: 2});
      expect(this.obj.get('prop2')).to.eql([{a: 2}, {a: 2}, {a: 2}]);
    });

    it('`[]` for null/undefined collection', function () {
      this.obj.set('prop1', null);
      expect(this.obj.get('prop2')).to.eql([]);
    });

    it('`prop2` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1.@each.a']);
    });

  });

  describe('#filterByKey', function () {

    beforeEach(function () {
      App.setProperties({
        someAnotherKey: 2
      });
      this.obj = Em.Object.create({
        prop1: [{a: 2}, {a: 2}, {a: 3}],
        prop2: Em.computed.filterByKey('prop1', 'a', 'value1'),
        prop3: Em.computed.filterByKey('prop1', 'a', 'App.someRandomTestingKey'),
        value1: 2
      });
    });

    it('should filter dependent property', function () {
      expect(this.obj.get('prop2')).to.eql([{a: 2}, {a: 2}]);
    });

    it('should filter dependent property (2)', function () {
      this.obj.get('prop1').pushObject({a: 2});
      expect(this.obj.get('prop2')).to.eql([{a: 2}, {a: 2}, {a: 2}]);
    });

    it('`[]` for null/undefined collection', function () {
      this.obj.set('prop1', null);
      expect(this.obj.get('prop2')).to.eql([]);
    });

    it('`prop2` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1.@each.a', 'value1']);
    });

    it('`prop3` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['prop1.@each.a', 'App.someRandomTestingKey']);
    });

    it('`prop3` depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.eql([{a: 2}, {a: 2}]);
      this.obj.set('prop1.1.a', 3);
      expect(this.obj.get('prop3')).to.eql([{a: 2}]);
    });

  });

  describe('#findBy', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: [{b: 1, a: 2}, {b: 2, a: 2}, {a: 3}],
        prop2: Em.computed.findBy('prop1', 'a', 2)
      });
    });

    it('should filter dependent property', function () {
      expect(this.obj.get('prop2')).to.eql({b:1, a: 2});
    });

    it('should filter dependent property (2)', function () {
      this.obj.get('prop1').pushObject({b: 3, a: 2});
      expect(this.obj.get('prop2')).to.eql({b: 1, a: 2});
    });

    it('`null` for null/undefined collection', function () {
      this.obj.set('prop1', null);
      expect(this.obj.get('prop2')).to.be.null;
    });

    it('`prop2` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1.@each.a']);
    });

  });

  describe('#findByKey', function () {

    beforeEach(function () {
      App.setProperties({
        someAnotherKey: 2
      });
      this.obj = Em.Object.create({
        prop1: [{b: 1, a: 2}, {b: 2, a: 2}, {a: 3}],
        prop2: Em.computed.findByKey('prop1', 'a', 'value1'),
        prop3: Em.computed.findByKey('prop1', 'a', 'App.someRandomTestingKey'),
        value1: 2
      });
    });

    it('should filter dependent property', function () {
      expect(this.obj.get('prop2')).to.eql({b:1, a: 2});
    });

    it('should filter dependent property (2)', function () {
      this.obj.get('prop1').pushObject({b: 3, a: 2});
      expect(this.obj.get('prop2')).to.eql({b: 1, a: 2});
    });

    it('`null` for null/undefined collection', function () {
      this.obj.set('prop1', null);
      expect(this.obj.get('prop2')).to.be.null;
    });

    it('`prop2` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1.@each.a', 'value1']);
    });

    it('`prop3` has valid dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['prop1.@each.a', 'App.someRandomTestingKey']);
    });

    it('`prop3` depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.eql({b: 1, a: 2});
      this.obj.get('prop1').pushObject({b: 3, a: 2});
      expect(this.obj.get('prop3')).to.eql({b: 1, a: 2});
    });

  });

  describe('#alias', function() {

    beforeEach(function () {
      App.set('someAnotherKey', {a: {b: 1}});
      this.obj = Em.Object.create({
        prop1: {
          a: {
            b: {
              c: 1
            }
          }
        },
        prop2: Em.computed.alias('prop1.a.b.c'),
        prop3: Em.computed.alias('App.someAnotherKey.a.b')
      })
    });

    it('should be equal to dependent property', function () {
      expect(this.obj.get('prop2')).to.equal(1);
    });

    it('should be equal to dependent property (2)', function () {
      this.obj.set('prop1.a.b.c', 2);
      expect(this.obj.get('prop2')).to.equal(2);
    });

    it('prop3 depends on App.* key', function () {
      expect(this.obj.get('prop3')).to.equal(1);
      App.set('someAnotherKey.a.b', 4);
      expect(this.obj.get('prop3')).to.equal(4);
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['App.someAnotherKey.a.b']);
    });

  });

  describe('#existsIn', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 'v1',
        prop2: Em.computed.existsIn('prop1', ['v1', 'v2'])
      });
    });

    it('`true` if dependent value is in the array', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`true` if dependent value is in the array (2)', function () {
      this.obj.set('prop1', 'v2');
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if dependent value is not in the array', function () {
      this.obj.set('prop1', 'v3');
      expect(this.obj.get('prop2')).to.be.false;
    });

  });

  describe('#existsInByKey', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 'v1',
        prop2: Em.computed.existsInByKey('prop1', 'prop3'),
        prop3: ['v1', 'v2']
      });
    });

    it('`true` if dependent value is in the array', function () {
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`true` if dependent value is in the array (2)', function () {
      this.obj.set('prop1', 'v2');
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`false` if dependent value is not in the array', function () {
      this.obj.set('prop1', 'v3');
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`false` if dependent value is not in the array (2)', function () {
      this.obj.set('prop1', 'v1');
      this.obj.set('prop3', ['v2', 'v3']);
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('prop2 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1', 'prop3.[]']);
    });

  });

  describe('#percents', function () {

    beforeEach(function () {
      App.setProperties({
        p1: 25,
        p2: 100
      });
      this.obj = Em.Object.create({
        prop1: 10,
        prop2: 25,
        prop3: Em.computed.percents('prop1', 'prop2'),
        prop4: Em.computed.percents('prop1', 'prop2', 2),
        prop5: Em.computed.percents('App.p1', 'App.p2', 1)
      });
    });

    afterEach(function () {
      delete App.p1;
      delete App.p2;
    });

    it('should calculate percents', function () {
      expect(this.obj.get('prop3')).to.equal(40);
      expect(this.obj.get('prop4')).to.equal(40.00);
    });

    it('should calculate percents (2)', function () {
      this.obj.set('prop2', 35);
      expect(this.obj.get('prop3')).to.equal(29);
      expect(this.obj.get('prop4')).to.equal(28.57);
    });

    it('should calculate percents (3)', function () {
      this.obj.set('prop2', '35');
      expect(this.obj.get('prop3')).to.equal(29);
      expect(this.obj.get('prop4')).to.equal(28.57);
    });

    it('should calculate percents (4)', function () {
      this.obj.set('prop1', 10.6);
      this.obj.set('prop2', 100);
      expect(this.obj.get('prop3')).to.equal(11);
      expect(this.obj.get('prop4')).to.equal(10.60);
    });

    it('should calculate percents (5)', function () {
      this.obj.set('prop1', '10.6');
      this.obj.set('prop2', 100);
      expect(this.obj.get('prop3')).to.equal(11);
      expect(this.obj.get('prop4')).to.equal(10.60);
    });

    it('prop5 depends on App.* keys', function () {
      expect(this.obj.get('prop5')).to.equal(25.0);
      App.set('p2', 50);
      expect(this.obj.get('prop5')).to.equal(50.0);
      App.set('p1', 10);
      expect(this.obj.get('prop5')).to.equal(20.0);
    });

    it('prop4 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['App.p1', 'App.p2']);
    });

  });

  describe('#formatRole', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 'NAMENODE',
        prop2: false,
        prop3: Em.computed.formatRole('prop1', 'prop2')
      });
      sinon.stub(App.StackServiceComponent, 'find', function () {
        return [
          Em.Object.create({id: 'NAMENODE', displayName: 'NameNode'}),
          Em.Object.create({id: 'SECONDARY_NAMENODE', displayName: 'Secondary NameNode'})
        ];
      });
      sinon.stub(App.StackService, 'find', function () {
        return [
          Em.Object.create({id: 'MAPREDUCE2', displayName: 'MapReduce2'}),
          Em.Object.create({id: 'HIVE', displayName: 'Hive'})
        ];
      });
    });

    afterEach(function () {
      App.StackService.find.restore();
      App.StackServiceComponent.find.restore();
    });

    it('should format as role', function () {
      expect(this.obj.get('prop3')).to.equal('NameNode');
    });

    it('should format as role (2)', function () {
      this.obj.set('prop1', 'HIVE');
      this.obj.set('prop2', true);
      expect(this.obj.get('prop3')).to.equal('Hive');
    });

  });

  describe('#sumBy', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: [
          {a: 1}, {a: 2}, {a: 3}
        ],
        prop2: Em.computed.sumBy('prop1', 'a')
      });
    });

    it('should calculate sum', function () {
      expect(this.obj.get('prop2')).to.equal(6);
    });

    it('should calculate sum (2)', function () {
      this.obj.get('prop1').pushObject({a: 4});
      expect(this.obj.get('prop2')).to.equal(10);
    });

    it('0 for empty collection', function () {
      this.obj.set('prop1', []);
      expect(this.obj.get('prop2')).to.equal(0);
    });

  });

  describe('#i18nFormat', function () {

    beforeEach(function () {

      App.setProperties({
        someAnotherKey: 'some value'
      });

      sinon.stub(Em.I18n, 't', function (key) {
        var msgs = {
          key1: '{0} {1} {2}'
        };
        return msgs[key];
      });
      this.obj = Em.Object.create({
        prop1: 'abc',
        prop2: 'cba',
        prop3: 'aaa',
        prop4: Em.computed.i18nFormat('key1', 'prop1', 'prop2', 'prop3'),
        prop5: Em.computed.i18nFormat('not_existing_key', 'prop1', 'prop2', 'prop3'),
        prop6: Em.computed.i18nFormat('key1', 'App.someRandomTestingKey', 'prop2', 'prop3')
      });
    });

    afterEach(function () {
      Em.I18n.t.restore();
    });

    it('`prop4` check dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop4._dependentKeys).to.eql(['prop1', 'prop2', 'prop3']);
    });

    it('should format message', function () {
      expect(this.obj.get('prop4')).to.equal('abc cba aaa');
    });

    it('should format message (2)', function () {
      this.obj.set('prop1', 'aaa');
      expect(this.obj.get('prop4')).to.equal('aaa cba aaa');
    });

    it('empty string for not existing i18-key', function () {
      expect(this.obj.get('prop5')).to.equal('');
    });

    it('`prop6` depends on App.* key', function () {
      expect(this.obj.get('prop6')).to.equal('some value cba aaa');
      App.set('someAnotherKey', '');
      expect(this.obj.get('prop6')).to.equal(' cba aaa');
    });

    it('prop6 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop6._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop2', 'prop3']);
    });

  });

  describe('#concat', function () {

    beforeEach(function () {

      App.setProperties({
        someAnotherKey: 'some value'
      });

      this.obj = Em.Object.create({
        prop1: 'abc',
        prop2: 'cba',
        prop3: 'aaa',
        prop4: Em.computed.concat(' ', 'prop1', 'prop2', 'prop3'),
        prop5: Em.computed.concat(' ', 'App.someRandomTestingKey', 'prop2', 'prop3'),
        prop6: Em.computed.concat(' ')
      });
    });

    it('should concat dependent values', function () {
      expect(this.obj.get('prop4')).to.equal('abc cba aaa');
    });

    it('should concat dependent values (2)', function () {
      this.obj.set('prop1', 'aaa');
      expect(this.obj.get('prop4')).to.equal('aaa cba aaa');
    });

    it('`prop5` depends on App.* key', function () {
      expect(this.obj.get('prop5')).to.equal('some value cba aaa');
      App.set('someAnotherKey', '');
      expect(this.obj.get('prop5')).to.equal(' cba aaa');
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop2', 'prop3']);
    });

    it('prop6 without dependent keys', function () {
      expect(this.obj.get('prop6')).to.equal('');
    });

  });

  describe('#notExistsIn', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 'v1',
        prop2: Em.computed.notExistsIn('prop1', ['v1', 'v2'])
      });
    });

    it('`false` if dependent value is in the array', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`false` if dependent value is in the array (2)', function () {
      this.obj.set('prop1', 'v2');
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if dependent value is not in the array', function () {
      this.obj.set('prop1', 'v3');
      expect(this.obj.get('prop2')).to.be.true;
    });

  });

  describe('#notExistsInByKey', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: 'v1',
        prop2: Em.computed.notExistsInByKey('prop1', 'prop3'),
        prop3: ['v1', 'v2']
      });
    });

    it('`false` if dependent value is in the array', function () {
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`false` if dependent value is in the array (2)', function () {
      this.obj.set('prop1', 'v2');
      expect(this.obj.get('prop2')).to.be.false;
    });

    it('`true` if dependent value is not in the array', function () {
      this.obj.set('prop1', 'v3');
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('`true` if dependent value is not in the array (2)', function () {
      this.obj.set('prop1', 'v1');
      this.obj.set('prop3', ['v2', 'v3']);
      expect(this.obj.get('prop2')).to.be.true;
    });

    it('prop2 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.eql(['prop1', 'prop3.[]']);
    });

  });

  describe('#firstNotBlank', function () {

    beforeEach(function () {

      App.setProperties({
        someAnotherKey: 'NOT-EMPTY-STRING'
      });

      this.obj = Em.Object.create({
        prop1: '',
        prop2: null,
        prop3: '1234',
        prop4: Em.computed.firstNotBlank('prop1', 'prop2', 'prop3'),
        prop5: Em.computed.firstNotBlank('prop1', 'App.someRandomTestingKey', 'prop3'),
        prop6: Em.computed.firstNotBlank('prop1', 'prop2')
      })
    });

    it('`prop4` check dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop4._dependentKeys).to.eql(['prop1', 'prop2', 'prop3']);
    });

    it('should returns prop3', function () {
      expect(this.obj.get('prop4')).to.equal('1234');
    });

    it('should returns prop2', function () {
      this.obj.set('prop2', 'not empty string');
      expect(this.obj.get('prop4')).to.equal('not empty string');
    });

    it('should returns prop1', function () {
      this.obj.set('prop2', 'not empty string');
      this.obj.set('prop1', 'prop1 is used');
      expect(this.obj.get('prop4')).to.equal('prop1 is used');
    });

    it('`prop5` depends on App.* key', function () {
      expect(this.obj.get('prop5')).to.equal('NOT-EMPTY-STRING');
      App.set('someAnotherKey', '!!!!!!!');
      expect(this.obj.get('prop5')).to.equal('!!!!!!!');
      App.set('someAnotherKey', null);
      expect(this.obj.get('prop5')).to.equal('1234');
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['prop1', 'App.someRandomTestingKey', 'prop3']);
    });

    it('prop6 depends on blank values', function () {
      expect(this.obj.get('prop6')).to.be.null;
    });

  });

  describe('#format', function () {

    beforeEach(function () {

      App.setProperties({
        someAnotherKey: 'some value'
      });

      this.obj = Em.Object.create({
        prop1: 'abc',
        prop2: 'cba',
        prop3: 'aaa',
        prop4: Em.computed.format('{0} {1} {2}', 'prop1', 'prop2', 'prop3'),
        prop5: Em.computed.format(null, 'prop1', 'prop2', 'prop3'),
        prop6: Em.computed.format('{0} {1} {2}', 'App.someRandomTestingKey', 'prop2', 'prop3')
      });
    });

    it('`prop4` check dependent keys', function () {
      expect(Em.meta(this.obj).descs.prop4._dependentKeys).to.eql(['prop1', 'prop2', 'prop3']);
    });

    it('should format message', function () {
      expect(this.obj.get('prop4')).to.equal('abc cba aaa');
    });

    it('should format message (2)', function () {
      this.obj.set('prop1', 'aaa');
      expect(this.obj.get('prop4')).to.equal('aaa cba aaa');
    });

    it('empty string for not existing i18-key', function () {
      expect(this.obj.get('prop5')).to.equal('');
    });

    it('`prop6` depends on App.* key', function () {
      expect(this.obj.get('prop6')).to.equal('some value cba aaa');
      App.set('someAnotherKey', '');
      expect(this.obj.get('prop6')).to.equal(' cba aaa');
    });

    it('prop6 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop6._dependentKeys).to.eql(['App.someRandomTestingKey', 'prop2', 'prop3']);
    });

  });

  describe('#formatUnavailable', function () {

    beforeEach(function () {
      App.setProperties({
        someAnotherKey: 1
      });

      this.obj = Em.Object.create({
        prop1: 1,
        prop2: Em.computed.formatUnavailable('prop1'),
        prop3: Em.computed.formatUnavailable('App.someRandomTestingKey')
      });
    });

    it('`value` is 1', function () {
      expect(this.obj.get('prop2')).to.equal(1);
      expect(this.obj.get('prop3')).to.equal(1);
    });

    it('`value` is 0', function () {
      App.set('someAnotherKey', 0);
      this.obj.set('prop1', 0);
      expect(this.obj.get('prop2')).to.equal(0);
      expect(this.obj.get('prop3')).to.equal(0);
    });

    it('`value` is `0`', function () {
      App.set('someAnotherKey', '0');
      this.obj.set('prop1', '0');
      expect(this.obj.get('prop2')).to.equal('0');
      expect(this.obj.get('prop3')).to.equal('0');
    });

    it('`value` is not numeric', function () {
      App.set('someAnotherKey', null);
      this.obj.set('prop1', null);
      expect(this.obj.get('prop2')).to.equal('n/a');
      expect(this.obj.get('prop3')).to.equal('n/a');
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['App.someRandomTestingKey']);
    });

  });

  describe('#countBasedMessage', function () {

    var msg0 = 'msg0';
    var msg1 = 'msg1';
    var msgM = 'msgM';

    beforeEach(function () {
      App.setProperties({
        someAnotherKey: 1
      });

      this.obj = Em.Object.create({
        prop1: 1,
        prop2: Em.computed.countBasedMessage('prop1', msg0, msg1, msgM),
        prop3: Em.computed.countBasedMessage('App.someRandomTestingKey', msg0, msg1, msgM)
      });
    });

    it('`value` is 1', function () {
      expect(this.obj.get('prop2')).to.equal(msg1);
      expect(this.obj.get('prop3')).to.equal(msg1);
    });

    it('`value` is 0', function () {
      App.set('someAnotherKey', 0);
      this.obj.set('prop1', 0);
      expect(this.obj.get('prop2')).to.equal(msg0);
      expect(this.obj.get('prop3')).to.equal(msg0);
    });

    it('`value` is greater than 1', function () {
      App.set('someAnotherKey', 3);
      this.obj.set('prop1', 3);
      expect(this.obj.get('prop2')).to.equal(msgM);
      expect(this.obj.get('prop3')).to.equal(msgM);
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['App.someRandomTestingKey']);
    });

  });

  describe('#getByKey', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: {a: 1, b: 2, c: 3},
        prop2: 'a',
        prop3: Em.computed.getByKey('prop1', 'prop2'),
        prop4: Em.computed.getByKey('prop1', 'App.someRandomTestingKey'),
        prop5: Em.computed.getByKey('prop1', 'prop2', 100500) // with default value
      });
      App.set('someAnotherKey', 'a');
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.eql(['prop1', 'prop2']);
    });

    it('prop4 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop4._dependentKeys).to.eql(['prop1', 'App.someRandomTestingKey']);
    });

    it('prop5 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop5._dependentKeys).to.eql(['prop1', 'prop2']);
    });

    it('prop3 value is 1', function () {
      expect(this.obj.get('prop3')).to.be.equal(1);
    });

    it('prop3 value is 2', function () {
      this.obj.set('prop2', 'b');
      expect(this.obj.get('prop3')).to.be.equal(2);
    });

    it('prop3 value is 3', function () {
      this.obj.set('prop2', 'c');
      expect(this.obj.get('prop3')).to.be.equal(3);
    });

    it('prop3 value is 4', function () {
      this.obj.set('prop1.c', 4);
      this.obj.set('prop2', 'c');
      expect(this.obj.get('prop3')).to.be.equal(4);
    });

    it('prop4 values is 1', function () {
      expect(this.obj.get('prop4')).to.be.equal(1);
    });

    it('prop4 values is 2', function () {
      App.set('someAnotherKey', 'b');
      expect(this.obj.get('prop4')).to.be.equal(2);
    });

    it('prop4 values is 3', function () {
      App.set('someAnotherKey', 'c');
      expect(this.obj.get('prop4')).to.be.equal(3);
    });

    it('prop5 value is set to the default value', function () {
      this.obj.set('prop2', 'd');
      expect(this.obj.get('prop5')).to.be.equal(100500);
    });

  });

  describe('#truncate', function () {

    beforeEach(function () {
      this.obj = Em.Object.create({
        prop1: '123456789',
        prop2: Em.computed.truncate('prop1', 8, 5),
        prop3: Em.computed.truncate('App.someRandomTestingKey', 8, 5),
        prop4: Em.computed.truncate('prop1', 8, 5, '###')
      });
      App.set('someAnotherKey', 'abcdefghi');
    });

    it('prop2 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop2._dependentKeys).to.be.eql(['prop1']);
    });

    it('prop3 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop3._dependentKeys).to.be.eql(['App.someRandomTestingKey']);
    });

    it('prop4 dependent keys are valid', function () {
      expect(Em.meta(this.obj).descs.prop4._dependentKeys).to.be.eql(['prop1']);
    });

    it('prop2 value is 12345...', function () {
      expect(this.obj.get('prop2')).to.be.equal('12345...');
    });

    it('prop2 value is 54321...', function () {
      this.obj.set('prop1', '543216789');
      expect(this.obj.get('prop2')).to.be.equal('54321...');
    });

    it('prop2 value is 1234', function () {
      this.obj.set('prop1', '1234');
      expect(this.obj.get('prop2')).to.be.equal('1234');
    });

    it('prop2 value is ""', function () {
      this.obj.set('prop1', null);
      expect(this.obj.get('prop2')).to.be.equal('');
    });

    it('prop3 value is abcde...', function () {
      expect(this.obj.get('prop3')).to.be.equal('abcde...');
    });

    it('prop3 value is edcba...', function () {
      App.set('someAnotherKey', 'edcbafghi');
      expect(this.obj.get('prop3')).to.be.equal('edcba...');
    });

    it('prop3 value is abcd', function () {
      App.set('someAnotherKey', 'abcd');
      expect(this.obj.get('prop3')).to.be.equal('abcd');
    });

    it('prop4 value is 12345###', function () {
      expect(this.obj.get('prop4')).to.be.equal('12345###');
    });

    it('prop4 value is 54321###', function () {
      this.obj.set('prop1', '543216789');
      expect(this.obj.get('prop4')).to.be.equal('54321###');
    });

    it('prop4 value is 12345', function () {
      this.obj.set('prop1', '12345');
      expect(this.obj.get('prop4')).to.be.equal('12345');
    });

  });

});