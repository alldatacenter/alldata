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

/**
 *
 *
 * @class App.TestAliases
 */
App.TestAliases = {
  helpers: {

    /**
     * Get needed value (basing on <code>key</code>) from <code>self</code> or <code>App</code>
     *
     * @param {Ember.Object} self
     * @param {string} key
     * @returns {*}
     */
    smartGet: function (self, key) {
      var isApp = key.startsWith('App.');
      var name = isApp ? key.replace('App.', '') : key;
      return isApp ? App.get(name) : self.get(name);
    },

    /**
     * Stub <code>get</code> for <code>App</code> or <code>self</code>
     *
     * @returns {App.TestAliases}
     */
    smartStubGet: function () {
      var args = [].slice.call(arguments);
      if (args.length === 3) {
        return this._stubOneKey.apply(this, args);
      }
      return this._stubManyKeys.apply(this, args)
    },

    /**
     * Trigger recalculation of the needed property in the <code>self</code>
     * or in the <code>App</code> (depends on <code>propertyName</code>)
     *
     * @param {Ember.Object} self
     * @param {string} propertyName
     * @returns {App.TestAliases}
     */
    propertyDidChange: function (self, propertyName) {
      var isApp = propertyName.startsWith('App.');
      var name = isApp ? propertyName.replace('App.', '') : propertyName;
      var context = isApp ? App : self;
      Em.propertyDidChange(context, name);
      return this;
    },

    /**
     * Try to restore (@see sinon.restore) <code>get</code> for <code>App</code> and <code>context</code>
     *
     * @param {Ember.Object} context
     * @returns {App.TestAliases}
     */
    smartRestoreGet: function(context) {
      Em.tryInvoke(context.get, 'restore');
      Em.tryInvoke(App.get, 'restore');
      return this;
    },

    /**
     * Stub <code>get</code>-method for <code>App</code> or <code>self</code> (depends on <code>dependentKey</code>)
     * to return <code>value</code> if <code>dependentKey</code> is get
     *
     * @param {Ember.Object} self
     * @param {string} dependentKey
     * @param {*} value
     * @returns {App.TestAliases}
     * @private
     */
    _stubOneKey: function (self, dependentKey, value) {
      var isApp = dependentKey.startsWith('App.');
      var name = isApp ? dependentKey.replace('App.', '') : dependentKey;
      var context = isApp ? App : self;
      sinon.stub(context, 'get', function (k) {
        return k === name ? value : Em.get(context, k);
      });
      return this;
    },

    /**
     * Stub <code>get</code>-method for <code>App</code> or <code>self</code> (depends on </code>hash</code>-keys)
     * If some key is starts with 'App.' it will be used in the App-stub,
     * otherwise it will be used in thw self-stub
     *
     * @param {Ember.Object} self
     * @param {object} hash
     * @returns {App.TestAliases}
     * @private
     */
    _stubManyKeys: function (self, hash) {
      var hashForApp = {}; // used in the App-stub
      var hashForSelf = {}; // used in the self-stub
      Object.keys(hash).forEach(function(key) {
        var isApp = key.startsWith('App.');
        var name = isApp ? key.replace('App.', '') : key;
        if(isApp) {
          hashForApp[name] = hash[key];
        }
        else {
          hashForSelf[name] = hash[key];
        }
      });
      sinon.stub(App, 'get', function (k) {
        if (hashForApp.hasOwnProperty(k)) {
          return hashForApp[k];
        }
        return Em.get(App, k);
      });
      sinon.stub(self, 'get', function (k) {
        if (hashForSelf.hasOwnProperty(k)) {
          return hashForSelf[k];
        }
        return Em.get(self, k);
      });
      return this;
    },

    /**
     * Generates array of all possible boolean combinations
     * Example:
     * <code>
     *   var keys = ['a', 'b'];
     *   var result = getBinaryCombos(keys);
     *   console.log(result); // [{a: true, b: true}, {a: true, b: false}, {a: false, b: true}, {a: false, b: false}]
     * </code>
     *
     * @param {string[]} dependentKeys
     * @returns {Array}
     */
    getBinaryCombos: function (dependentKeys) {
      var n = dependentKeys.length;
      var result = [];
      var allCombos = Math.pow(2, n);
      for (var y = 0; y < allCombos; y++) {
        var combo = {};
        for (var x = 0; x < n; x++) {
          combo[dependentKeys[x]] = !!(y >> x & 1);
        }
        result.push(combo);
      }
      return result;
    },

    /**
     * Reopens property of the given object as constant with the given value
     * @param {Ember.Object} context
     * @param {String} key
     * @param {*} value
     */
    reopenProperty: function (context, key, value) {
      var reopenObject = {},
        isUndefined = typeof value === 'undefined';
      // if the only property in reopen argument is undefined, context won't be changed
      reopenObject[key] = isUndefined ? null : value;
      context.reopen(reopenObject);
      if (isUndefined) {
        context.set(key, undefined);
      }
    }

  }
};

require('test/aliases/computed/equal');
require('test/aliases/computed/notEqual');
require('test/aliases/computed/equalProperties');
require('test/aliases/computed/notEqualProperties');
require('test/aliases/computed/ifThenElse');
require('test/aliases/computed/ifThenElseByKeys');
require('test/aliases/computed/sumProperties');
require('test/aliases/computed/countBasedMessage');
require('test/aliases/computed/firstNotBlank');
require('test/aliases/computed/percents');
require('test/aliases/computed/existsIn');
require('test/aliases/computed/existsInByKey');
require('test/aliases/computed/notExistsIn');
require('test/aliases/computed/notExistsInByKey');
require('test/aliases/computed/alias');
require('test/aliases/computed/gte');
require('test/aliases/computed/gt');
require('test/aliases/computed/gteProperties');
require('test/aliases/computed/gtProperties');
require('test/aliases/computed/lte');
require('test/aliases/computed/lt');
require('test/aliases/computed/lteProperties');
require('test/aliases/computed/ltProperties');
require('test/aliases/computed/someBy');
require('test/aliases/computed/someByKey');
require('test/aliases/computed/everyBy');
require('test/aliases/computed/everyByKey');
require('test/aliases/computed/mapBy');
require('test/aliases/computed/filterBy');
require('test/aliases/computed/filterByKey');
require('test/aliases/computed/findBy');
require('test/aliases/computed/findByKey');
require('test/aliases/computed/sumBy');
require('test/aliases/computed/and');
require('test/aliases/computed/or');
require('test/aliases/computed/formatUnavailable');
require('test/aliases/computed/getByKey');
require('test/aliases/computed/truncate');
require('test/aliases/computed/concat');
require('test/aliases/computed/empty');