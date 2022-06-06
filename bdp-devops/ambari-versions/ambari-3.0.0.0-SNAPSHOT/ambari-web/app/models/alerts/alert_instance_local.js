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
 * Just a copy of <code>App.AlertInstance</code>, used to save separately alert instances for host and definition
 * Sometimes alert instances can't be stored in the one model. Example: user is in the definition details page and open
 * alerts popup. Different lists of instances should be shown and loaded. Also it's pretty hard to determine, which
 * models should be removed from store, when user navigates in the popup or just leave the page
 * @type {DS.Model}
 */
App.AlertInstanceLocal = App.AlertInstance.extend({});

App.AlertInstanceLocal.FIXTURES = [];