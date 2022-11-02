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
var stringUtils = require('utils/string_utils');

require('utils/configs/config_initializer_class');
require('utils/configs/hosts_based_initializer_mixin');

/**
 * Zookeeper-based configs don't have any customization settings
 *
 * @see _initAsZookeeperServersList
 * @returns {{type: string}}
 */
function getZKBasedConfig() {
  return {
    type: 'zookeeper_based'
  };
}

/**
 * Initializer for configs
 * Used on the cluster install
 *
 * Usage:
 * <pre>
 *   var configProperty = Object.create({});
 *   var localDB = {
 *    hosts: [],
 *    masterComponentHosts: [],
 *    slaveComponentHosts: []
 *   };
 *   var dependencies = {};
 *   App.ConfigInitializer.initialValue(configProperty, localDB, dependencies);
 * </pre>
 *
 * @instance ConfigInitializer
 */
App.ConfigInitializer = App.ConfigInitializerClass.create(App.HostsBasedInitializerMixin, {

  initializers: function() {
    return {
      'dfs.namenode.rpc-address': this.getSimpleComponentConfig('NAMENODE'),
      'dfs.http.address': this.getSimpleComponentConfig('NAMENODE'),
      'dfs.namenode.http-address': this.getSimpleComponentConfig('NAMENODE'),
      'dfs.https.address': this.getSimpleComponentConfig('NAMENODE'),
      'dfs.namenode.https-address': this.getSimpleComponentConfig('NAMENODE'),
      'dfs.secondary.http.address': this.getSimpleComponentConfig('SECONDARY_NAMENODE'),
      'dfs.namenode.secondary.http-address': this.getSimpleComponentConfig('SECONDARY_NAMENODE'),
      'yarn.resourcemanager.hostname': this.getSimpleComponentConfig('RESOURCEMANAGER', false),
      'yarn.resourcemanager.resource-tracker.address': this.getSimpleComponentConfig('RESOURCEMANAGER'),
      'yarn.resourcemanager.webapp.https.address': this.getSimpleComponentConfig('RESOURCEMANAGER'),
      'yarn.resourcemanager.webapp.address': this.getSimpleComponentConfig('RESOURCEMANAGER'),
      'yarn.resourcemanager.scheduler.address': this.getSimpleComponentConfig('RESOURCEMANAGER'),
      'yarn.resourcemanager.address': this.getSimpleComponentConfig('RESOURCEMANAGER'),
      'yarn.resourcemanager.admin.address': this.getSimpleComponentConfig('RESOURCEMANAGER'),
      'yarn.timeline-service.webapp.address': this.getSimpleComponentConfig('APP_TIMELINE_SERVER'),
      'yarn.timeline-service.webapp.https.address': this.getSimpleComponentConfig('APP_TIMELINE_SERVER'),
      'yarn.timeline-service.address': this.getSimpleComponentConfig('APP_TIMELINE_SERVER'),
      'mapred.job.tracker': this.getSimpleComponentConfig('JOBTRACKER'),
      'mapred.job.tracker.http.address': this.getSimpleComponentConfig('JOBTRACKER'),
      'mapreduce.history.server.http.address': this.getSimpleComponentConfig('HISTORYSERVER'),
      'oozie.base.url': this.getComponentConfigWithAffixes('OOZIE_SERVER', '://'),
      'hawq_dfs_url': this.getSimpleComponentConfig('NAMENODE'),
      'hawq_rm_yarn_address': this.getSimpleComponentConfig('RESOURCEMANAGER'),
      'hawq_rm_yarn_scheduler_address': this.getSimpleComponentConfig('RESOURCEMANAGER'),
      'fs.default.name': this.getComponentConfigWithAffixes('NAMENODE', '://'),
      'fs.defaultFS': this.getComponentConfigWithAffixes('NAMENODE', '://'),
      'hbase.rootdir': this.getComponentConfigWithAffixes('NAMENODE', '://'),
      'instance.volumes': this.getComponentConfigWithAffixes('NAMENODE', '://'),
      'yarn.log.server.url': this.getComponentConfigWithAffixes('HISTORYSERVER', '://'),
      'mapreduce.jobhistory.webapp.address': this.getSimpleComponentConfig('HISTORYSERVER'),
      'mapreduce.jobhistory.address': this.getSimpleComponentConfig('HISTORYSERVER'),
      'kafka.ganglia.metrics.host': this.getSimpleComponentConfig('GANGLIA_SERVER', false),
      'hive_master_hosts': this.getComponentsHostsConfig(['HIVE_METASTORE', 'HIVE_SERVER']),
      'hadoop_host': this.getSimpleComponentConfig('NAMENODE', false),
      'nimbus.host': this.getSimpleComponentConfig('NIMBUS', false),
      'nimbus.seeds': this.getComponentsHostsConfig('NIMBUS', true),
      'storm.zookeeper.servers': this.getComponentsHostsConfig('ZOOKEEPER_SERVER', true),
      'hawq_master_address_host': this.getSimpleComponentConfig('HAWQMASTER', false),
      'hawq_standby_address_host': this.getSimpleComponentConfig('HAWQSTANDBY', false),

      '*.broker.url': {
        type: 'host_with_component',
        component: 'FALCON_SERVER',
        modifier: {
          type: 'regexp',
          regex: 'localhost'
        }
      },

      'zookeeper.connect': getZKBasedConfig(),
      'hive.zookeeper.quorum': getZKBasedConfig(),
      'templeton.zookeeper.hosts': getZKBasedConfig(),
      'hadoop.registry.zk.quorum': getZKBasedConfig(),
      'hive.cluster.delegation.token.store.zookeeper.connectString': getZKBasedConfig(),
      'instance.zookeeper.host': getZKBasedConfig()
    }
  }.property(''),

  uniqueInitializers: {
    'ranger_admin_password': '_setRangerAdminPassword',
    'hive_database': '_initHiveDatabaseValue',
    'templeton.hive.properties': '_initTempletonHiveProperties',
    'hbase.zookeeper.quorum': '_initHBaseZookeeperQuorum',
    'yarn.resourcemanager.zk-address': '_initYarnRMzkAddress',
    'RANGER_HOST': '_initRangerHost',
    'hive.metastore.uris': '_initHiveMetastoreUris',
    'atlas.rest.address': '_initAtlasRestAddress'
  },

  initializerTypes: [
    {name: 'zookeeper_based', method: '_initAsZookeeperServersList'}
  ],

  /**
   * Some strange method that should define <code>ranger_admin_password</code>
   * TODO DELETE as soon as <code>ranger_admin_password</code> will be fetched from stack adviser!
   *
   * @param {configProperty} configProperty
   * @private
   */
  _setRangerAdminPassword: function(configProperty) {
    var value = 'P1!q' + stringUtils.getRandomString(12);
    Em.setProperties(configProperty, {'value': value, 'recommendedValue': value, 'retypedPassword': value});
    return configProperty;
  },

  /**
   * Unique initializer for <code>hive_database</code>-config
   *
   * @param {configProperty} configProperty
   * @returns {Object}
   * @private
   */
  _initHiveDatabaseValue: function (configProperty) {
    var newMySQLDBOption = Em.get(configProperty, 'options').findProperty('displayName', 'New MySQL Database');
    if (newMySQLDBOption) {
      var isNewMySQLDBOptionHidden = !App.get('supports.alwaysEnableManagedMySQLForHive') && App.get('router.currentState.name') !== 'configs' &&
        !App.get('isManagedMySQLForHiveEnabled');
      if (isNewMySQLDBOptionHidden && Em.get(configProperty, 'value') === 'New MySQL Database') {
        Em.set(configProperty, 'value', 'Existing MySQL Database');
      }
      Em.set(newMySQLDBOption, 'hidden', isNewMySQLDBOptionHidden);
    }
    return configProperty;
  },

  /**
   * Initializer for configs with value equal to hostNames-list where ZOOKEEPER_SERVER is installed
   * Value example: 'host1:2020,host2:2020,host3:2020'
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @returns {Object}
   * @private
   */
  _initAsZookeeperServersList: function (configProperty, localDB) {
    var zkHosts = localDB.masterComponentHosts.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');
    var zkHostPort = zkHosts;
    var regex = '\\w*:(\\d+)';   //regex to fetch the port
    var portValue = Em.get(configProperty, 'recommendedValue') && Em.get(configProperty, 'recommendedValue').match(new RegExp(regex));
    if (!portValue) {
      return configProperty;
    }
    if (portValue[1]) {
      for ( var i = 0; i < zkHosts.length; i++ ) {
        zkHostPort[i] = zkHosts[i] + ':' + portValue[1];
      }
    }
    this.setRecommendedValue(configProperty, '(.*)', zkHostPort);
    return configProperty;
  },

  /**
   * Unique initializer for <code>templeton.hive.properties</code>
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @returns {Object}
   * @private
   */
  _initTempletonHiveProperties: function (configProperty, localDB, dependencies) {
    var hiveMSUris = this.getHiveMetastoreUris(localDB.masterComponentHosts, dependencies['hive.metastore.uris']).replace(',', '\\,');
    if (/\/\/localhost:/g.test(Em.get(configProperty, 'value'))) {
      Em.set(configProperty, 'recommendedValue', Em.get(configProperty, 'value') + ',hive.metastore.execute.setugi=true');
    }
    this.setRecommendedValue(configProperty, "(hive\\.metastore\\.uris=)([^\\,]+)", "$1" + hiveMSUris);
    return configProperty;
  },

  /**
   * Unique initializer for <code>hbase.zookeeper.quorum</code>
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @returns {Object}
   * @private
   */
  _initHBaseZookeeperQuorum: function (configProperty, localDB) {
    if ('hbase-site.xml' === Em.get(configProperty, 'filename')) {
      var zkHosts = localDB.masterComponentHosts.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');
      this.setRecommendedValue(configProperty, "(.*)", zkHosts);
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>RANGER_HOST</code>
   * If RANGER_ADMIN-component isn't installed, this config becomes unneeded (isVisible - false, isRequired - false)
   * Value example: 'hostName'
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @returns {Object}
   * @private
   */
  _initRangerHost: function (configProperty, localDB) {
    var rangerAdminHost = localDB.masterComponentHosts.findProperty('component', 'RANGER_ADMIN');
    if(rangerAdminHost) {
      Em.setProperties(configProperty, {
        value: rangerAdminHost.hostName,
        recommendedValue: rangerAdminHost.hostName
      });
    }
    else {
      Em.setProperties(configProperty, {
        isVisible: 'false',
        isRequired: 'false'
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>yarn.resourcemanager.zk-address</code>
   * List of hosts where ZOOKEEPER_SERVER is installed
   * Port is taken from <code>dependencies.clientPort</code>
   * Value example: 'host1:111,host2:111,host3:111'
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @returns {Object}
   * @private
   */
  _initYarnRMzkAddress: function (configProperty, localDB, dependencies) {
    var value = localDB.masterComponentHosts.filterProperty('component', 'ZOOKEEPER_SERVER').map(function (component) {
      return component.hostName + ':' + dependencies.clientPort;
    }).join(',');
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: value
    });
    return configProperty;
  },

  /**
   * Unique initializer for <code>hive.metastore.uris</code>
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @returns {Object}
   * @private
   */
  _initHiveMetastoreUris: function (configProperty, localDB, dependencies) {
    if (App.config.getConfigTagFromFileName(Em.get(configProperty, 'filename')) === 'hive-site') {
      var hiveMSUris = this.getHiveMetastoreUris(localDB.masterComponentHosts, dependencies['hive.metastore.uris']);
      if (hiveMSUris) {
        this.setRecommendedValue(configProperty, "(.*)", hiveMSUris);
      }
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>atlas.rest.address</code>
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @return {Object}
   * @private
   */
  _initAtlasRestAddress: function (configProperty, localDB, dependencies) {
    var atlasTls = dependencies['atlas.enableTLS'];
    var httpPort = dependencies['atlas.server.http.port'];
    var httpsPort = dependencies['atlas.server.https.port'];
    var protocol = atlasTls ? 'https': 'http';
    var port = atlasTls ? httpsPort : httpPort;
    var value = localDB.masterComponentHosts.filterProperty('component', 'ZOOKEEPER_SERVER').map(function (component) {
      return protocol + '://' + component.hostName + ':' + port;
    }).join(',');
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: value
    });
    return configProperty;
  },

  /**
   * Get hive.metastore.uris initial value
   *
   * @param {object[]} hosts
   * @param {string} recommendedValue
   * @returns {string}
   */
  getHiveMetastoreUris: function (hosts, recommendedValue) {
    var hiveMSHosts = hosts.filterProperty('component', 'HIVE_METASTORE').mapProperty('hostName'),
      hiveMSUris = hiveMSHosts,
      regex = "\\w*:(\\d+)",
      portValue = recommendedValue && recommendedValue.match(new RegExp(regex));

    if (!portValue) {
      return '';
    }
    if (portValue[1]) {
      for (var i = 0; i < hiveMSHosts.length; i++) {
        hiveMSUris[i] = "thrift://" + hiveMSHosts[i] + ":" + portValue[1];
      }
    }
    return hiveMSUris.join(',');
  },

  /**
   * Set <code>value</code> and <code>recommendedValue</code> for <code>configProperty</code>
   * basing on <code>recommendedValue</code> with replacing <code>regex</code> for <code>replaceWith</code>
   *
   * @param {configProperty} configProperty
   * @param {string} regex
   * @param {string} replaceWith
   * @return {Object}
   */
  setRecommendedValue: function (configProperty, regex, replaceWith) {
    var recommendedValue = Em.get(configProperty, 'recommendedValue');
    recommendedValue = Em.isNone(recommendedValue) ? '' : recommendedValue;
    var re = new RegExp(regex);
    recommendedValue = recommendedValue.replace(re, replaceWith);
    Em.set(configProperty, 'recommendedValue', recommendedValue);
    var value = Em.isNone(Em.get(configProperty, 'recommendedValue')) ? '' : recommendedValue;
    Em.set(configProperty, 'value', value);
    Em.set(configProperty, 'initialValue', value);
    return configProperty;
  }
});
