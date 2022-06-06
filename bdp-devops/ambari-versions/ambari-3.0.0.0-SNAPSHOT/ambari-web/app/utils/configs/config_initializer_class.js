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

/**
 * @typedef {object} initializer
 * @property {string} type initializer type name
 * @property {boolean} [isChecker] determines control flow callback
 */

/**
 * @typedef {object} initializerType
 * @property {string} name key
 * @property {string} method function's name (prefer to start method-name with '_init' or '_initAs'). Each method here is called with arguments equal to <code>initialValue</code>-call args. Initializer-settings are added as last argument
 */

/**
 * Minimal fields-list that should be in the config-object
 * There is no way to set value without any of them
 *
 * @typedef {object} configProperty
 * @property {string} name config's name
 * @property {number|string} value current value
 * @property {string} filename file name where this config is
 * @property {number|string} [recommendedValue] value which is recommended
 */

/**
 * Basic class for config-initializers
 * Each child should fill <code>initializers</code> or <code>uniqueInitializers</code> and <code>initializerTypes</code>
 * Usage:
 * <pre>
 * var myCoolInitializer = App.ConfigInitializerClass.create({
 *  initializers: {
 *    'my-cool-config': {
 *      type: 'some_type'
 *    }
 *  },
 *
 *  initializerTypes: {
 *    some_type: {
 *      method: '_initAsCool'
 *    }
 *  },
 *
 *  _initAsCool: function (configProperty, localDB, dependencies, initializer) {
 *    // some magic
 *    return configProperty;
 *  }
 * });
 *
 * var myConfig = { name: 'my-cool-config' };
 * var localDB = getLocalDB();
 * var dependencies = {};
 * myCoolInitializer.initialValue(myConfig, localDB, dependencies);
 * </pre>
 * <code>dependencies</code> - it's an object with almost any information that might be needed to set config's value. It
 * shouldn't contain App.*-flags like 'isHaEnabled' or 'isHadoop2Stack'. They might be accessed directly from App. But something
 * bigger data should be pulled to the <code>dependencies</code>.
 * Information about cluster's hosts, topology of master components should be in the <code>localDB</code>
 *
 * @type {ConfigInitializerClass}
 * @augments {Em.Object}
 */
