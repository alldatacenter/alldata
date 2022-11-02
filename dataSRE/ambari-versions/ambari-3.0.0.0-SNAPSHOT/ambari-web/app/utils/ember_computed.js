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

var computed = Em.computed;
var get = Em.get;
var makeArray = Em.makeArray;

var slice = [].slice;

var dataUtils = require('utils/data_manipulation');

/**
 * Returns hash with values of name properties from the context
 * If <code>propertyName</code> starts with 'App.', <code>App</code> is used as context, <code>self</code> used otherwise
 * If some <code>propertyName</code> starts with '!' its value will be inverted
 *
 * @param {object} self current context
 * @param {string[]} propertyNames needed properties
 * @returns {object} hash with needed values
 */
function getProperties(self, propertyNames) {
  var ret = {};
  for (var i = 0; i < propertyNames.length; i++) {
    var propertyName = propertyNames[i];
    var shouldBeInverted = propertyName.startsWith('!');
    propertyName = shouldBeInverted ? propertyName.substr(1) : propertyName;
    var isApp = propertyName.startsWith('App.');
    var name = isApp ? propertyName.replace('App.', '') : propertyName;
    var value = isApp ? App.get(name) : self.get(name);
    value = shouldBeInverted ? !value : value;
    ret[propertyName] = value;
  }
  return ret;
}

/**
 * Returns value of named property from the context
 * If <code>propertyName</code> starts with 'App.', <code>App</code> is used as context, <code>self</code> used otherwise
 *
 * @param {object} self current context
 * @param {string} propertyName needed property
 * @returns {*} needed value
 */
function smartGet(self, propertyName) {
  var isApp = propertyName.startsWith('App.');
  var name = isApp ? propertyName.replace('App.', '') : propertyName;
  return isApp ? App.get(name) : self.get(name);
}

/**
 * Returns list with values of name properties from the context
 * If <code>propertyName</code> starts with 'App.', <code>App</code> is used as context, <code>self</code> used otherwise
 *
 * @param {object} self current context
 * @param {string[]} propertyNames needed properties
 * @returns {array} list of needed values
 */
function getValues(self, propertyNames) {
  return propertyNames.map(function (propertyName) {
    return smartGet(self, propertyName);
  });
}

function generateComputedWithKey(macro) {
  return function () {
    var properties = slice.call(arguments, 1);
    var key = arguments[0];
    var computedFunc = computed(function () {
      var values = getValues(this, properties);
      return macro.call(this, key, values);
    });

    return computedFunc.property.apply(computedFunc, properties);
  }
}

function generateComputedWithProperties(macro) {
  return function () {
    var properties = slice.call(arguments);
    var computedFunc = computed(function () {
      return macro.apply(this, [getProperties(this, properties)]);
    });

    var realProperties = properties.slice().invoke('replace', '!', '');
    return computedFunc.property.apply(computedFunc, realProperties);
  };
}

function generateComputedWithValues(macro) {
  return function () {
    var properties = slice.call(arguments);
    var computedFunc = computed(function () {
      return macro.apply(this, [getValues(this, properties)]);
    });

    return computedFunc.property.apply(computedFunc, properties);
  };
}

/**
 *
 * A computed property that returns true if the provided dependent property
 * is equal to the given value.
 * App.*-keys are supported
 * Example*
 * ```javascript
 * var Hamster = Ember.Object.extend({
 *    napTime: Ember.computed.equal('state', 'sleepy')
 *  });
 * var hamster = Hamster.create();
 * hamster.get('napTime'); // false
 * hamster.set('state', 'sleepy');
 * hamster.get('napTime'); // true
 * hamster.set('state', 'hungry');
 * hamster.get('napTime'); // false
 * ```
 * @method equal
 * @param {String} dependentKey
 * @param {String|Number|Object} value
 * @return {Ember.ComputedProperty} computed property which returns true if
 * the original value for property is equal to the given value.
 * @public
 */
computed.equal = function (dependentKey, value) {
  return computed(dependentKey, function () {
    return smartGet(this, dependentKey) === value;
  }).cacheable();
};

/**
 * A computed property that returns true if the provided dependent property is not equal to the given value
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 'a',
 *  p2: Em.computed.notEqual('p1', 'a')
 * });
 * console.log(o.get('p2')); // false
 * o.set('p1', 'b');
 * console.log(o.get('p2')); // true
 * </pre>
 *
 * @method notEqual
 * @param {string} dependentKey
 * @param {*} value
 * @returns {Ember.ComputedProperty}
 */
computed.notEqual = function (dependentKey, value) {
  return computed(dependentKey, function () {
    return smartGet(this, dependentKey) !== value;
  });
};

/**
 * A computed property that returns true if provided dependent properties are equal to the each other
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 'a',
 *  p2: 'b',
 *  p3: Em.computed.equalProperties('p1', 'p2')
 * });
 * console.log(o.get('p3')); // false
 * o.set('p1', 'b');
 * console.log(o.get('p3')); // true
 * </pre>
 *
 * @method equalProperties
 * @param {string} dependentKey1
 * @param {string} dependentKey2
 * @returns {Ember.ComputedProperty}
 */
