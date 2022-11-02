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
require('utils/configs/ha_config_initializer_class');
require('utils/configs/hosts_based_initializer_mixin');

/**
 * @typedef {topologyLocalDB} extendedTopologyLocalDB
 * @property {string[]} installedServices list of installed service names
 */

/**
 * Setting for <code>rename</code>-initializer
 * Used for configs which should be renamed
 * Replace some part if their names with <code>namespaceId</code> (provided by user on the wizard's 1st step)
 *
 * @param {string} toReplace
 * @returns {{type: string, toReplace: string}}
 */
function getRenameWithNamespaceConfig(toReplace) {
  return {
    type: 'rename',
    toReplace: toReplace
  };
}

/**
 * Settings for <code>namespace</code>-initializer
 * Used for configs with value equal to the <code>namespaceId</code> (provided by user on the wizard's 1st step)
 * Value may be customized with prefix and suffix
 *
 * @param {string} [prefix=''] substring added before namespace in the replace
 * @param {string} [suffix=''] substring added after namespace in the replace
 * @returns {{type: string, modifier: {prefix: (string), suffix: (string)}}}
 */
function getNamespaceConfig (prefix, suffix) {
  prefix = prefix || '';
  suffix = suffix || '';
  return {
    type: 'namespace',
    modifier: {
      prefix: prefix,
      suffix: suffix
    }
  }
}

/**
 * Settings for <code>replace_namespace</code>
 * Used for configs with values that have to be modified with replacing some part of them
 * to the <code>namespaceId</code> (provided by user on the wizard's 1st step)
 *
 * @param {string} toReplace
 * @returns {{type: string, toReplace: *}}
 */
function getReplaceNamespaceConfig(toReplace) {
  return {
    type: 'replace_namespace',
    toReplace: toReplace
  };
}

/**
 * Initializer for configs that are updated when NameNode HA-mode is activated
 *
 * @class {NnHaConfigInitializer}
 */
