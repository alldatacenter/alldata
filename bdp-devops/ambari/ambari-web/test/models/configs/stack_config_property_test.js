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
var model;

function getModel() {
  return App.StackConfigProperty.createRecord();
}

describe('App.StackConfigProperty', function () {

  beforeEach(function () {
    model = getModel();
  });

  describe("#Attributes", function() {
    var testCases = [
      {
        propertyKey: 'type',
        propertyName: 'displayType',
        value: 't1',
        expectedValue: 't1',
        defaultValue: 'string'
      },
      {
        propertyKey: 'overridable',
        propertyName: 'isOverridable',
        value: false,
        expectedValue: false,
        defaultValue: true
      },
      {
        propertyKey: 'visible',
        propertyName: 'isVisible',
        value: false,
        expectedValue: false,
        defaultValue: true
      },
      {
        propertyKey: 'empty_value_valid',
        propertyName: 'isRequired',
        value: true,
        expectedValue: false,
        defaultValue: true
      },
      {
        propertyKey: 'editable_only_at_install',
        propertyName: 'isReconfigurable',
        value: true,
        expectedValue: false,
        defaultValue: true
      },
      {
        propertyKey: 'show_property_name',
        propertyName: 'showLabel',
        value: false,
        expectedValue: false,
        defaultValue: true
      },
      {
        propertyKey: 'read_only',
        propertyName: 'isEditable',
        value: false,
        expectedValue: false,
        defaultValue: true
      },
      {
        propertyKey: 'unit',
        propertyName: 'unit',
        value: 'mb',
        expectedValue: 'mb',
        defaultValue: ''
      }
    ];

    testCases.forEach(function(test) {

      it("valueAttributes is null, " + test.propertyName + " should be " + test.defaultValue, function() {
        model.set('valueAttributes', null);
        expect(model.get(test.propertyName)).to.equal(test.defaultValue);
      });

      it("valueAttributes is object, " + test.propertyName + " should be " + test.expectedValue, function() {
        var valueAttributes = {};
        valueAttributes[test.propertyKey] = test.value;
        model.set('valueAttributes', valueAttributes);
        expect(model.get(test.propertyName)).to.equal(test.expectedValue);
      });

    });
  });

  describe("#getAttribute()", function() {

    it("valueAttributes is null", function() {
      model.set('valueAttributes', null);
      expect(model.getAttribute('attr1', 'defVal')).to.equal('defVal');
    });

    it("valueAttributes is empty object", function() {
      model.set('valueAttributes', {});
      expect(model.getAttribute('attr1', 'defVal')).to.equal('defVal');
    });

    it("valueAttributes is correct object", function() {
      model.set('valueAttributes', {attr1: 'val'});
      expect(model.getAttribute('attr1', 'defVal')).to.equal('val');
    });

  });

});