computed.equalProperties = function (dependentKey1, dependentKey2) {
  return computed(dependentKey1, dependentKey2, function () {
    return smartGet(this, dependentKey1) === smartGet(this, dependentKey2);
  });
};

/**
 * A computed property that returns true if provided dependent properties are not equal to the each other
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 'a',
 *  p2: 'b',
 *  p3: Em.computed.notEqualProperties('p1', 'p2')
 * });
 * console.log(o.get('p3')); // true
 * o.set('p1', 'b');
 * console.log(o.get('p3')); // false
 * </pre>
 *
 * @method notEqualProperties
 * @param {string} dependentKey1
 * @param {string} dependentKey2
 * @returns {Ember.ComputedProperty}
 */
computed.notEqualProperties = function (dependentKey1, dependentKey2) {
  return computed(dependentKey1, dependentKey2, function () {
    return smartGet(this, dependentKey1) !== smartGet(this, dependentKey2);
  });
};

/**
 * A computed property that returns grouped collection's items by propertyName-value
 *
 * @method groupBy
 * @param {string} collectionKey
 * @param {string} propertyName
 * @returns {Ember.ComputedProperty}
 */
computed.groupBy = function (collectionKey, propertyName) {
  return computed(collectionKey + '.@each.' + propertyName, function () {
    var collection = get(this, collectionKey);
    return dataUtils.groupPropertyValues(collection, propertyName);
  });
};

/**
 * A computed property that returns filtered collection by propertyName values-list
 * Wrapper to filterProperty-method that allows using list of values to filter
 *
 * @method filterByMany
 * @param {string} collectionKey
 * @param {string} propertyName
 * @param {array} valuesToFilter
 * @returns {Ember.ComputedProperty}
 */
computed.filterByMany = function (collectionKey, propertyName, valuesToFilter) {
  return computed(collectionKey + '.@each.' + propertyName, function () {
    var collection = get(this, collectionKey);
    return dataUtils.filterPropertyValues(collection, propertyName, makeArray(valuesToFilter));
  });
};

/**
 * A computed property that returns collection without elements with value that is in <code>valuesToReject</code>
 * Exclude objects from <code>collection</code> if its <code>key</code> exist in <code>valuesToReject</code>
 *
 * @method rejectMany
 * @param {string} collectionKey
 * @param {string} propertyName
 * @param {array} valuesToReject
 * @returns {Ember.ComputedProperty}
 */
computed.rejectMany = function (collectionKey, propertyName, valuesToReject) {
  return computed(collectionKey + '.@each.' + propertyName, function () {
    var collection = get(this, collectionKey);
    return dataUtils.rejectPropertyValues(collection, propertyName, makeArray(valuesToReject));
  });
};

/**
 * A computed property that returns trueValue if dependent value is true and falseValue otherwise
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: true,
 *  p2: Em.computed.ifThenElse('p1', 'abc', 'cba')
 * });
 * console.log(o.get('p2')); // 'abc'
 * o.set('p1', false);
 * console.log(o.get('p2')); // 'cba'
 * </pre>
 *
 * @method ifThenElse
 * @param {string} dependentKey
 * @param {*} trueValue
 * @param {*} falseValue
 * @returns {Ember.ComputedProperty}
 */
computed.ifThenElse = function (dependentKey, trueValue, falseValue) {
  return computed(dependentKey, function () {
    return smartGet(this, dependentKey) ? trueValue : falseValue;
  });
};

/**
 * A computed property that returns value for trueKey property if dependent value is true and falseKey-value otherwise
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: true,
 *  p2: 1,
 *  p3: 2,
 *  p4: Em.computed.ifThenElseByKeys('p1', 'p2', 'p3')
 * });
 * console.log(o.get('p4')); // 1
 * o.set('p1', false);
 * console.log(o.get('p4')); // 2
 *
 * o.set('p3', 3);
 * console.log(o.get('p4')); // 3
 * </pre>
 *
 * @method ifThenElseByKeys
 * @param {string} dependentKey
 * @param {string} trueKey
 * @param {string} falseKey
 * @returns {Ember.ComputedProperty}
 */
computed.ifThenElseByKeys = function (dependentKey, trueKey, falseKey) {
  return computed(dependentKey, trueKey, falseKey, function () {
    return smartGet(this, dependentKey) ? smartGet(this, trueKey) : smartGet(this, falseKey);
  });
};

/**
 * A computed property that is equal to the logical 'and'
 * Takes any number of arguments
 * Returns true if all of them are truly, false - otherwise
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: true,
 *  p2: true,
 *  p3: true,
 *  p4: Em.computed.and('p1', 'p2', 'p3')
 * });
 * console.log(o.get('p4')); // true
 * o.set('p1', false);
 * console.log(o.get('p4')); // false
 * </pre>
 *
 * @method and
 * @param {...string} dependentKeys
 * @returns {Ember.ComputedProperty}
 */
computed.and = generateComputedWithProperties(function (properties) {
  var value;
  for (var key in properties) {
    value = !!properties[key];
    if (properties.hasOwnProperty(key) && !value) {
      return false;
    }
  }
  return value;
});

