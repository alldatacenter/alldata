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
 * THIS IS NOT USED FOR NOW
 * FOR CONFIG GROUPS WE ARE USING OLD MODELS AND LOGIC
 */

var App = require('app');

App.ThemeCondition = DS.Model.extend({
  /**
   * unique id generated as <code>config_name<code><code>filename<code>
   * @property {string}
   */
  id: DS.attr('string'),

  /**
   *  type can be `config`, `service`
   */
  type: DS.attr('string'),

  /**
   * This is specific to subsection and subsectionTab type
   */

  name: DS.attr('string'),

  /**
   * theme from which this is coming from , eg: default, database, credentials, etc.
   */
  themeName: DS.attr('string'),

  /**
   * Name of the config that is being affected with the condition
   */
  configName: DS.attr('string'),

  /**
   * File name to which the config getting affected belongs
   */
  fileName: DS.attr('string'),

  /**
   * List of configs whose values affect the config
   * Each Object in an array consists of configName and fileName
   */
  configs: DS.attr('array', {defaultValue: []}),

  /**
   * resource can be `config`, `service`
   */
  resource: DS.attr('string', {defaultValue: 'config'}),

  /**
   * conditional String which can be evaluated to boolean result.
   * If evaluated result of this staring is true then use the statement provided by `then` attribute.
   * Otherwise use the attribute provided by `else` attributes
   */
  if: DS.attr('string'),
  then: DS.attr('object', {defaultValue: null}),
  else: DS.attr('object', {defaultValue: null})

});

App.ThemeCondition.FIXTURES = [];
