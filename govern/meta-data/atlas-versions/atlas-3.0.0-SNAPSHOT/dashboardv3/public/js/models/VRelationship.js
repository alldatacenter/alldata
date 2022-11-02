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

define(['require',
    'utils/Globals',
    'models/BaseModel',
    'utils/UrlLinks'
], function(require, Globals, VBaseModel, UrlLinks) {
    'use strict';
    var VRelationship = VBaseModel.extend({

        urlRoot: UrlLinks.relationshipApiUrl(),

        defaults: {},

        serverSchema: {},

        idAttribute: 'id',

        initialize: function() {
            this.modelName = 'VRelationship';
        },
        toString: function() {
            return this.get('name');
        },
        /*************************
         * Non - CRUD operations
         *************************/

        getRelationship: function(token, options) {
            var url = UrlLinks.relationshipApiUrl(token);
            options = _.extend({
                contentType: 'application/json',
                dataType: 'json'
            }, options);

            return this.constructor.nonCrudOperation.call(this, url, 'GET', options);
        },
        saveRelationship: function(options) {
            var url = UrlLinks.relationshipApiUrl();
            options = _.extend({
                contentType: 'application/json',
                dataType: 'json'
            }, options);
            return this.constructor.nonCrudOperation.call(this, url, 'PUT', options);
        },
    }, {});
    return VRelationship;
});