/**
 * A computed property that is equal to the logical 'or'
 * Takes any number of arguments
 * Returns true if at least one of them is truly, false - otherwise
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: false,
 *  p2: false,
 *  p3: false,
 *  p4: Em.computed.or('p1', 'p2', 'p3')
 * });
 * console.log(o.get('p4')); // false
 * o.set('p1', true);
 * console.log(o.get('p4')); // true
 * </pre>
 *
 * @method or
 * @param {...string} dependentKeys
 * @returns {Ember.ComputedProperty}
 */
computed.or = generateComputedWithProperties(function (properties) {
  var value;
  for (var key in properties) {
    value = !!properties[key];
    if (properties.hasOwnProperty(key) && value) {
      return value;
    }
  }
  return value;
});

/**
 * A computed property that returns sum on the dependent properties values
 * Takes any number of arguments
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 1,
 *  p2: 2,
 *  p3: 3,
 *  p4: Em.computed.sumProperties('p1', 'p2', 'p3')
 * });
 * console.log(o.get('p4')); // 6
 * o.set('p1', 2);
 * console.log(o.get('p4')); // 7
 * </pre>
 *
 * @method sumProperties
 * @param {...string} dependentKeys
 * @returns {Ember.ComputedProperty}
 */
computed.sumProperties = generateComputedWithProperties(function (properties) {
  var sum = 0;
  for (var key in properties) {
    if (properties.hasOwnProperty(key)) {
      sum += Number(properties[key]);
    }
  }
  return sum;
});

/**
 * A computed property that returns true if dependent value is greater or equal to the needed value
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 4,
 *  p2: Em.computed.gte('p1', 1)
 * });
 * console.log(o.get('p2')); // true
 * o.set('p1', 4);
 * console.log(o.get('p2')); // true
 * o.set('p1', 5);
 * console.log(o.get('p2')); // false
 * </pre>
 *
 * @method gte
 * @param {string} dependentKey
 * @param {*} value
 * @returns {Ember.ComputedProperty}
 */
computed.gte = function (dependentKey, value) {
  return computed(dependentKey, function () {
    return smartGet(this, dependentKey) >= value;
  });
};

/**
 * A computed property that returns true if first dependent property is greater or equal to the second dependent property
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 4,
 *  p2: 1,
 *  p3: Em.computed.gteProperties('p1', 'p2')
 * });
 * console.log(o.get('p3')); // true
 * o.set('p2', 4);
 * console.log(o.get('p3')); // true
 * o.set('p2', 5);
 * console.log(o.get('p3')); // false
 * </pre>
 *
 * @method gteProperties
 * @param {string} dependentKey1
 * @param {string} dependentKey2
 * @returns {Ember.ComputedProperty}
 */
computed.gteProperties = function (dependentKey1, dependentKey2) {
  return computed(dependentKey1, dependentKey2, function () {
    return smartGet(this, dependentKey1) >= smartGet(this, dependentKey2);
  });
};

/**
 * A computed property that returns true if dependent property is less or equal to the needed value
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 4,
 *  p2: Em.computed.lte('p1', 1)
 * });
 * console.log(o.get('p2')); // false
 * o.set('p1', 4);
 * console.log(o.get('p2')); // true
 * o.set('p1', 5);
 * console.log(o.get('p2')); // true
 * </pre>
 *
 * @method lte
 * @param {string} dependentKey
 * @param {*} value
 * @returns {Ember.ComputedProperty}
 */
computed.lte = function (dependentKey, value) {
  return computed(dependentKey, function () {
    return smartGet(this, dependentKey) <= value;
  });
};

/**
 * A computed property that returns true if first dependent property is less or equal to the second dependent property
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 4,
 *  p2: 1,
 *  p3: Em.computed.lteProperties('p1', 'p2')
 * });
 * console.log(o.get('p3')); // false
 * o.set('p2', 4);
 * console.log(o.get('p3')); // true
 * o.set('p2', 5);
 * console.log(o.get('p3')); // true
 * </pre>
 *
 * @method lteProperties
 * @param {string} dependentKey1
 * @param {string} dependentKey2
 * @returns {Ember.ComputedProperty}
 */
computed.lteProperties = function (dependentKey1, dependentKey2) {
  return computed(dependentKey1, dependentKey2, function () {
    return smartGet(this, dependentKey1) <= smartGet(this, dependentKey2);
  });
};

/**
 * A computed property that returns true if dependent value is greater than the needed value
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 4,
 *  p2: Em.computed.gt('p1', 1)
 * });
 * console.log(o.get('p2')); // true
 * o.set('p1', 4);
 * console.log(o.get('p2')); // false
 * o.set('p1', 5);
 * console.log(o.get('p2')); // false
 * </pre>
 *
 * @method gt
 * @param {string} dependentKey
 * @param {*} value
 * @returns {Ember.ComputedProperty}
 */
computed.gt = function (dependentKey, value) {
  return computed(dependentKey, function () {
    return smartGet(this, dependentKey) > value;
  });
};

/**
 * A computed property that returns true if first dependent property is greater than the second dependent property
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 4,
 *  p2: 1,
 *  p3: Em.computed.gteProperties('p1', 'p2')
 * });
 * console.log(o.get('p3')); // true
 * o.set('p2', 4);
 * console.log(o.get('p3')); // false
 * o.set('p2', 5);
 * console.log(o.get('p3')); // false
 * </pre>
 *
 * @method gtProperties
 * @param {string} dependentKey1
 * @param {string} dependentKey2
 * @returns {Ember.ComputedProperty}
 */
