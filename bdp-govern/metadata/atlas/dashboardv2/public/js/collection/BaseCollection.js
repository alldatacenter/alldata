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
    'utils/Utils',
    'utils/CommonViewFunction',
    'backbone.paginator'
], function(require, Globals, Utils, CommonViewFunction) {
    'use strict';

    var BaseCollection = Backbone.PageableCollection.extend(
        /** @lends BaseCollection.prototype */
        {
            /**
             * BaseCollection's initialize function
             * @augments Backbone.PageableCollection
             * @constructs
             */

            initialize: function() {
                this.sort_key = 'id';
            },
            comparator: function(key, value) {
                key = key.get(this.sort_key);
                value = value.get(this.sort_key);
                return key > value ? 1 : key < value ? -1 : 0;
            },
            sortByKey: function(sortKey) {
                this.sort_key = sortKey;
                this.sort();
            },
            /**
             * state required for the PageableCollection
             */
            state: {
                firstPage: 0,
                pageSize: Globals.settings.PAGE_SIZE
            },
            mode: 'client',
            /**
             * override the parseRecords of PageableCollection for our use
             */
            parseRecords: function(resp, options) {
                this.responseData = {
                    dataType: resp.dataType,
                    query: resp.query,
                    queryType: resp.queryType,
                    requestId: resp.requestId
                };
                try {
                    if (!this.modelAttrName) {
                        throw new Error("this.modelAttrName not defined for " + this);
                    }
                    return resp[this.modelAttrName];
                } catch (e) {
                    console.log(e);
                }
            },

            ////////////////////////////////////////////////////////////
            // Overriding backbone-pageable page handlers methods   //
            ////////////////////////////////////////////////////////////
            getFirstPage: function(options) {
                return this.getPage('first', _.extend({
                    reset: true
                }, options));
            },

            getPreviousPage: function(options) {
                return this.getPage("prev", _.extend({
                    reset: true
                }, options));
            },

            getNextPage: function(options) {
                return this.getPage("next", _.extend({
                    reset: true
                }, options));
            },

            getLastPage: function(options) {
                return this.getPage("last", _.extend({
                    reset: true
                }, options));
            },
            hasPrevious: function(options) {
                return this.hasPreviousPage();
            },
            hasNext: function(options) {
                return this.hasNextPage();
            }
            /////////////////////////////
            // End overriding methods //
            /////////////////////////////

        },
        /** BaseCollection's Static Attributes */
        {
            // Static functions
            getTableCols: function(cols, collection, defaultSortDirection) {
                var retCols = _.map(cols, function(v, k, l) {
                    var defaults = collection.constructor.tableCols[k];
                    if (!defaults) {
                        //console.log("Error!! " + k + " not found in collection: " , collection);
                        defaults = {};
                    }
                    return _.extend({
                        'name': k,
                        direction: defaultSortDirection ? defaultSortDirection : null,
                    }, defaults, v);
                });
                return retCols;
            },
            nonCrudOperation: function(url, requestMethod, options) {
                var that = this;
                options['beforeSend'] = CommonViewFunction.addRestCsrfCustomHeader;
                if (options.data && typeof options.data === "object") {
                    options.data = JSON.stringify(options.data);
                }
                return Backbone.sync.call(this, null, this, _.extend({
                    url: url,
                    type: requestMethod
                }, options));
            }
        });
    return BaseCollection;
});