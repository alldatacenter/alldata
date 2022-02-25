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
require('utils/configs/move_hive_component_config_initializer_class');

/**
 * Initializer for configs which should be affected when Hive Metastore is moved from one host to another
 *
 * @type {MoveHiveComponentConfigInitializerClass}
 */
App.MoveHmConfigInitializer = App.MoveHiveComponentConfigInitializerClass.create({

  initializers: {
    'hadoop.proxyuser.{{hiveUser}}.hosts': App.MoveHiveComponentConfigInitializerClass.getHostsWithComponentsConfig(['HIVE_SERVER', 'HIVE_METASTORE', 'HIVE_SERVER_INTERACTIVE'], 'HIVE_METASTORE')
  },

  uniqueInitializers: {
    'hive.metastore.uris': '_initHiveMetastoreUris',
    'templeton.hive.properties': '_initTempletonHiveProperties'
  },

  /**
   * Unique initializer for <code>hive.metastore.uris</code>-config
   * Value example: 'thrift://host1:1234,thrift://host2:1234,thrift://host3:1234'
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @returns {object}
   * @private
   * @method _initHiveMetastoreUris
   */
  _initHiveMetastoreUris: function (configProperty, localDB, dependencies) {
    if (App.config.getConfigTagFromFileName(Em.get(configProperty, 'filename')) === 'hive-site') {
      var hiveMSHosts = this.__getHmHostsConsideringMoved(localDB, dependencies);

      var value = Em.get(configProperty, 'value');

      var port = value.match(/:[0-9]{2,4}/);
      port = port ? port[0].slice(1) : '9083';

      value = hiveMSHosts.uniq().map(function (hiveMSHost) {
        return 'thrift://' + hiveMSHost + ':' + port;
      }).join(',');

      Em.set(configProperty, 'value', value);
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>templeton.hive.properties</code>-config
   * Replace existing hosts with new
   * Value example: 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true'
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @returns {object}
   * @private
   * @method _initTempletonHiveProperties
   */
  _initTempletonHiveProperties: function (configProperty, localDB, dependencies) {
    var hiveMSHosts = this.__getHmHostsConsideringMoved(localDB, dependencies);
    var value = Em.get(configProperty, 'value');
    value = value.replace(/thrift.+[0-9]{2,},/i, hiveMSHosts.join('\\,') + ',');

    Em.set(configProperty, 'value', value);
    return configProperty;
  },

  /**
   * Get list of hosts where HIVE_METASTORE exists considering component's moving (host where it was is removed and host
   * where it will be is added)
   *
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @returns {string[]}
   * @private
   * @method __getHmHostsConsideringMoved
   */
  __getHmHostsConsideringMoved: function (localDB, dependencies) {
    var hiveMSHosts = localDB.masterComponentHosts.filterProperty('component', 'HIVE_METASTORE').mapProperty('hostName');
    hiveMSHosts = hiveMSHosts.removeObject(dependencies.sourceHostName).addObject(dependencies.targetHostName);
    return hiveMSHosts.uniq();
  }

});