computed.gtProperties = function (dependentKey1, dependentKey2) {
  return computed(dependentKey1, dependentKey2, function () {
    return smartGet(this, dependentKey1) > smartGet(this, dependentKey2);
  });
};

/**
 * A computed property that returns true if dependent value is less than the needed value
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 4,
 *  p2: Em.computed.lt('p1', 1)
 * });
 * console.log(o.get('p2')); // false
 * o.set('p1', 4);
 * console.log(o.get('p2')); // false
 * o.set('p1', 5);
 * console.log(o.get('p2')); // true
 * </pre>
 *
 * @method lt
 * @param {string} dependentKey
 * @param {*} value
 * @returns {Ember.ComputedProperty}
 */
computed.lt = function (dependentKey, value) {
  return computed(dependentKey, function () {
    return smartGet(this, dependentKey) < value;
  });
};

/**
 * A computed property that returns true if first dependent property is less than the second dependent property
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 4,
 *  p2: 1,
 *  p3: Em.computed.ltProperties('p1', 'p2')
 * });
 * console.log(o.get('p3')); // false
 * o.set('p2', 4);
 * console.log(o.get('p3')); // false
 * o.set('p2', 5);
 * console.log(o.get('p3')); // true
 * </pre>
 *
 * @method gtProperties
 * @param {string} dependentKey1
 * @param {string} dependentKey2
 * @returns {Ember.ComputedProperty}
 */
computed.ltProperties = function (dependentKey1, dependentKey2) {
  return computed(dependentKey1, dependentKey2, function () {
    return smartGet(this, dependentKey1) < smartGet(this, dependentKey2);
  });
};

/**
 * A computed property that returns true if dependent property is match to the needed regular expression
 * <pre>
 * var o = Em.Object.create({
 *  p1: 'abc',
 *  p2: Em.computed.match('p1', /^a/)
 * });
 * console.log(o.get('p2')); // true
 * o.set('p1', 'bc');
 * console.log(o.get('p2')); // false
 * </pre>
 *
 * @method match
 * @param {string} dependentKey
 * @param {RegExp} regexp
 * @returns {Ember.ComputedProperty}
 */
computed.match = function (dependentKey, regexp) {
  return computed(dependentKey, function () {
    var value = get(this, dependentKey);
    if (!regexp) {
      return false;
    }
    return regexp.test(value);
  });
};

/**
 * A computed property that returns true of some collection's item has property with needed value
 * <pre>
 * var o = Em.Object.create({
 *  p1: [{a: 1}, {a: 2}, {a: 3}],
 *  p2: Em.computed.someBy('p1', 'a', 1)
 * });
 * console.log(o.get('p2')); // true
 * o.set('p1.0.a', 2);
 * console.log(o.get('p2')); // false
 * </pre>
 *
 * @method someBy
 * @param {string} collectionKey
 * @param {string} propertyName
 * @param {*} neededValue
 * @returns {Ember.ComputedProperty}
 */
computed.someBy = function (collectionKey, propertyName, neededValue) {
  return computed(collectionKey + '.@each.' + propertyName, function () {
    var collection = smartGet(this, collectionKey);
    if (!collection) {
      return false;
    }
    return collection.someProperty(propertyName, neededValue);
  });
};

/**
 * A computed property that returns true of some collection's item has property with needed value
 * Needed value is stored in the another property
 * <pre>
 * var o = Em.Object.create({
 *  p1: [{a: 1}, {a: 2}, {a: 3}],
 *  p2: Em.computed.someByKey('p1', 'a', 'v1'),
 *  v1: 1
 * });
 * console.log(o.get('p2')); // true
 * o.set('p1.0.a', 2);
 * console.log(o.get('p2')); // false
 * </pre>
 *
 * @method someByKey
 * @param {string} collectionKey
 * @param {string} propertyName
 * @param {string} neededValueKey
 * @returns {Ember.ComputedProperty}
 */
computed.someByKey = function (collectionKey, propertyName, neededValueKey) {
  return computed(collectionKey + '.@each.' + propertyName, neededValueKey, function () {
    var collection = smartGet(this, collectionKey);
    if (!collection) {
      return false;
    }
    var neededValue = smartGet(this, neededValueKey);
    return collection.someProperty(propertyName, neededValue);
  });
};

/**
 * A computed property that returns true of all collection's items have property with needed value
 * <pre>
 * var o = Em.Object.create({
 *  p1: [{a: 1}, {a: 1}, {a: 1}],
 *  p2: Em.computed.everyBy('p1', 'a', 1)
 * });
 * console.log(o.get('p2')); // true
 * o.set('p1.0.a', 2);
 * console.log(o.get('p2')); // false
 * </pre>
 *
 * @method everyBy
 * @param {string} collectionKey
 * @param {string} propertyName
 * @param {*} neededValue
 * @returns {Ember.ComputedProperty}
 */
computed.everyBy = function (collectionKey, propertyName, neededValue) {
  return computed(collectionKey + '.@each.' + propertyName, function () {
    var collection = smartGet(this, collectionKey);
    if (!collection) {
      return false;
    }
    return collection.everyProperty(propertyName, neededValue);
  });
};

