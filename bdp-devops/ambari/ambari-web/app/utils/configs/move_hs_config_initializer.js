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
 * Initializer for configs which should be affected when Hive Server is moved from one host to another
 *
 * @type {MoveHiveComponentConfigInitializerClass}
 */
App.MoveHsConfigInitializer = App.MoveHiveComponentConfigInitializerClass.create({

  initializers: {
    'hadoop.proxyuser.{{hiveUser}}.hosts': App.MoveHiveComponentConfigInitializerClass.getHostsWithComponentsConfig(['HIVE_SERVER', 'HIVE_METASTORE', 'HIVE_SERVER_INTERACTIVE'], 'HIVE_SERVER')
  }

});