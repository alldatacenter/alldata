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
require('utils/configs/move_component_config_initializer_class');

/**
 * Initializer for configs which should be affected when NameNode is moved from one host to another
 * If NameNode HA-mode is already activated, several configs are also updated
 *
 * @instance MoveComponentConfigInitializerClass
 */
App.MoveNameNodeConfigInitializer = App.MoveComponentConfigInitializerClass.create({

  initializers: {
    'dfs.namenode.http-address.{{namespaceId}}.{{suffix}}': App.MoveComponentConfigInitializerClass.getTargetHostConfig(50070),
    'dfs.namenode.https-address.{{namespaceId}}.{{suffix}}': App.MoveComponentConfigInitializerClass.getTargetHostConfig(50470),
    'dfs.namenode.rpc-address.{{namespaceId}}.{{suffix}}': App.MoveComponentConfigInitializerClass.getTargetHostConfig(8020),
    'dfs.namenode.servicerpc-address.{{namespaceId}}.{{suffix}}': App.MoveComponentConfigInitializerClass.getTargetHostConfig(8021),
  },

  uniqueInitializers: {
    'instance.volumes': '_initInstanceVolumes',
    'instance.volumes.replacements': '_initInstanceVolumesReplacements',
    'hbase.rootdir': '_initHbaseRootDir',
    'hawq_dfs_url': '_initHawqDfsUrl'
  },

  /**
   * Unique initializer for <code>instance.volumes</code>-config
   * Value example: 'hdfs://host1:8020/apps/accumulo/data'
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @returns {object}
   * @private
   * @method _initInstanceVolumes
   */
  _initInstanceVolumes: function (configProperty, localDB, dependencies) {
    if (!App.get('isHaEnabled') && localDB.installedServices.contains('ACCUMULO')) {
      var value = Em.get(configProperty, 'value');
      value = value.replace(/\/\/[^\/]*/, '//' + dependencies.targetHostName + ':8020');
      Em.set(configProperty, 'value', value);
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>instance.volumes.replacements</code>-config
   * Value example: 'hdfs://host1:8020/apps/accumulo/data hdfs://host2:8020/apps/accumulo/data'
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @returns {object}
   * @private
   * @method _initInstanceVolumesReplacements
   */
  _initInstanceVolumesReplacements: function (configProperty, localDB, dependencies) {
    if (!App.get('isHaEnabled') && localDB.installedServices.contains('ACCUMULO')) {
      var target = 'hdfs://' + dependencies.targetHostName + ':8020' + '/apps/accumulo/data';
      var source = 'hdfs://' + dependencies.sourceHostName + ':8020' + '/apps/accumulo/data';
      var value = source + ' ' + target;
      Em.set(configProperty, 'value', value);
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>hbase.rootdir</code>-config (for HIVE service)
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @returns {object}
   * @private
   * @method _initHbaseRootDir
   */
  _initHbaseRootDir: function (configProperty, localDB, dependencies) {
    if (!App.get('isHaEnabled') && localDB.installedServices.contains('HBASE') && 'hbase-site' === configProperty.filename) {
      var value = Em.get(configProperty, 'value');
      value = value.replace(/\/\/[^\/]*/, '//' + dependencies.targetHostName + ':8020');
      Em.set(configProperty, 'value', value);
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>hawq_dfs_url</code>-config (for HAWQ service)
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @returns {object}
   * @private
   * @method _initHawqDfsUrl
   */
  _initHawqDfsUrl: function (configProperty, localDB, dependencies) {
    if (!App.get('isHaEnabled') && localDB.installedServices.contains('HAWQ') && 'hawq-site' === configProperty.filename) {
      var value = Em.get(configProperty, 'value');
      value = value.replace(/(.*):/, dependencies.targetHostName + ':');
      Em.set(configProperty, 'value', value);
    }
    return configProperty;
  }

});