/**
 * A computed property that returns true of all collection's items have property with needed value
 * Needed value is stored in the another property
 * <pre>
 * var o = Em.Object.create({
 *  p1: [{a: 1}, {a: 1}, {a: 1}],
 *  p2: Em.computed.everyByKey('p1', 'a', 'v1'),
 *  v1: 1
 * });
 * console.log(o.get('p2')); // true
 * o.set('p1.0.a', 2);
 * console.log(o.get('p2')); // false
 * </pre>
 *
 * @method everyByKey
 * @param {string} collectionKey
 * @param {string} propertyName
 * @param {string} neededValueKey
 * @returns {Ember.ComputedProperty}
 */
computed.everyByKey = function (collectionKey, propertyName, neededValueKey) {
  return computed(collectionKey + '.@each.' + propertyName, neededValueKey, function () {
    var collection = smartGet(this, collectionKey);
    if (!collection) {
      return false;
    }
    var neededValue = smartGet(this, neededValueKey);
    return collection.everyProperty(propertyName, neededValue);
  });
};

/**
 * A computed property that returns array with values of named property on all items in the collection
 * <pre>
 * var o = Em.Object.create({
 *  p1: [{a: 1}, {a: 2}, {a: 3}],
 *  p2: Em.computed.everyBy('p1', 'a')
 * });
 * console.log(o.get('p2')); // [1, 2, 3]
 * o.set('p1.0.a', 2);
 * console.log(o.get('p2')); // [2, 2, 3]
 * </pre>
 *
 * @method mapBy
 * @param {string} collectionKey
 * @param {string} propertyName
 * @returns {Ember.ComputedProperty}
 */
computed.mapBy = function (collectionKey, propertyName) {
  return computed(collectionKey + '.@each.' + propertyName, function () {
    var collection = smartGet(this, collectionKey);
    if (!collection) {
      return [];
    }
    return collection.mapProperty(propertyName);
  });
};

/**
 * A computed property that returns array with collection's items that have needed property value
 * <pre>
 * var o = Em.Object.create({
 *  p1: [{a: 1}, {a: 2}, {a: 3}],
 *  p2: Em.computed.filterBy('p1', 'a', 2)
 * });
 * console.log(o.get('p2')); // [{a: 2}]
 * o.set('p1.0.a', 2);
 * console.log(o.get('p2')); // [{a: 2}, {a: 2}]
 * </pre>
 *
 * @method filterBy
 * @param {string} collectionKey
 * @param {string} propertyName
 * @param {*} neededValue
 * @returns {Ember.ComputedProperty}
 */
computed.filterBy = function (collectionKey, propertyName, neededValue) {
  return computed(collectionKey + '.@each.' + propertyName, function () {
    var collection = smartGet(this, collectionKey);
    if (!collection) {
      return [];
    }
    return collection.filterProperty(propertyName, neededValue);
  });
};

/**
 * A computed property that returns array with collection's items that have needed property value
 * Needed value is stored in the another property
 *
 * <pre>
 * var o = Em.Object.create({
 *  p1: [{a: 1}, {a: 2}, {a: 3}],
 *  p2: Em.computed.filterByKey('p1', 'a', 'v1'),
 *  v1: 2
 * });
 * console.log(o.get('p2')); // [{a: 2}]
 * o.set('p1.0.a', 2);
 * console.log(o.get('p2')); // [{a: 2}, {a: 2}]
 * </pre>
 *
 * @method filterByKey
 * @param {string} collectionKey
 * @param {string} propertyName
 * @param {string} neededValueKey
 * @returns {Ember.ComputedProperty}
 */
computed.filterByKey = function (collectionKey, propertyName, neededValueKey) {
  return computed(collectionKey + '.@each.' + propertyName, neededValueKey, function () {
    var collection = smartGet(this, collectionKey);
    if (!collection) {
      return [];
    }
    var neededValue = smartGet(this, neededValueKey);
    return collection.filterProperty(propertyName, neededValue);
  });
};

/**
 * A computed property that returns first collection's item that has needed property value
 * <pre>
 * var o = Em.Object.create({
 *  p1: [{a: 1, b: 1}, {a: 2, b: 2}, {a: 3, b: 3}],
 *  p2: Em.computed.findBy('p1', 'a', 2)
 * });
 * console.log(o.get('p2')); // [{a: 2, b: 2}]
 * o.set('p1.0.a', 2);
 * console.log(o.get('p2')); // [{a: 2, b: 1}]
 * </pre>
 *
 * @method findBy
 * @param {string} collectionKey
 * @param {string} propertyName
 * @param {*} neededValue
 * @returns {Ember.ComputedProperty}
 */
computed.findBy = function (collectionKey, propertyName, neededValue) {
  return computed(collectionKey + '.@each.' + propertyName, function () {
    var collection = smartGet(this, collectionKey);
    if (!collection) {
      return null;
    }
    return collection.findProperty(propertyName, neededValue);
  });
};

