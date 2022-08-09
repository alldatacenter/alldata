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
require('utils/configs/config_initializer_class');
require('utils/configs/ha_config_initializer_class');
require('utils/configs/hosts_based_initializer_mixin');
require('utils/configs/control_flow_initializer_mixin');

var _slice = Array.prototype.slice;

/**
 * Main class responsible for properties computation.
 * This class contains all required info to manipulate properties regarding value updates
 * during removing/adding components.
 * To determine when component removed or added you just need to setup properly localDB object
 * and set `isInstalled` flag to `true` where selected component(s) will be located after adding/removing.
 * By default all initializer handlers filtering localDB by `isInstalled` `true`.
 *
 * @mixes App.ControlFlowInitializerMixin
 * @mixes App.HostsBasedInitializerMixin
 * @type {AddComponentConfigInitializer}
 * @augments {HaConfigInitializerClass}
 */
App.AddComponentConfigInitializer = App.HaConfigInitializerClass.extend(App.HostsBasedInitializerMixin, App.ControlFlowInitializerMixin, {
  /**
   * All initializer properties definition.
   * Object format is the same as for App.ConfigInitializerClass.initializers
   * @see App.ConfigInitializerClass.initializers
   *
   * @return {object} property name - initializer map
   */
  __defaultInitializers: function() {
    return {
      'zookeeper.connect': this.getHDPStackOnlyHostsPortConfig('2.2', 'ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true),
      'ha.zookeeper.quorum': this.getNameNodeHAOnlyHostsPortConfig('ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true),
      'hbase.zookeeper.quorum': this.getHostsListComponentConfig('ZOOKEEPER_SERVER', true),
      'instance.zookeeper.host': this.getHostsWithPortConfig('ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true),
      'templeton.zookeeper.hosts': this.getHostsWithPortConfig('ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true),
      'hive.cluster.delegation.token.store.zookeeper.connectString': this.getHostsWithPortConfig('ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true),
      'storm.zookeeper.servers': this.getHostsListComponentJSONStringifiedConfig('ZOOKEEPER_SERVER', true),
      'hive.zookeeper.quorum': this.getHDPStackOnlyHostsPortConfig('2.2', 'ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true),
      'hadoop.registry.zk.quorum': this.getHDPStackOnlyHostsPortConfig('2.2', 'ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true),
      'nimbus.seeds': this.getHostsListComponentJSONStringifiedConfig('NIMBUS', true),
      'hadoop.proxyuser.{{webhcatUser}}.hosts': this.getComponentsHostsConfig(['HIVE_SERVER', 'WEBHCAT_SERVER', 'HIVE_METASTORE'], false, true),
      'hadoop.proxyuser.{{hiveUser}}.hosts': this.getComponentsHostsConfig(['HIVE_SERVER', 'WEBHCAT_SERVER', 'HIVE_METASTORE', 'HIVE_SERVER_INTERACTIVE'], false, true),
      'hive.metastore.uris': this.getHostsWithPortConfig(['HIVE_METASTORE'], 'thrift://', '', ',thrift://', 'hiveMetastorePort', true),
      'atlas.audit.hbase.zookeeper.quorum': this.getHostsListComponentConfig('ZOOKEEPER_SERVER', true),
      'atlas.graph.storage.hostname': this.getHostsListComponentConfig('ZOOKEEPER_SERVER', true),
      'atlas.kafka.zookeeper.connect': this.getHostsWithPortConfig('ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true)
    };
  },

  /**
   * All unique initializer definition.
   * Object format is the same as for App.ConfigInitializerClass.uniqueInitializers
   * @see App.ConfigInitializerClass.uniqueInitializers
   *
   * @type {Object}
   */
  __defaultUniqueInitializers: {
    'yarn.resourcemanager.zk-address': '_initYarnRMZkAdress',
    'templeton.hive.properties': '_initTempletonHiveProperties',
    'atlas.graph.index.search.solr.zookeeper-url': '_initAtlasGraphIndexSearchSolrZkUrl'
  },

  /**
   * Property names to initialize. This attribute should be overrided in class instance.
   * `initializers` property will set up according this list from `__defaultUniqueInitializers` and
   * `__defaultInitializers`
   *
   * @type {string[]}
   */
  initializeForProperties: null,

  initializers: function() {
    return {};
  }.property(),

  uniqueInitializers: {},

  init: function() {
    this._super();
    this._bootstrapInitializers(this.get('initializeForProperties'));
  },

  initializerTypes: [
    {
      name: 'json_stringified_value',
      method: '_initAsJSONStrigifiedValueConfig'
    }
  ],

  /**
   * @override
   * @param {object} settings
   */
  setup: function (settings) {
    this._updateInitializers(settings);
  },

  /**
   * @override
   */
  cleanup: function () {
    this._restoreInitializers();
  },

  getJSONStringifiedValueConfig: function() {
    return {
      type: 'json_stringified_value'
    };
  },

  _initAsJSONStrigifiedValueConfig: function(configProperty, localDB, dependencies, initializer) {
    var hostsValue = Em.get(configProperty, 'value').split(Em.getWithDefault(initializer, 'modifier.delimiter', ','));
    var propertyValue = JSON.stringify(hostsValue).replace(/"/g, "'");
    Em.setProperties(configProperty, {
      value: propertyValue,
      recommendedValue: propertyValue
    });
    return configProperty;
  },

  /**
   * Perform value update according to hosts. Mutate <code>siteConfigs</code>
   *
   * @param {object} siteConfigs
   * @param {configProperty} configProperty
   * @returns {boolean}
   */
  updateSiteObj: function(siteConfigs, configProperty) {
    if (!siteConfigs || !configProperty) return false;
    var initializer = this.get('initializers')[configProperty.name],
      isArray = !!(initializer && (initializer.type === 'json_stringified_value'
        || Em.isArray(initializer) && initializer.someProperty('type', 'json_stringified_value')));
    App.config.updateHostsListValue(siteConfigs, configProperty.fileName, configProperty.name, configProperty.value, isArray);
    return true;
  },

  /**
   * @see App.ControlFlowInitializerMixin.getNameNodeHAControl
   * @see App.HostsBasedInitializerMixin.getComponentsHostsConfig
   */
  getNameNodeHAOnlyHostsConfig: function(components, asArray) {
    return [
      this.getNameNodeHAControl(),
      this.getComponentsHostsConfig.apply(this, _slice.call(arguments))
    ];
  },

  /**
   * @override
   **/
  getHostsWithPortConfig: function (component, prefix, suffix, delimiter, port, portFromDependencies) {
    var ret = this._super.apply(this, _slice.call(arguments));
    ret.componentExists = true;
    return ret;
  },

  /**
   * @see App.ControlFlowInitializerMixin.getNameNodeHAControl
   * @see App.HostsBasedInitializerMixin.getHostsWithPortConfig
   */
  getNameNodeHAOnlyHostsPortConfig: function(component, prefix, suffix, delimiter, port, portFromDependencies) {
    return [
      this.getNameNodeHAControl(),
      this.getHostsWithPortConfig.apply(this, _slice.call(arguments))
    ];
  },

  /**
   * @see App.ControlFlowInitializerMixin.getResourceManagerHAControl
   * @see App.HostsBasedInitializerMixin.getHostsWithPortConfig
   */
  getResourceManagerHAOnlyHostsPortConfig: function(component, prefix, suffix, delimiter, port, portFromDependencies) {
    return [
      this.getResourceManagerHAControl(),
      this.getHostsWithPortConfig.apply(this, _slice.call(arguments))
    ];
  },

  /**
   * @see App.HostsBasedInitializerMixin.getHostsListComponentConfig
   * @see getJSONStringifiedValueConfig
   */
  getHostsListComponentJSONStringifiedConfig: function(component, componentExists, delimiter) {
    return [
      this.getHostsListComponentConfig.apply(this, _slice.call(arguments)),
      this.getJSONStringifiedValueConfig()
    ];
  },

  /**
   * @see App.ControlFlowInitializerMixin.getHDPStackVersionControl
   * @see App.HostsBasedInitializerMixin.getHostsWithPortConfig
   */
  getHDPStackOnlyHostsPortConfig: function(minStackVersion, component, prefix, suffix, delimiter, port, portFromDependencies) {
    return [
      this.getHDPStackVersionControl(minStackVersion),
      this.getHostsWithPortConfig.apply(this, _slice.call(arguments, 1))
    ];
  },

  _initYarnRMZkAdress: function (configProperty, localDB, dependencies) {
    return this._initAsHostsWithPort(configProperty, localDB, dependencies, {
      component: 'ZOOKEEPER_SERVER',
      componentExists: true,
      modifier: {
        prefix: '',
        suffix: '',
        delimiter: ','
      },
      portKey: 'zkClientPort'
    });
  },

  _initTempletonHiveProperties: function(configProperty, localDB, dependecies, initializer) {
    var hostNames = localDB.masterComponentHosts.filter(function(masterComponent) {
      return ['HIVE_METASTORE'].contains(masterComponent.component) && masterComponent.isInstalled === true;
    }).mapProperty('hostName').uniq().sort();
    var hiveMSHosts = hostNames.map(function(hostName) {
      return "thrift://" + hostName + ":" + dependecies.hiveMetastorePort;
    }).join('\\,');
    var value = configProperty.value.replace(/thrift.+[0-9]{2,},/i, hiveMSHosts + ",");
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: value
    });
    return configProperty;
  },

  _initAtlasGraphIndexSearchSolrZkUrl: function (configProperty, localDB, dependencies, initializer) {
    var solr = dependencies.infraSolrZnode;
    return this._initAsHostsWithPort(configProperty, localDB, dependencies, {
      component: 'ZOOKEEPER_SERVER',
      componentExists: true,
      modifier: {
        prefix: '',
        suffix: solr,
        delimiter: solr + ','
      },
      portKey: 'zkClientPort'
    });
  },

  /**
   * Set up `this.initializers` and `this.uniqueInitializers` properties according
   * to property list names.
   *
   * @param  {string[]} properties list of property names
   */
  _bootstrapInitializers: function(properties) {
    var initializers = {},
     uniqueInitializers = {},
     defaultInitializers = this.__defaultInitializers(),
     defaultUniqueInitializers = this.get('__defaultUniqueInitializers');

    if (Em.isNone(properties)) {
      initializers = this.__defaultInitializers();
      uniqueInitializer = this.get('__defaultUniqueInitializers');
    } else {
      properties.forEach(function(propertyName) {
        if (defaultInitializers[propertyName]) {
          initializers[propertyName] = defaultInitializers[propertyName];
        } else if (defaultUniqueInitializers[propertyName]) {
          uniqueInitializers[propertyName] = defaultUniqueInitializers[propertyName];
        }
      });
    }
    this._setForComputed('initializers', initializers);
    this.set('uniqueInitializers', uniqueInitializers);
  }
});

/**
 * ZooKeeper service add/remove components initializer.
 * @instance App.AddComponentConfigInitializer
 */
App.AddZooKeeperComponentsInitializer = App.AddComponentConfigInitializer.create({
  initializeForProperties: [
    'zookeeper.connect',
    'ha.zookeeper.quorum',
    'hbase.zookeeper.quorum',
    'instance.zookeeper.host',
    'templeton.zookeeper.hosts',
    'hive.cluster.delegation.token.store.zookeeper.connectString',
    'yarn.resourcemanager.zk-address',
    'hive.zookeeper.quorum',
    'storm.zookeeper.servers',
    'hadoop.registry.zk.quorum',
    'atlas.audit.hbase.zookeeper.quorum',
    'atlas.graph.index.search.solr.zookeeper-url',
    'atlas.graph.storage.hostname',
    'atlas.kafka.zookeeper.connect'
  ]
});

/**
 * Hive service add/remove components initializer.
 * @instance App.AddComponentConfigInitializer
 */
App.AddHiveComponentsInitializer = App.AddComponentConfigInitializer.create({
  initializeForProperties: [
    'hive.metastore.uris',
    'templeton.hive.properties',
    'hadoop.proxyuser.{{hiveUser}}.hosts'
  ]
});

/**
 * WebHCat service add/remove components initializer.
 * @instance App.AddComponentConfigInitializer
 */
App.AddWebHCatComponentsInitializer = App.AddComponentConfigInitializer.create({
  initializeForProperties: [
    'hadoop.proxyuser.{{webhcatUser}}.hosts'
  ]
});

/**
 * Hive Server Interactive component add initializer.
 * @instance App.AddHiveServerInteractiveInitializer
 */
App.AddHiveServerInteractiveInitializer = App.AddComponentConfigInitializer.create({
  initializeForProperties: [
    'hadoop.proxyuser.{{hiveUser}}.hosts'
  ]
});
