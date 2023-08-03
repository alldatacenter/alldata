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
    'hbs!tmpl/search/save/SaveSearchView_tmpl',
    'views/search/save/SaveSearchItemView',
    'collection/VSearchList',
    'utils/Utils',
    'utils/Globals',
    'utils/UrlLinks',
    'utils/CommonViewFunction',
    'utils/Messages'
], function(require, Backbone, SaveSearchViewTmpl, SaveSearchItemView, VSearchList, Utils, Globals, UrlLinks, CommonViewFunction, Messages) {
    'use strict';

    return Backbone.Marionette.CompositeView.extend({
        template: SaveSearchViewTmpl,
        childView: SaveSearchItemView,
        childViewContainer: "[data-id='itemViewContent']",
        ui: {
            saveAs: "[data-id='saveAsBtn']",
            save: "[data-id='saveBtn']"
        },
        childViewOptions: function() {
            return {
                collection: this.collection,
                typeHeaders: this.typeHeaders,
                applyValue: this.applyValue,
                isBasic: this.isBasic,
                isRelationship: this.isRelationship,
                classificationDefCollection: this.classificationDefCollection,
                entityDefCollection: this.entityDefCollection,
                relationshipDefCollection: this.relationshipDefCollection,
                fetchFavioriteCollection: this.fetchCollection.bind(this),
                searchTypeObj: this.searchTypeObj
            };
        },
        childEvents: function() {
            return {
                "item:clicked": function() {
                    this.ui.save.attr('disabled', false);
                }
            }
        },
        events: function() {
            var events = {};
            events['click ' + this.ui.saveAs] = "saveAs";
            events['click ' + this.ui.save] = "save";
            return events;
        },
        initialize: function(options) {
            var that = this;
            _.extend(this, _.pick(options, 'collection', 'value', 'searchVent', 'typeHeaders', 'applyValue', 'getValue', 'isBasic', 'isRelationship', 'fetchCollection', 'classificationDefCollection', 'entityDefCollection', 'relationshipDefCollection'));
            this.searchTypeObj = {
                'searchType': 'dsl',
                'dslChecked': 'true'
            }
            if (this.isBasic) {
                this.searchTypeObj.dslChecked = false;
                this.searchTypeObj.searchType = 'basic';
            }
        },
        onRender: function() {
            this.bindEvents();
        },
        bindEvents: function() {
            var that = this,
                searchType;
            if (that.isRelationship) {
                searchType = "isRelationship"
            } else if (that.isBasic) {
                searchType = "isBasic";
            } else {
                searchType = "isAdvance"
            }
            this.listenTo(this.collection, "add reset error remove", function(model, collection) {
                this.$('.fontLoader-relative').hide();
                if (this.collection && this.collection.length) {
                    this.$(".noFavoriteSearch").hide();
                } else {
                    this.$(".noFavoriteSearch").show();
                }
            }, this);
            $('body').on('click', '.saveSearchPopoverList_' + searchType + ' li', function(e) {
                that.$('.saveSearchPopover').popover('hide');
                var id = $(this).parent('ul').data('id');
                that[$(this).find('a').data('fn')]({
                    'model': that.collection.find({ 'guid': id })
                });
            });
        },
        saveAs: function(e) {
            var value = this.getValue();
            if (value && (value.type || value.tag || value.query || value.term)) {
                this.callSaveModalLayoutView({
                    'collection': this.collection,
                    'getValue': this.getValue,
                    'isBasic': this.isBasic
                });
            } else if (value && value.relationshipName) {
                this.callSaveModalLayoutView({
                    'collection': this.collection,
                    'getValue': this.getValue,
                    'isRelationship': this.isRelationship
                });
            } else {
                Utils.notifyInfo({
                    content: Messages.search.favoriteSearch.notSelectedSearchFilter
                })
            }
        },
        save: function() {
            var that = this,
                obj = {},
                notifyObj = {
                    modal: true,
                    html: true,
                    ok: function(argument) {
                        if (that.isRelationship) {
                            that.callSaveModalLayoutView({
                                'saveObj': obj,
                                'collection': that.collection,
                                'getValue': that.getValue,
                                'isRelationship': that.isRelationship,
                            });
                        } else {
                            that.callSaveModalLayoutView({
                                'saveObj': obj,
                                'collection': that.collection,
                                'getValue': that.getValue,
                                'isBasic': that.isBasic,
                            });
                        }

                    },
                    cancel: function(argument) {}
                },
                selectedEl = this.$('.saveSearchList li.active').find('div.item');
            obj.name = selectedEl.find('a').text();
            obj.guid = selectedEl.data('id');
            if (selectedEl && selectedEl.length) {
                notifyObj['text'] = Messages.search.favoriteSearch.save + " <b>" + _.escape(obj.name) + "</b> ?";
                Utils.notifyConfirm(notifyObj);
            } else {
                Utils.notifyInfo({
                    content: Messages.search.favoriteSearch.notSelectedFavoriteElement
                })
            }
        },
        callSaveModalLayoutView: function(options) {
            require([
                'views/search/save/SaveModalLayoutView'
            ], function(SaveModalLayoutView) {
                new SaveModalLayoutView(options);
            });
        },
        onSearch: function(options) {
            if (options && options.model) {
                var searchParameters = options.model.toJSON().searchParameters,
                    searchType = options.model.get('searchType'),
                    params = CommonViewFunction.generateUrlFromSaveSearchObject({
                        value: { "searchParameters": searchParameters, "uiParameters": options.model.get('uiParameters') },
                        classificationDefCollection: this.classificationDefCollection,
                        entityDefCollection: this.entityDefCollection,
                        relationshipDefCollection: this.relationshipDefCollection
                    });
                if (searchType === "BASIC_RELATIONSHIP") {
                    Globals.fromRelationshipSearch = true;
                    Utils.setUrl({
                        url: '#!/relationship/relationshipSearchResult',
                        urlParams: _.extend({}, { 'searchType': 'basic' }, params),
                        mergeBrowserUrl: false,
                        trigger: true,
                        updateTabState: true
                    });
                } else {
                    Globals.fromRelationshipSearch = false;
                    Utils.setUrl({
                        url: '#!/search/searchResult',
                        urlParams: _.extend({}, this.searchTypeObj, params),
                        mergeBrowserUrl: false,
                        trigger: true,
                        updateTabState: true
                    });
                }
            }
        },
        onRename: function(options) {
            if (options && options.model) {
                var that = this;
                require([
                    'views/search/save/SaveModalLayoutView'
                ], function(SaveModalLayoutView) {
                    new SaveModalLayoutView({ 'selectedModel': options.model, 'collection': that.collection, 'getValue': that.getValue, 'isBasic': that.isBasic, 'isRelationship': that.isRelationship });
                });
            }
        },
        onDelete: function(options) {
            if (options && options.model) {
                var that = this;
                var notifyObj = {
                    modal: true,
                    html: true,
                    text: Messages.conformation.deleteMessage + "<b>" + _.escape(options.model.get('name')) + "</b>" + " ?",
                    ok: function(obj) {
                        that.notificationModal = obj;
                        obj.showButtonLoader();
                        that.onDeleteNotifyOk(options);
                    },
                    okCloses: false,
                    cancel: function(argument) {}
                }
                Utils.notifyConfirm(notifyObj);
            }
        },
        onDeleteNotifyOk: function(options) {
            var that = this;
            options.model.urlRoot = UrlLinks.saveSearchApiUrl();
            options.model.destroy({
                wait: true,
                success: function(model, data) {
                    if (that.collection) {
                        that.collection.remove(model);
                    }
                    that.notificationModal.hideButtonLoader();
                    that.notificationModal.remove();
                    Utils.notifySuccess({
                        content: options.model.get('name') + Messages.getAbbreviationMsg(false, 'deleteSuccessMessage')
                    });
                }
            });
        }
    });
});