/**
 * A computed property that returns first collection's item that has needed property value
 * Needed value is stored in the another property
 * <pre>
 * var o = Em.Object.create({
 *  p1: [{a: 1, b: 1}, {a: 2, b: 2}, {a: 3, b: 3}],
 *  p2: Em.computed.findByKey('p1', 'a', 'v1'),
 *  v1: 2
 * });
 * console.log(o.get('p2')); // [{a: 2, b: 2}]
 * o.set('p1.0.a', 2);
 * console.log(o.get('p2')); // [{a: 2, b: 1}]
 * </pre>
 *
 * @method findByKey
 * @param {string} collectionKey
 * @param {string} propertyName
 * @param {string} neededValueKey
 * @returns {Ember.ComputedProperty}
 */
computed.findByKey = function (collectionKey, propertyName, neededValueKey) {
  return computed(collectionKey + '.@each.' + propertyName, neededValueKey, function () {
    var collection = smartGet(this, collectionKey);
    if (!collection) {
      return null;
    }
    var neededValue = smartGet(this, neededValueKey);
    return collection.findProperty(propertyName, neededValue);
  });
};

/**
 * A computed property that returns value equal to the dependent
 * Should be used as 'short-name' for deeply-nested values
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: {a: {b: {c: 2}}},
 *  p2: Em.computed.alias('p1.a.b.c')
 * });
 * console.log(o.get('p2')); // 2
 * o.set('p1.a.b.c', 4);
 * console.log(o.get('p2')); // 4
 * </pre>
 *
 * @method alias
 * @param {string} dependentKey
 * @returns {Ember.ComputedProperty}
 */
computed.alias = function (dependentKey) {
  return computed(dependentKey, function () {
    return smartGet(this, dependentKey);
  });
};

/**
 * A computed property that returns true if dependent property exists in the needed values
 * <pre>
 * var o = Em.Object.create({
 *  p1: 2,
 *  p2: Em.computed.existsIn('p1', [1, 2, 3])
 * });
 * console.log(o.get('p2')); // true
 * o.set('p1', 4);
 * console.log(o.get('p2')); // false
 * </pre>
 *
 * @method existsIn
 * @param {string} dependentKey
 * @param {array} neededValues
 * @returns {Ember.ComputedProperty}
 */
computed.existsIn = function (dependentKey, neededValues) {
  return computed(dependentKey, function () {
    var value = smartGet(this, dependentKey);
    return makeArray(neededValues).contains(value);
  });
};

/**
 * A computed property that returns true if dependent property exists in the property with needed values
 * <pre>
 * var o = Em.Object.create({
 *  p1: 2,
 *  p2: Em.computed.existsInByKey('p1', 'p3'),
 *  p3: [1, 2, 3]
 * });
 * console.log(o.get('p2')); // true
 * o.set('p1', 4);
 * console.log(o.get('p2')); // false
 * </pre>
 *
 * @method existsIn
 * @param {string} dependentKey
 * @param {string} neededValuesKey
 * @returns {Ember.ComputedProperty}
 */
computed.existsInByKey = function (dependentKey, neededValuesKey) {
  return computed(dependentKey, `${neededValuesKey}.[]`, function () {
    var value = smartGet(this, dependentKey);
    var neededValues = smartGet(this, neededValuesKey);
    return makeArray(neededValues).contains(value);
  });
};

/**
 * A computed property that returns true if dependent property doesn't exist in the needed values
 * <pre>
 * var o = Em.Object.create({
 *  p1: 2,
 *  p2: Em.computed.notExistsIn('p1', [1, 2, 3])
 * });
 * console.log(o.get('p2')); // false
 * o.set('p1', 4);
 * console.log(o.get('p2')); // true
 * </pre>
 *
 * @method notExistsIn
 * @param {string} dependentKey
 * @param {array} neededValues
 * @returns {Ember.ComputedProperty}
 */
computed.notExistsIn = function (dependentKey, neededValues) {
  return computed(dependentKey, function () {
    var value = smartGet(this, dependentKey);
    return !makeArray(neededValues).contains(value);
  });
};

/**
 * A computed property that returns true if dependent property doesn't exist in the property with needed values
 * <pre>
 * var o = Em.Object.create({
 *  p1: 2,
 *  p2: Em.computed.notExistsInByKey('p1', 'p3'),
 *  p3: [1, 2, 3]
 * });
 * console.log(o.get('p2')); // false
 * o.set('p1', 4);
 * console.log(o.get('p2')); // true
 * </pre>
 *
 * @method notExistsInByKey
 * @param {string} dependentKey
 * @param {string} neededValuesKey
 * @returns {Ember.ComputedProperty}
 */
computed.notExistsInByKey = function (dependentKey, neededValuesKey) {
  return computed(dependentKey, `${neededValuesKey}.[]`, function () {
    var value = smartGet(this, dependentKey);
    var neededValues = smartGet(this, neededValuesKey);
    return !makeArray(neededValues).contains(value);
  });
};

/**
 * A computed property that returns result of calculation <code>(dependentProperty1/dependentProperty2 * 100)</code>
 * If accuracy is 0 (by default), result is rounded to integer
 * Otherwise - result is float with provided accuracy
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 2,
 *  p2: 4,
 *  p3: Em.computed.percents('p1', 'p2')
 * });
 * console.log(o.get('p3')); // 50
 * o.set('p2', 5);
 * console.log(o.get('p3')); // 40
 * </pre>
 *
 * @method percents
 * @param {string} dependentKey1
 * @param {string} dependentKey2
 * @param {number} [accuracy=0]
 * @returns {Ember.ComputedProperty}
 */