App.ConfigInitializerClass = Em.Object.extend({

  _initializerFlowCode: {
    next: 0,
    skipNext: 1,
    skipAll: 2
  },

  concatenatedProperties: ['initializerTypes'],

  /**
   * Map with configurations for config initializers
   * It's used only for initializers which are common for some configs (if not - use <code>uniqueInitializers</code>-map)
   * Key {string} configProperty-name
   * Value {initializer|initializer[]} settings for initializer
   *
   * @type {object}
   */
  initializers: {},

  /**
   * Map with initializers types
   * It's not overridden in the child-classes (@see Ember's concatenatedProperties)
   *
   * @type {initializerType[]}
   */
  initializerTypes: [],

  /**
   * Map with initializers that are used only for one config (are unique)
   * Key: configProperty-name
   * Value: method-name
   * Every method from this map is called with same arguments as <code>initialValue</code> is (prefer to start method-name with '_init' or '_initAs')
   *
   * @type {object}
   */
  uniqueInitializers: {},

  /**
   * Wrapper for common initializers
   * Execute initializer if it is a function or throw an error otherwise
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @returns {Object}
   * @private
   */
  _defaultInitializer: function (configProperty, localDB, dependencies) {
    var args = [].slice.call(arguments);
    var self = this;
    var initializers = this.get('initializers');
    var initializerTypes = this.get('initializerTypes');
    var initializer = initializers[Em.get(configProperty, 'name')];
    if (initializer) {
      initializer = Em.makeArray(initializer);
      var i = 0;
      while(i < initializer.length) {
        var init = initializer[i];
        var _args = [].slice.call(args);
        var type = initializerTypes.findProperty('name', init.type);
        // add initializer-settings
        _args.push(init);
        var methodName = type.method;
        Em.assert('method-initializer is not a function ' + methodName, 'function' === Em.typeOf(self[methodName]));
        if (init.isChecker) {
          var result = self[methodName].apply(self, _args);
          if (result === this.flowSkipNext()) {
            i++; // skip next
          }
          else {
            if (result === this.flowSkipAll()) {
              break;
            }
          }
        }
        else {
          configProperty = self[methodName].apply(self, _args);
        }
        i++;
      }
    }
    return configProperty;
  },

  /**
   * Entry-point for any config's value initializing
   * Before calling it, be sure that <code>initializers</code> or <code>uniqueInitializers</code>
   * contains record about needed configs
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @returns {Object}
   * @method initialValue
   */
  initialValue: function (configProperty, localDB, dependencies) {
    var configName = Em.get(configProperty, 'name');
    var initializers = this.get('initializers');
    var initializer = initializers[configName];
    if (initializer) {
      return this._defaultInitializer(configProperty, localDB, dependencies);
    }

    var uniqueInitializers = this.get('uniqueInitializers');
    var uniqueInitializer = uniqueInitializers[configName];
    if (uniqueInitializer) {
      var args = [].slice.call(arguments);
      return this[uniqueInitializer].apply(this, args);
    }
    Em.set(configProperty, 'initialValue', Em.get(configProperty, 'value'));
    return configProperty;
  },

  /**
   * Should do some preparing for initializing-process
   * Shouldn't be redefined without needs
   * Should be used before any <code>initialValue</code>-calls (if needed)
   *
   * @method setup
   */
  setup: Em.K,

  /**
   * Should restore Initializer's to the initial state
   * Basically, should revert changes done in the <code>setup</code>
   *
   * @method cleanup
   */
  cleanup: Em.K,

  /**
   * Setup <code>initializers</code>
   * There are some configs with names based on other config's values.
   * Example: config with name <code>hadoop.proxyuser.{{oozieUser}}.hosts</code>
   * Here <code>{{oozieUser}}</code> is value of the config <code>['oozie-env']['oozie_user']</code>.
   * So, when <code>initializers</code> are manually set up in the Initializer-instance, there is no way to populate initializer
   * for such configs. Reason: each initializer's key is a config-name which is compared strictly to the provided config-name.
   * Solution: use config-names with placeholders. Example: <code>hadoop.proxyuser.${oozieUser}.hosts</code>
   * In this case, before calling <code>initialValue</code> this key should be updated with real value. And it should be done
   * in the common way for the all such configs.
   * Code example:
   * <pre>
   *   var mySettings = {
   *    oozieUser: 'someDude'
   *   };
   *
   *   var myCoolInitializer = App.MoveComponentConfigInitializerClass.create({
   *    initializers: {
   *      'hadoop.proxyuser.{{oozieUser}}.hosts': {
   *        // some setting are here
   *      }
   *    },
   *
   *    setup: function (settings) {
   *      this._updateInitializers(settings);
   *    },
   *
   *    cleanup: function () {
   *      this._restoreInitializers();
   *    }
   *
   *   });
   *
   *   myCoolInitializer.setup(mySettings); // after this call `myCoolInitializer.initializers` will contain two keys
   *   console.log(myCoolInitializer.initializers); // {'hadoop.proxyuser.{{oozieUser}}.hosts': {}, 'hadoop.proxyuser.someDude.hosts': {}}
   *   // it's possible to call `initialValue` now
   *   myCoolInitializer.initialValue({name: 'hadoop.proxyuser.someDude.hosts', value: ''}, {}, {});
   *   myCoolInitializer.cleanup(); // after updating values for the all configs
   * </pre>
   * Additional key in the <code>initializers</code> won't cause any issues.
   * Keep in mind replacing-rules:
   * <ul>
   *  <li>Each field in the <code>settings</code> should be a string or number</li>
   *  <li>Substring that will be replaced should be wrapped with '{{', '}}'</li>
   *  <li>Each field is searched in the each initializers-key, so try to avoid names-collision</li>
   *  <li>Value for 'new' key is the same as for 'old' ('hadoop.proxyuser.{{oozieUser}}.hosts' and 'hadoop.proxyuser.someDude.hosts' will have same initializer)</li>
   * </ul>
   * <b>Important! Be sure, that you call <code>_restoreInitializers</code> before calling <code>_updateInitializers</code> second time</b>
   *
   * @param {object} settings
   * @method _updateInitializers
   */
  _updateInitializers: function (settings) {
    settings = settings || {};
    var originalInitializers = this.get('initializers');
    var copyInitializers = Em.copy(originalInitializers, true);
    this.set('__copyInitializers', copyInitializers);
    var initializers = this._updateNames('initializers', settings);
    this._setForComputed('initializers', initializers);

    var originalUniqueInitializers = this.get('uniqueInitializers');
    var copyUniqueInitializers = Em.copy(originalUniqueInitializers, true);
    this.set('__copyUniqueInitializers', copyUniqueInitializers);
    var uniqueInitializers = this._updateNames('uniqueInitializers', settings);
    this._setForComputed('uniqueInitializers', uniqueInitializers);
  },

  /**
   * Revert names changes done in the <code>_updateInitializers</code>
   *
   * @method _restoreInitializers
   */
  _restoreInitializers: function() {
    var copyInitializers = this.get('__copyInitializers');
    var copyUniqueInitializers = this.get('__copyUniqueInitializers');
    if ('object' === Em.typeOf(copyInitializers)) {
      this._setForComputed('initializers', Em.copy(copyInitializers, true));
    }
    if ('object' === Em.typeOf(copyUniqueInitializers)) {
      this._setForComputed('uniqueInitializers', Em.copy(copyUniqueInitializers, true));
    }
  },

  /**
   * Replace list of <code>settings</code> in the sourceKey
   * Example:
   * <pre>
   *   var sourceKey = '{{A}},{{B}},{{C}}';
   *   var settings = {A: 'a', B: 'b', C: 'c'};
   *   sourceKey = _updateNames(sourceKey, settings);
   *   console.log(sourceKey); // 'a,b,c'
   * </pre>
   *
   * @param {string} sourceKey
   * @param {object} settings
   * @returns {object}
   * @private
   * @method _updateNames
   */
  _updateNames: function (sourceKey, settings) {
    settings = settings || {};
    var source = this.get(sourceKey);
    Object.keys(source).forEach(function (configName) {
      var initializer = source[configName];
      Object.keys(settings).forEach(function (key) {
        var replaceWith = settings[key];
        var toReplace = '{{' + key + '}}';
        configName = configName.replace(toReplace, replaceWith);
      });
      source[configName] = initializer;
    });
    return source;
  },

  flowNext: function() {
    return this.get('_initializerFlowCode.next');
  },

  flowSkipNext: function() {
    return this.get('_initializerFlowCode.skipNext');
  },

  flowSkipAll: function() {
    return this.get('_initializerFlowCode.skipAll');
  },

  /**
   * Set value for computed property using `reopen`. Currently used to update 'initializers'
   * and 'uniqueInitializers'.
   * Used to set value for props like:
   * <code>cp: function() { }.property()</code>
   * <code>
   * var obj = App.ConfigInitializerClass.create({
   *   cp: function() {
   *   		return {
   *     		key: "value"
   *   		}
   *   }.property(),
   *   setProp: function() {
   *   		this.set('cp', {newKey: "new_value"}); // will not change `cp` value
   *   },
   *   updateProp: function() {
   *   		this._setForComputed('cp', { newKey: "new_value"}); // will update
   *   }
   * });
   *
   * obj.get('cp'); // {key: "value"}
   * obj.setProp();
   * obj.get('cp'); // {key: "value"}
   * obj.updateProp();
   * obj.get('cp'); // {newKey: "new_value"}
   * </code>
   * @private
   * @param  {string} key
   * @param  {*} value
   */
  _setForComputed: function(key, value) {
    var obj = {};
    obj[key] = function() {
      return value;
    }.property();
    this.reopen(obj);
  }
});
