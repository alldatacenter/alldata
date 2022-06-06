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
    'models/VSchema',
    'utils/UrlLinks'
], function(require, Globals, BaseCollection, VSchema, UrlLinks) {
    'use strict';
    var VSchemaList = BaseCollection.extend(
        //Prototypal attributes
        {
            url: UrlLinks.baseURL,
            model: VSchema,
            initialize: function() {
                this.modelName = 'VSchema';
                this.modelAttrName = 'results';
            },
            parseRecords: function(resp, options) {
                try {
                    if (!this.modelAttrName) {
                        throw new Error("this.modelAttrName not defined for " + this);
                    }
                    this.keyList = resp[this.modelAttrName].dataType.attributeDefinitions;
                    if (resp[this.modelAttrName].dataType.superTypes) {
                        if (resp[this.modelAttrName].dataType.superTypes.indexOf("Asset") != -1) {
                            this.keyList.push({
                                "name": "name",
                                "dataTypeName": "string",
                                "isComposite": false,
                                "isIndexable": true,
                                "isUnique": false,
                                "multiplicity": {},
                                "reverseAttributeName": null
                            })
                        }
                    }
                    var arr = [];
                    resp[this.modelAttrName].rows.forEach(function(d) {
                        arr.push(d);
                    });
                    return arr;
                } catch (e) {
                    console.log(e);
                }
            },
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
    return VSchemaList;
});