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
 * Show popup with list of stack versions/components installed on host
 * @param header
 * @param hostName
 * @param items
 * @returns {App.ModalPopup}
 */
App.showHostsTableListPopup = function (header, hostName, items) {
  var isObjectsList = items.map(function (item) {
    return typeof item;
  }).contains('object');
  return App.ModalPopup.show({
    header: header,
    secondary: null,
    bodyClass: Em.View.extend({
      templateName: require('templates/common/modal_popups/hosts_table_list_popup'),
      hostName: hostName,
      items: items,
      isObjectsList: isObjectsList
    })
  });
}
