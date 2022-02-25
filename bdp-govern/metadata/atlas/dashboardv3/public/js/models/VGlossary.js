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
    var VGlossary = VBaseModel.extend({

        urlRoot: UrlLinks.glossaryApiUrl(),

        defaults: {},

        serverSchema: {},

        idAttribute: 'guid',

        initialize: function() {
            this.modelName = 'VGlossary';
        },
        toString: function() {
            return this.get('name');
        },
        createEditCategory: function(options) {
            var type = "POST",
                url = UrlLinks.categoryApiUrl();
            if (options.guid) {
                type = "PUT";
                url = UrlLinks.categoryApiUrl({ guid: options.guid });
            }
            options = _.extend({
                contentType: 'application/json',
                dataType: 'json'
            }, options);
            return this.constructor.nonCrudOperation.call(this, url, type, options);
        },
        createEditTerm: function(options) {
            var type = "POST",
                url = UrlLinks.termApiUrl();
            if (options.guid) {
                type = "PUT";
                url = UrlLinks.termApiUrl({ guid: options.guid });
            }
            options = _.extend({
                contentType: 'application/json',
                dataType: 'json'
            }, options);
            return this.constructor.nonCrudOperation.call(this, url, type, options);
        },
        deleteCategory: function(guid, options) {
            var url = UrlLinks.categoryApiUrl({ "guid": guid });
            options = _.extend({
                contentType: 'application/json',
                dataType: 'json'
            }, options);
            return this.constructor.nonCrudOperation.call(this, url, 'DELETE', options);
        },
        deleteTerm: function(guid, options) {
            var url = UrlLinks.termApiUrl({ "guid": guid });
            options = _.extend({
                contentType: 'application/json',
                dataType: 'json'
            }, options);
            return this.constructor.nonCrudOperation.call(this, url, 'DELETE', options);
        },
        assignTermToEntity: function(guid, options) {
            var url = UrlLinks.termToEntityApiUrl(guid);
            options = _.extend({
                contentType: 'application/json',
                dataType: 'json'
            }, options);
            return this.constructor.nonCrudOperation.call(this, url, 'POST', options);
        },
        assignTermToCategory: function(options) {
            return this.createEditCategory(options);
        },
        assignCategoryToTerm: function(options) {
            return this.createEditTerm(options);
        },
        assignTermToAttributes: function(options) {
            return this.createEditTerm(options);
        },
        removeTermFromAttributes: function(options) {
            return this.createEditTerm(options);
        },
        removeTermFromEntity: function(guid, options) {
            var url = UrlLinks.termToEntityApiUrl(guid);
            options = _.extend({
                contentType: 'application/json',
                dataType: 'json'
            }, options);
            return this.constructor.nonCrudOperation.call(this, url, 'PUT', options);
        },
        removeTermFromCategory: function() {

        },
        removeCategoryFromTerm: function() {}
    }, {});
    return VGlossary;
});