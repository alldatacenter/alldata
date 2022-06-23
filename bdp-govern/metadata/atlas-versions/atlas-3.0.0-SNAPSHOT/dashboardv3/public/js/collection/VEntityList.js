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
    'models/VEntity',
    'utils/UrlLinks'
], function(require, Globals, BaseCollection, VEntity, UrlLinks) {
    'use strict';
    var VEntityList = BaseCollection.extend(
        //Prototypal attributes
        {
            url: UrlLinks.entitiesApiUrl(),

            model: VEntity,

            initialize: function() {
                this.modelName = 'VEntity';
                this.modelAttrName = 'entityDefs';
            },
            parseRecords: function(resp, options) {
                try {
                    // if (!this.modelAttrName) {
                    //     throw new Error("this.modelAttrName not defined for " + this);
                    // }
                    if (resp.entity && resp.referredEntities) {
                        var obj = {
                            entity: resp.entity,
                            referredEntities: resp.referredEntities
                        }
                        return obj;
                    } else if (resp[this.modelAttrName]) {
                        return resp[this.modelAttrName];
                    } else {
                        return resp
                    }

                } catch (e) {
                    console.log(e);
                }
            },
            getAdminData: function(options) {
                var url = UrlLinks.adminApiUrl();
                options = _.extend({
                    contentType: 'application/json',
                    dataType: 'json'
                }, options);
                return this.constructor.nonCrudOperation.call(this, url, 'POST', options);
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
    return VEntityList;
});