computed.percents = function (dependentKey1, dependentKey2, accuracy) {
  if (arguments.length < 3) {
    accuracy = 0;
  }
  return computed(dependentKey1, dependentKey2, function () {
    var v1 = Number(smartGet(this, dependentKey1));
    var v2 = Number(smartGet(this, dependentKey2));
    var result = v1 / v2 * 100;
    if (0 === accuracy) {
      return Math.round(result);
    }
    return parseFloat(result.toFixed(accuracy));
  });
};

/**
 * A computed property that returns result of <code>App.format.role</code> for dependent value
 * <pre>
 * var o = Em.Object.create({
 *  p1: 'SECONDARY_NAMENODE',
 *  p3: Em.computed.formatRole('p1', false)
 * });
 * console.log(o.get('p2')); // 'SNameNode'
 * o.set('p1', 'FLUME_HANDLER);
 * console.log(o.get('p2')); // 'Flume'
 * </pre>
 *
 * @method formatRole
 * @param {string} dependentKey
 * @param {boolean} isServiceRole
 * @returns {Ember.ComputedProperty}
 */
computed.formatRole = function (dependentKey, isServiceRole) {
  return computed(dependentKey, function () {
    var value = get(this, dependentKey);
    return App.format.role(value, isServiceRole);
  });
};

/**
 * A computed property that returns sum of the named property in the each collection's item
 * <pre>
 * var o = Em.Object.create({
 *  p1: [{a: 1}, {a: 2}, {a: 3}],
 *  p2: Em.computed.sumBy('p1', 'a')
 * });
 * console.log(o.get('p2')); // 6
 * o.set('p1.0.a', 2);
 * console.log(o.get('p2')); // 7
 * </pre>
 *
 * @method sumBy
 * @param {string} collectionKey
 * @param {string} propertyName
 * @returns {Ember.ComputedProperty}
 */
computed.sumBy = function (collectionKey, propertyName) {
  return computed(collectionKey + '.@each.' + propertyName, function () {
    var collection = smartGet(this, collectionKey);
    if (Em.isEmpty(collection)) {
      return 0;
    }
    var sum = 0;
    collection.forEach(function (item) {
      sum += Number(get(item, propertyName));
    });
    return sum;
  });
};

/**
 * A computed property that returns I18n-string formatted with dependent properties
 * Takes at least one argument
 * App.*-keys are supported
 *
 * @param {string} key key in the I18n-messages
 * @param {...string} dependentKeys
 * @method i18nFormat
 * @returns {Ember.ComputedProperty}
 */
computed.i18nFormat = generateComputedWithKey(function (key, dependentValues) {
  var str = Em.I18n.t(key);
  if (!str) {
    return '';
  }
  return str.format.apply(str, dependentValues);
});

/**
 * A computed property that returns string formatted with dependent properties
 * Takes at least one argument
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 'abc',
 *  p2: 'cba',
 *  p3: Em.computed.format('{0} => {1}', 'p1', 'p2')
 * });
 * console.log(o.get('p3')); // 'abc => cba'
 * o.set('p1', 'aaa');
 * console.log(o.get('p3')); // 'aaa => cba'
 * </pre>
 *
 * @param {string} str string to format
 * @param {...string} dependentKeys
 * @method format
 * @returns {Ember.ComputedProperty}
 */
computed.format = generateComputedWithKey(function (str, dependentValues) {
  if (!str) {
    return '';
  }
  return str.format.apply(str, dependentValues);
});

/**
 * A computed property that returns dependent values joined with separator
 * Takes at least one argument
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 'abc',
 *  p2: 'cba',
 *  p3: Em.computed.concat('|', 'p1', 'p2')
 * });
 * console.log(o.get('p3')); // 'abc|cba'
 * o.set('p1', 'aaa');
 * console.log(o.get('p3')); // 'aaa|cba'
 * </pre>
 *
 * @param {string} separator
 * @param {...string} dependentKeys
 * @method concat
 * @return {Ember.ComputedProperty}
 */
computed.concat = generateComputedWithKey(function (separator, dependentValues) {
  return dependentValues.join(separator);
});

/**
 * A computed property that returns first not blank value from dependent values
 * Based on <code>Ember.isBlank</code>
 * Takes at least 1 argument
 * Dependent values order affects the result
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: null,
 *  p2: '',
 *  p3: 'abc'
 *  p4: Em.computed.firstNotBlank('p1', 'p2', 'p3')
 * });
 * console.log(o.get('p4')); // 'abc'
 * o.set('p1', 'aaa');
 * console.log(o.get('p4')); // 'aaa'
 * </pre>
 *
 * @param {...string} dependentKeys
 * @method firstNotBlank
 * @return {Ember.ComputedProperty}
 */
computed.firstNotBlank = generateComputedWithValues(function (values) {
  for (var i = 0; i < values.length; i++) {
    if (!Em.isBlank(values[i])) {
      return values[i];
    }
  }
  return null;
});

