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
 * Regexp for host with port ('hostName:1234')
 *
 * @type {string}
 */
var hostWithPort = "([\\w|\\.]*)(?=:)";

/**
 * Regexp for host with port and protocol ('://hostName:1234')
 *
 * @type {string}
 */
var hostWithPrefix = ":\/\/" + hostWithPort;

/**
 * Mixin describes host name computations initializers and handlers.
 *
 * @mixin App.HostsBasedInitializerMixin
 */
App.HostsBasedInitializerMixin = Em.Mixin.create({

  initializerTypes: [
    {name: 'host_with_component', method: '_initAsHostWithComponent'},
    {name: 'hosts_with_components', method: '_initAsHostsWithComponents'},
    {name: 'hosts_list_with_component', method: '_initAsHostsListWithComponent'}
  ],

  /**
   * Initializer for configs with value equal to hostName with needed component
   * Value example: 'hostName'
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @param {object} initializer
   * @returns {Object}
   * @private
   */
  _initAsHostWithComponent: function (configProperty, localDB, dependencies, initializer) {
    var component = localDB.masterComponentHosts.findProperty('component', initializer.component);
    if (!component) {
      return configProperty;
    }
    if (initializer.modifier) {
      var replaceWith = Em.getWithDefault(initializer.modifier, 'prefix', '')
          + component.hostName
        + Em.getWithDefault(initializer.modifier, 'suffix', '');
      this.setRecommendedValue(configProperty, initializer.modifier.regex, replaceWith);
    }
    else {
      Em.setProperties(configProperty, {
        recommendedValue: component.hostName,
        value: component.hostName
      });
    }

    return configProperty;
  },

  /**
   * Settings for <code>hosts_with_components</code>-initializer
   * Used for configs with value equal to the hosts list
   * May set value as array (if <code>asArray</code> is true) or as comma-sepratated string (if <code>asArray</code> is false)
   *
   * @see _initAsHostsWithComponents
   * @param {string|string[]} components
   * @param {boolean} [asArray=false]
   * @param {boolean|undefined} [isInstalled=undefined]
   * @returns {{type: string, components: string[], asArray: boolean}}
   */
  getComponentsHostsConfig: function(components, asArray, isInstalled) {
    if (1 === arguments.length) {
      asArray = false;
    }
    return {
      type: 'hosts_with_components',
      components: Em.makeArray(components),
      asArray: asArray,
      isInstalled: isInstalled
    };
  },

  /**
   * Initializer for configs with value equal to hostNames with needed components
   * May be array or comma-separated list
   * Depends on <code>initializer.asArray</code> (true - array, false - string)
   * Value example: 'hostName1,hostName2,hostName3' or ['hostName1', 'hostName2', 'hostName3']
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @param {object} initializer
   * @return {Object}
   * @private
   */
  _initAsHostsWithComponents: function (configProperty, localDB, dependencies, initializer) {
    var hostNames = localDB.masterComponentHosts.filter(function (masterComponent) {
      var hasFound = initializer.components.contains(masterComponent.component);
      if (Em.isNone(initializer.isInstalled)) {
        return hasFound;
      }
      return hasFound && masterComponent.isInstalled === initializer.isInstalled;
    }).sortProperty('hostName').mapProperty('hostName');
    if (!initializer.asArray) {
      hostNames = hostNames.uniq().join(',');
    }
    Em.setProperties(configProperty, {
      value: hostNames,
      recommendedValue: hostNames
    });
    return configProperty;
  },

  /**
   * Settings for <code>host_with_component</code>-initializer
   * Used for configs with value equal to hostName that has <code>component</code>
   * Value may be modified with if <code>withModifier</code> is true (it is by default)
   * <code>hostWithPort</code>-regexp will be used in this case
   *
   * @see _initAsHostWithComponent
   * @param {string} component
   * @param {boolean} [withModifier=true]
   * @return {object}
   */
  getSimpleComponentConfig: function(component, withModifier) {
    if (arguments.length === 1) {
      withModifier = true;
    }
    var config = {
      type: 'host_with_component',
      component: component
    };
    if (withModifier) {
      config.modifier = {
        type: 'regexp',
        regex: hostWithPort
      };
    }
    return config;
  },

  /**
   * Almost the same to <code>getSimpleComponentConfig</code>, but with possibility to modify <code>replaceWith</code>-value
   * <code>prefix</code> is added before it
   * <code>suffix</code> is added after it
   * <code>hostWithPrefix</code>-regexp is used
   *
   * @see _initAsHostWithComponent
   * @param {string} component
   * @param {string} [prefix]
   * @param {string} [suffix]
   * @returns {object}
   */
  getComponentConfigWithAffixes: function(component, prefix, suffix) {
    prefix = prefix || '';
    suffix = suffix || '';
    return {
      type: 'host_with_component',
      component: component,
      modifier: {
        type: 'regexp',
        regex: hostWithPrefix,
        prefix: prefix,
        suffix: suffix
      }
    };
  },

  /**
   * Settings for <code>host_with_port</code>-initializer
   * Used for configs with value equal to hostName where some component exists concatenated with port-value
   * Port-value is calculated according to <code>port</code> and <code>portFromDependencies</code> values
   * If <code>portFromDependencies</code> is <code>true</code>, <code>port</code>-value is used as key of the <code>dependencies</code> (where real port-value is)
   * Otherwise - <code>port</code>-value used as is
   * If calculated port-value is empty, it will be skipped (and ':' too)
   * Value also may be customized with prefix and suffix
   *
   * @param {string} component needed component
   * @param {boolean} componentExists component already exists or just going to be installed
   * @param {string} prefix=''
   * @param {string} suffix=''
   * @param {string} port
   * @param {boolean} [portFromDependencies=false]
   * @returns {{type: string, component: string, componentExists: boolean, modifier: {prefix: (string), suffix: (string)}}}
   * @method getHostWithPortConfig
   * @static
   */
  getHostWithPortConfig: function (component, componentExists, prefix, suffix, port, portFromDependencies) {
    if (arguments.length < 6) {
      portFromDependencies = false;
    }
    prefix = prefix || '';
    suffix = suffix || '';
    var ret = {
      type: 'host_with_port',
      component: component,
      componentExists: componentExists,
      modifier: {
        prefix: prefix,
        suffix: suffix
      }
    };
    if (portFromDependencies) {
      ret.portKey = port;
    }
    else {
      ret.port = port;
    }
    return ret;
  },

  /**
   * Initializer for configs with value equal to the hostName where some component exists
   * Value may be customized with prefix and suffix (see <code>initializer.modifier</code>)
   * Port-value is calculated according to <code>initializer.portKey</code> or <code>initializer.port</code> values
   * If calculated port-value is empty, it will be skipped (and ':' too)
   * Value-examples: 'SOME_COOL_PREFIXhost1:port1SOME_COOL_SUFFIX', 'host1:port2'
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {object} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initAsHostWithPort
   */
  _initAsHostWithPort: function (configProperty, localDB, dependencies, initializer) {
    var hostName = localDB.masterComponentHosts.filterProperty('component', initializer.component).findProperty('isInstalled', initializer.componentExists).hostName;
    var port = this.__getPort(dependencies, initializer);
    var value = initializer.modifier.prefix + hostName + (port ? ':' + port : '') + initializer.modifier.suffix;
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: value
    });
    return configProperty;
  },

  /**
   * Settings for <code>hosts_with_port</code>-initializer
   * Used for configs with value equal to the list of hostNames with port
   * Value also may be customized with prefix, suffix and delimiter between host:port elements
   * Port-value is calculated according to <code>port</code> and <code>portFromDependencies</code> values
   * If <code>portFromDependencies</code> is <code>true</code>, <code>port</code>-value is used as key of the <code>dependencies</code> (where real port-value is)
   * Otherwise - <code>port</code>-value used as is
   * If calculated port-value is empty, it will be skipped (and ':' too)
   *
   * @param {string|string[]} component hosts where this component(s) exists are used as config-value
   * @param {string} prefix='' substring added before hosts-list
   * @param {string} suffix='' substring added after hosts-list
   * @param {string} delimiter=',' delimiter between hosts in the value
   * @param {string} port if <code>portFromDependencies</code> is <code>false</code> this value is used as port for hosts
   * if <code>portFromDependencies</code> is <code>true</code> `port` is used as key in the <code>dependencies</code> to get real port-value
   * @param {boolean} portFromDependencies=false true - use <code>port</code> as key for <code>dependencies</code> to get real port-value,
   * false - use <code>port</code> as port-value
   * @returns {{type: string, component: string, modifier: {prefix: (string), suffix: (string), delimiter: (string)}}}
   * @method getHostsWithPortConfig
   * @static
   */
  getHostsWithPortConfig: function (component, prefix, suffix, delimiter, port, portFromDependencies) {
    if (arguments.length < 6) {
      portFromDependencies = false;
    }
    prefix = prefix || '';
    suffix = suffix || '';
    delimiter = delimiter || ',';
    var ret = {
      type: 'hosts_with_port',
      component: component,
      modifier: {
        prefix: prefix,
        suffix: suffix,
        delimiter: delimiter
      }
    };
    if (portFromDependencies) {
      ret.portKey = port;
    }
    else {
      ret.port = port;
    }
    return ret;
  },

  /**
   * Initializer for configs with value equal to the list of hosts where some component exists
   * Value may be customized with prefix and suffix (see <code>initializer.modifier</code>)
   * Delimiter between hostNames also may be customized in the <code>initializer.modifier</code>
   * Port-value is calculated according to <code>initializer.portKey</code> or <code>initializer.port</code> values
   * If calculated port-value is empty, it will be skipped (and ':' too)
   * Value examples: 'SOME_COOL_PREFIXhost1:port,host2:port,host2:portSOME_COOL_SUFFIX', 'host1:port|||host2:port|||host2:port'
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initAsHostsWithPort
   */
  _initAsHostsWithPort: function (configProperty, localDB, dependencies, initializer) {
    var hostNames, hosts;
    if (Em.isArray(initializer.component)) {
      hosts = localDB.masterComponentHosts.filter(function(masterComponent) {
        return initializer.component.contains(masterComponent.component);
      }).sortProperty('hostName');
    } else {
      hosts = localDB.masterComponentHosts.filterProperty('component', initializer.component);
    }
    if (Em.isNone(initializer.componentExists)) {
      hostNames = hosts.mapProperty('hostName');
    } else {
      hostNames = hosts.filterProperty('isInstalled', initializer.componentExists).mapProperty('hostName');
    }
    var port = this.__getPort(dependencies, initializer);
    var value = initializer.modifier.prefix + hostNames.uniq().map(function (hostName) {
        return hostName + (port ? ':' + port : '');
      }).join(initializer.modifier.delimiter) + initializer.modifier.suffix;
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: value
    });
    return configProperty;
  },

  /**
   * Returns port-value from <code>dependencies</code> accorfing to <code>initializer.portKey</code> or <code>initializer.port</code> values
   *
   * @param {object} dependencies
   * @param {object} initializer
   * @returns {string|number}
   * @private
   * @method __getPort
   */
  __getPort: function (dependencies, initializer) {
    var portKey = initializer.portKey;
    if (portKey) {
      return dependencies[portKey];
    }
    return initializer.port;
  },

  /**
   *
   * @param {string} component component name
   * @param {boolean} componentExists
   * @returns {object}
   */
  getHostsListComponentConfig: function(component, componentExists, delimiter) {
    return {
      type: 'hosts_list_with_component',
      component: component,
      componentExists: componentExists,
      modifier: {
        delimiter: Em.isNone(delimiter) ? ',' : delimiter
      }
    };
  },

  /**
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @param {object} initializer
   * @returns {configProperty}
   */
  _initAsHostsListWithComponent: function(configProperty, localDB, dependencies, initializer) {
    var hostNames = localDB.masterComponentHosts
        .filterProperty('component', initializer.component)
        .filterProperty('isInstalled', initializer.componentExists)
        .mapProperty('hostName')
        .join(initializer.modifier.delimiter);

    Em.setProperties(configProperty, {
      value: hostNames,
      recommendedValue: hostNames
    });
    return configProperty;
  }
});