App.NnHaConfigInitializer = App.HaConfigInitializerClass.create(App.HostsBasedInitializerMixin, {

  initializers: function () {

    return {
      'dfs.ha.namenodes.${dfs.nameservices}': getRenameWithNamespaceConfig('${dfs.nameservices}'),
      'dfs.namenode.rpc-address.${dfs.nameservices}.nn1': [
        this.getHostWithPortConfig('NAMENODE', true, '', '', 'nnRpcPort', true),
        getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.namenode.rpc-address.${dfs.nameservices}.nn2': [
        this.getHostWithPortConfig('NAMENODE', false, '', '', '8020', false),
        getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.namenode.http-address.${dfs.nameservices}.nn1': [
        this.getHostWithPortConfig('NAMENODE', true, '', '', 'nnHttpPort', true),
        getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.namenode.http-address.${dfs.nameservices}.nn2': [
        this.getHostWithPortConfig('NAMENODE', false, '', '', '50070', false),
        getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.namenode.https-address.${dfs.nameservices}.nn1': [
        this.getHostWithPortConfig('NAMENODE', true, '', '', 'nnHttpsPort', true),
        getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.namenode.https-address.${dfs.nameservices}.nn2': [
        this.getHostWithPortConfig('NAMENODE', false, '', '', '50470', false),
        getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.client.failover.proxy.provider.${dfs.nameservices}': getRenameWithNamespaceConfig('${dfs.nameservices}'),
      'dfs.nameservices': getNamespaceConfig(),
      'dfs.internal.nameservices': getNamespaceConfig(),
      'fs.defaultFS': getNamespaceConfig('hdfs://'),
      'dfs.namenode.shared.edits.dir': [
        this.getHostsWithPortConfig('JOURNALNODE', 'qjournal://', '/${dfs.nameservices}', ';', '8485', false),
        getReplaceNamespaceConfig('${dfs.nameservices}')
      ],
      'ha.zookeeper.quorum': this.getHostsWithPortConfig('ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true)
    };
  }.property(),

  uniqueInitializers: {
    'hbase.rootdir': '_initHbaseRootDir',
    'hawq_dfs_url': '_initHawqDfsUrl',
    'instance.volumes': '_initInstanceVolumes',
    'instance.volumes.replacements': '_initInstanceVolumesReplacements',
    'dfs.journalnode.edits.dir': '_initDfsJnEditsDir',
    'xasecure.audit.destination.hdfs.dir': '_initXasecureAuditDestinationHdfsDir'
  },

  initializerTypes: [
    {name: 'rename', method: '_initWithRename'},
    {name: 'namespace', method: '_initAsNamespace'},
    {name: 'replace_namespace', method: '_initWithNamespace'}
  ],

  /**
   * Initializer for configs that should be renamed
   * Some part of their names should be replaced with <code>namespaceId</code> (user input this value on the wizard's 1st step)
   * Affects both - name and displayName
   * <b>Important! It's not the same as <code>_updateInitializers</code>!</b>
   * Main diff - this initializer used for configs
   * with names that come with some "predicates" in their names. <code>_updateInitializers</code> is used to determine needed
   * config name that depends on other config values or someting else
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initWithRename
   */
  _initWithRename: function (configProperty, localDB, dependencies, initializer) {
    var replaceWith = dependencies.namespaceId;
    var toReplace = initializer.toReplace;
    Em.assert('`dependencies.namespaceId` should be not empty string', !!replaceWith);
    var name = Em.getWithDefault(configProperty, 'name', '');
    var displayName = Em.getWithDefault(configProperty, 'displayName', '');
    name = name.replace(toReplace, replaceWith);
    displayName = displayName.replace(toReplace, replaceWith);
    Em.setProperties(configProperty, {
      name: name,
      displayName: displayName
    });
    return configProperty;
  },

  /**
   * Initializer for configs wih value equal to the <code>namespaceId</code> (user input this value on the wizard's 1st step)
   * Value may be customized with prefix and suffix (see <code>initializer.modifier</code>)
   * Value-examples: 'SOME_COOL_PREFIXmy_namespaceSOME_COOL_SUFFIX', 'my_namespace'
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initAsNamespace
   */
  _initAsNamespace: function (configProperty, localDB, dependencies, initializer) {
    var value = dependencies.namespaceId;
    Em.assert('`dependencies.namespaceId` should be not empty string', !!value);
    value = initializer.modifier.prefix + value + initializer.modifier.suffix;
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: value
    });
    return configProperty;
  },

  /**
   * Initializer for configs with value that should be modified with replacing some substring
   * to the <code>namespaceId</code> (user input this value on the wizard's 1st step)
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initWithNamespace
   */
  _initWithNamespace: function (configProperty, localDB, dependencies, initializer) {
    var replaceWith = dependencies.namespaceId;
    var toReplace = initializer.toReplace;
    Em.assert('`dependencies.namespaceId` should be not empty string', !!replaceWith);
    var value = Em.get(configProperty, 'value').replace(toReplace, replaceWith);
    var recommendedValue = Em.get(configProperty, 'recommendedValue').replace(toReplace, replaceWith);
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: recommendedValue
    });
    return configProperty;
  },

  /**
   * Unique initializer for <code>hbase.rootdir</code>
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initHbaseRootDir
   * @return {object}
   * @private
   */
  _initHbaseRootDir: function (configProperty, localDB, dependencies, initializer) {
    var fileName = Em.get(configProperty, 'filename');
    var args = [].slice.call(arguments);
    if ('hbase-site' === fileName) {
      return this._initHbaseRootDirForHbase.apply(this, args);
    }
    if('ams-hbase-site' === fileName) {
      return this._initHbaseRootDirForAMS.apply(this, args);
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>hbase.rootdir</code> (HBASE-service)
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initHbaseRootDirForHbase
   * @return {object}
   * @private
   */
  _initHbaseRootDirForHbase: function (configProperty, localDB, dependencies, initializer) {
    if (localDB.installedServices.contains('HBASE')) {
      var value = dependencies.serverConfigs.findProperty('type', 'hbase-site').properties['hbase.rootdir'].replace(/\/\/[^\/]*/, '//' + dependencies.namespaceId);
      Em.setProperties(configProperty, {
        value: value,
        recommendedValue: value
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>hbase.rootdir</code> (Ambari Metrics-service)
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initHbaseRootDirForAMS
   * @return {object}
   * @private
   */
  _initHbaseRootDirForAMS: function (configProperty, localDB, dependencies, initializer) {
    if (localDB.installedServices.contains('AMBARI_METRICS')) {
      var value = dependencies.serverConfigs.findProperty('type', 'ams-hbase-site').properties['hbase.rootdir'];
      var currentNameNodeHost = localDB.masterComponentHosts.filterProperty('component', 'NAMENODE').findProperty('isInstalled', true).hostName;
      if(value.contains("hdfs://" + currentNameNodeHost)){
        value = value.replace(/\/\/[^\/]*/, '//' + dependencies.namespaceId);
        configProperty.isVisible = true;
      }
      Em.setProperties(configProperty, {
        value: value,
        recommendedValue: value
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>hawq_dfs_url</code> (Hawq service)
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initHawqDfsUrl
   * @return {object}
   * @private
   */
  _initHawqDfsUrl: function (configProperty, localDB, dependencies, initializer) {
    if (localDB.installedServices.contains('HAWQ')) {
      var value = dependencies.serverConfigs.findProperty('type', 'hawq-site').properties['hawq_dfs_url'].replace(/(^.*:[0-9]+)(?=\/)/, dependencies.namespaceId);
      Em.setProperties(configProperty, {
        value: value,
        recommendedValue: value
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>instance.volumes</code>
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initInstanceVolumes
   * @return {object}
   * @private
   */
  _initInstanceVolumes: function (configProperty, localDB, dependencies, initializer) {
    if (localDB.installedServices.contains('ACCUMULO')) {
      var oldValue = dependencies.serverConfigs.findProperty('type', 'accumulo-site').properties['instance.volumes'];
      var value = oldValue.replace(/\/\/[^\/]*/, '//' + dependencies.namespaceId);
      Em.setProperties(configProperty, {
        value: value,
        recommendedValue: value
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>instance.volumes.replacements</code>
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initInstanceVolumesReplacements
   * @return {object}
   * @private
   */
  _initInstanceVolumesReplacements: function (configProperty, localDB, dependencies, initializer) {
    if (localDB.installedServices.contains('ACCUMULO')) {
      var oldValue = dependencies.serverConfigs.findProperty('type', 'accumulo-site').properties['instance.volumes'];
      var value = oldValue.replace(/\/\/[^\/]*/, '//' + dependencies.namespaceId);
      var replacements = oldValue + " " + value;
      Em.setProperties(configProperty, {
        value: replacements,
        recommendedValue: replacements
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>dfs.journalnode.edits.dir</code>
   * Used only for Windows Stacks
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initDfsJnEditsDir
   * @return {object}
   * @private
   */
  _initDfsJnEditsDir: function (configProperty, localDB, dependencies, initializer) {
    if (App.get('isHadoopWindowsStack') && localDB.installedServices.contains('HDFS')) {
      var value = dependencies.serverConfigs.findProperty('type', 'hdfs-site').properties['dfs.journalnode.edits.dir'];
      Em.setProperties(configProperty, {
        value: value,
        recommendedValue: value
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>xasecure.audit.destination.hdfs.dir</code>
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initXasecureAuditDestinationHdfsDir
   * @return {object}
   * @private
   */
  _initXasecureAuditDestinationHdfsDir: function(configProperty, localDB, dependencies, initializer) {
    if (localDB.installedServices.contains('RANGER')) {
      var oldValue = dependencies.serverConfigs.findProperty('type', 'ranger-env').properties['xasecure.audit.destination.hdfs.dir'];
      // Example of value - hdfs://c6401.ambari.apache.org:8020/ranger/audit
      // Replace hostname and port with Namespace
      var valueArray = oldValue.split("/");
      valueArray[2] = dependencies.namespaceId;
      var newValue = valueArray.join("/");
      Em.setProperties(configProperty, {
        value: newValue,
        recommendedValue: newValue
      });
    }
    return configProperty;
  }

});
