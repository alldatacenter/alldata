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
    'backbone',
    'modules/Modal',
    'utils/Utils',
    'hbs!tmpl/search/SearchQuery_tmpl',
    'utils/Globals',
    'utils/Enums'
], function(require, Backbone, Modal, Utils, SearchQuery_Tmpl, Globals, Enums) {

    var SearchQueryView = Backbone.Marionette.LayoutView.extend(
        /** @lends SearchQueryView */
        {
            _viewName: 'SearchQueryView',

            template: SearchQuery_Tmpl,



            /** Layout sub regions */
            regions: {
                RQueryBuilder: '#r_queryBuilder',
            },


            /** ui selector cache */
            ui: {},
            /** ui events hash */
            events: function() {
                var events = {};
                return events;
            },
            /**
             * intialize a new SearchQueryView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'value', 'entityDefCollection', 'typeHeaders', 'searchVent', 'enumDefCollection', 'classificationDefCollection', 'businessMetadataDefCollection', 'tag', 'searchTableFilters'));
                this.bindEvents();
                var that = this;
                this.modal = new Modal({
                    title: 'Attribute Filter',
                    content: this,
                    allowCancel: true,
                    mainClass: 'modal-lg',
                    okCloses: false,
                    buttons: [{
                            text: 'Cancel',
                            btnClass: "cancel btn-action",
                            title: 'Cancel'
                        }, {
                            text: 'Apply',
                            btnClass: "ok btn-atlas",
                            title: "Apply the filters and close popup (won't perform search)"
                        },
                        {
                            text: 'Search',
                            btnClass: "ok search btn-atlas",
                            title: 'Apply the filters and do search'
                        }
                    ]
                }).open();
                this.modal.on('closeModal', function() {
                    that.modal.trigger('cancel');
                });
            },
            onRender: function() {
                var obj = {
                    value: this.value,
                    searchVent: this.searchVent,
                    entityDefCollection: this.entityDefCollection,
                    enumDefCollection: this.enumDefCollection,
                    classificationDefCollection: this.classificationDefCollection,
                    businessMetadataDefCollection: this.businessMetadataDefCollection,
                    searchTableFilters: this.searchTableFilters,
                    typeHeaders: this.typeHeaders
                }

                if (this.tag) {
                    obj['tag'] = true;
                    obj['attrObj'] = this.classificationDefCollection.fullCollection.find({ name: this.value.tag });
                    if (obj.attrObj) {
                        obj.attrObj = Utils.getNestedSuperTypeObj({
                            data: obj.attrObj.toJSON(),
                            collection: this.classificationDefCollection,
                            attrMerge: true,
                        });
                    }
                    if (Globals[this.value.tag] || Globals[Enums.addOnClassification[0]]) {
                        obj['systemAttrArr'] = (Globals[this.value.tag] || Globals[Enums.addOnClassification[0]]).attributeDefs;
                    }
                } else {
                    obj['type'] = true;
                    obj['attrObj'] = this.entityDefCollection.fullCollection.find({ name: this.value.type });
                    if (obj.attrObj) {
                        obj.attrObj = Utils.getNestedSuperTypeObj({
                            data: obj.attrObj.toJSON(),
                            collection: this.entityDefCollection,
                            attrMerge: true
                        });
                    }
                    if (Globals[this.value.type] || Globals[Enums.addOnEntities[0]]) {
                        obj['systemAttrArr'] = (Globals[this.value.type] || Globals[Enums.addOnEntities[0]]).attributeDefs;
                    }
                }
                this.renderQueryBuilder(obj);
                // this.showHideFilter(this.value);
            },
            bindEvents: function() {},
            renderQueryBuilder: function(obj) {
                var that = this;
                require(['views/search/QueryBuilderView'], function(QueryBuilderView) {
                    that.RQueryBuilder.show(new QueryBuilderView(obj));
                });
            }
        });
    return SearchQueryView;
});