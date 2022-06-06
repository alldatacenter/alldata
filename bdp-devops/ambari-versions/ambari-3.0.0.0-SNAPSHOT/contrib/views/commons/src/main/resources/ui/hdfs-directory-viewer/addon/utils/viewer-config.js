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

import Ember from 'ember';

export default Ember.Object.extend({

  /**
   * Set this to true, if only directory listing is required.
   */
  showOnlyDirectories: false,

  /**
   * Set this to true, if refresh of current path is required.
   */
  showRefreshButton: false,

  /**
   * Override these for different Icon. Refer https://github.com/jonmiles/bootstrap-treeview
   */
  expandIcon: "fa fa-plus",
  collapseIcon: "fa fa-minus",
  //Custom
  fileIcon: "fa fa-file",
  folderIcon: "fa fa-folder",

  /**
   * Override to return the headers add to the XHR call made for various operations.
   * The Overriding object can also merge its result with the default in this object.
   */
  getHeaders() {
    return {"X-Requested-By": "ambari"};
  },

  /**
   * Return the URL endpoint for XHR call meant for listing Directories
   * @param pathParams
   * @returns {string}
   */
  listDirectoryUrl(pathParams) {
    return `/listdir?${pathParams}`;
  },

  createDirectoryUrl() {
    return '/mkdir';
  }

});
