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
    'collection/BaseCollection',
    'models/VGlossary',
    'utils/UrlLinks'
], function(require, Globals, BaseCollection, VGlossary, UrlLinks) {
    'use strict';
    var VGlossaryList = BaseCollection.extend(
        //Prototypal attributes
        {
            url: UrlLinks.glossaryApiUrl(),

            model: VGlossary,

            initialize: function() {
                this.modelName = 'VGlossary';
                this.modelAttrName = '';
            },
            parseRecords: function(resp, options) {
                if (_.isEmpty(this.modelAttrName)) {
                    return resp;
                } else {
                    return resp[this.modelAttrName]
                }
            },
            getCategory: function(options) {
                var url = UrlLinks.categoryApiUrl({ "guid": options.guid, "related": options.related }),
                    apiOptions = _.extend({
                        contentType: 'application/json',
                        dataType: 'json'
                    }, options.ajaxOptions);
                return this.constructor.nonCrudOperation.call(this, url, 'GET', apiOptions);
            },
            getTerm: function(options) {
                var url = UrlLinks.termApiUrl({ "guid": options.guid, "related": options.related }),
                    apiOptions = _.extend({
                        contentType: 'application/json',
                        dataType: 'json'
                    }, options.ajaxOptions);
                return this.constructor.nonCrudOperation.call(this, url, 'GET', apiOptions);
            }
        },
        //Static Class Members
        {
            /**
             * Table Cols to be passed to Backgrid
             * UI has to use this as base and extend this.
             *
             */
            tableCols: {}
        }
    );
    return VGlossaryList;
});