/**
 * A computed property that returns dependent value if it is truly or ('0'|0)
 * Returns <code>'n/a'</code> otherwise
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 0,
 *  p2: Em.computed.formatUnavailable('p1')
 * });
 * console.log(o.get('p2')); // 0
 * o.set('p1', 12);
 * console.log(o.get('p2')); // 12
 * o.set('p1', 'some string');
 * console.log(o.get('p2')); // 'n/a'
 * </pre>
 *
 * @param {string} dependentKey
 * @method formatUnavailable
 * @returns {Ember.ComputedProperty}
 */
computed.formatUnavailable = function(dependentKey) {
  return computed(dependentKey, function () {
    var value = smartGet(this, dependentKey);
    return value || value == 0 ? value : Em.I18n.t('services.service.summary.notAvailable');
  });
};

/**
 * A computed property that returns one of provided values basing on dependent value
 * If dependent value is 0, <code>zeroMsg</code> is returned
 * If dependent value is 1, <code>oneMsg</code> is returned
 * If dependent value is greater than 1, <code>manyMsg</code> is returned
 * App.*-keys are supported
 * <pre>
 * var o = Em.Object.create({
 *  p1: 0,
 *  p2: Em.computed.formatUnavailable('p1', '0msg', '1msg', '2+msg')
 * });
 * console.log(o.get('p2')); // '0msg'
 * o.set('p1', 1);
 * console.log(o.get('p2')); // '1msg'
 * o.set('p1', 100500);
 * console.log(o.get('p2')); // '2+msg'
 * </pre>
 *
 * @param {string} dependentKey
 * @param {string} zeroMsg
 * @param {string} oneMsg
 * @param {string} manyMsg
 * @returns {Ember.ComputedProperty}
 * @method countBasedMessage
 */
computed.countBasedMessage = function (dependentKey, zeroMsg, oneMsg, manyMsg) {
  return computed(dependentKey, function () {
    var value = Number(smartGet(this, dependentKey));
    if (value === 0) {
      return zeroMsg;
    }
    if (value > 1) {
      return manyMsg;
    }
    return oneMsg;
  });
};

/**
 * A computed property that returns property value according to the property key and object key
 * App.*-keys are supported
 * <pre>
 *   var o = Em.Object.create({
 *    p1: {a: 1, b: 2, c: 3},
 *    p2: 'a',
 *    p3: Em.computed.getByKey('p1', 'p2')
 *   });
 *   console.log(o.get('p3')); // 1
 *   o.set('p2', 'b');
 *   console.log(o.get('p3')); // 2
 *   o.set('p2', 'c');
 *   console.log(o.get('p3')); // 3
 * </pre>
 *
 * With `defaultValue`
 * <pre>
 *   var o = Em.Object.create({
 *    p1: {a: 1, b: 2, c: 3},
 *    p2: 'd',
 *    p3: Em.computed.getByKey('p1', 'p2', 100500)
 *   });
 *   console.log(o.get('p3')); // 100500 - default value is returned, because there is no key `d` in the `p1`
 * </pre>
 * <b>IMPORTANT!</b> This CP <b>SHOULD NOT</b> be used with for object with values equal to the views (like <code>{a: App.MyViewA, b: App.MyViewB}</code>)
 * This restriction exists because views may be undefined on the moment when this CP is calculated (files are not `required` yet)
 *
 * @param {string} objectKey
 * @param {string} propertyKey
 * @param {*} [defaultValue]
 * @returns {Ember.ComputedProperty}
 */
computed.getByKey = function (objectKey, propertyKey, defaultValue) {
  return computed(objectKey, propertyKey, function () {
    var object = smartGet(this, objectKey);
    var property = smartGet(this, propertyKey);
    if (!object) {
      return null;
    }
    return object.hasOwnProperty(property) ? object[property] : defaultValue;
  });
}

/**
 * A computed property that returns dependent value truncated to the `reduceTo`-size if its length is greater than `maxLength`
 * Truncated part may be replaced with `replacer` if it's provided ('...' by default)
 * <pre>
 *   var o = Em.Object.create({
 *     p1: Em.computed.truncate('p2', 8, 5, '###'),
 *     p2: 'some string',
 *     p3: Em.computed.truncate('p2', 8, 5)
 *   });
 *   console.log(o.get('p1')); // 'some ###'
 *   console.log(o.get('p3')); // 'some ...'
 *   o.set('p2', '123456789');
 *   console.log(o.get('p1')); // '12345###'
 *   console.log(o.get('p3')); // '12345...'
 * </pre>
 *
 * @param {string} dependentKey
 * @param {number} maxLength
 * @param {number} reduceTo
 * @param {string} [replacer] default - '...'
 * @returns {Ember.ComputedProperty}
 */
computed.truncate = function (dependentKey, maxLength, reduceTo, replacer) {
  Em.assert('`reduceTo` should be <=`maxLength`', reduceTo <= maxLength);
  var _replacer = arguments.length > 3 ? replacer : '...';
  return computed(dependentKey, function () {
    var value = smartGet(this, dependentKey) || '';
    if (value.length > maxLength) {
      return value.substr(0, reduceTo) + _replacer;
    }
    return value;
  });
}